#!/usr/bin/env python3
"""ローカル Ollama (qwen3:14b) にコード生成を委譲する CLI。

Claude が「契約」を書き、このスクリプトが「中身」をローカル LLM に埋めさせる。
ホストに追加パッケージを入れずに済むよう、標準ライブラリのみで実装している。

使い方:
    python3 tools/llm/gen.py \\
        --task tools/llm/tasks/post-dto.md \\
        --prompt java-record \\
        --out backend/src/main/java/com/example/xapp/post/dto/PostResponse.java

    # コンパイルエラーを差し戻して修正させる
    python3 tools/llm/gen.py \\
        --task tools/llm/tasks/post-dto.md \\
        --prompt java-record \\
        --out backend/src/main/java/.../PostResponse.java \\
        --repair /tmp/compile-error.txt

修復ループは最大 2 回まで。それで直らなければ Claude が引き取って手書きする
（ローカル LLM の修復ループは容易に無限ループ化し、全部手で書くより遅くなるため）。
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.request
from pathlib import Path

DEFAULT_BASE_URL = os.environ.get("OLLAMA_BASE_URL", "http://192.168.11.35:11434")
DEFAULT_MODEL = os.environ.get("OLLAMA_MODEL", "qwen3:14b")
DEFAULT_TIMEOUT = 300  # 14B のローカル推論は遅い。短いと途中で切れる。

REPO_ROOT = Path(__file__).resolve().parents[2]
PROMPT_DIR = Path(__file__).resolve().parent / "prompts"

# ```java ... ``` のようなコードフェンスを剥がす
FENCE_RE = re.compile(r"^\s*```[a-zA-Z0-9_+-]*\s*\n(.*?)\n?\s*```\s*$", re.DOTALL)
# qwen3 が reasoning を分離せず本文に <think> を混ぜた場合の保険
THINK_RE = re.compile(r"<think>.*?</think>\s*", re.DOTALL)


class GenError(RuntimeError):
    pass


def load_system_prompt(name: str) -> str:
    path = PROMPT_DIR / f"{name}.md"
    if not path.is_file():
        available = sorted(p.stem for p in PROMPT_DIR.glob("*.md"))
        raise GenError(
            f"system prompt が見つかりません: {path}\n"
            f"利用可能: {', '.join(available) if available else '(なし)'}"
        )
    return path.read_text(encoding="utf-8")


def build_messages(system: str, task: str, previous: str | None, errors: str | None) -> list[dict]:
    messages = [
        {"role": "system", "content": system},
        {"role": "user", "content": task},
    ]
    if previous is not None and errors is not None:
        messages.append({"role": "assistant", "content": previous})
        messages.append(
            {
                "role": "user",
                "content": (
                    "上記のコードは以下のエラーで失敗しました。\n"
                    "エラーを修正した完全なファイル内容だけを再出力してください。\n"
                    "差分や説明は出力しないでください。\n\n"
                    "```\n" + errors.strip() + "\n```"
                ),
            }
        )
    return messages


def call_ollama(base_url: str, model: str, messages: list[dict], timeout: int) -> str:
    """Ollama の OpenAI 互換エンドポイントを叩き、本文を返す。

    qwen3 は thinking を choices[0].message.reasoning に分離して返すため、
    content だけを取れば思考過程は自然に落ちる。
    """
    url = base_url.rstrip("/") + "/v1/chat/completions"
    payload = json.dumps(
        {
            "model": model,
            "messages": messages,
            "stream": False,
            # コード生成なので決定性を優先する
            "temperature": 0.1,
            "top_p": 0.9,
        }
    ).encode("utf-8")

    req = urllib.request.Request(
        url, data=payload, headers={"Content-Type": "application/json"}, method="POST"
    )
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            body = json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as exc:
        detail = exc.read().decode("utf-8", errors="replace")[:500]
        raise GenError(f"Ollama が HTTP {exc.code} を返しました: {detail}") from exc
    except urllib.error.URLError as exc:
        raise GenError(
            f"Ollama に接続できません ({url}): {exc.reason}\n"
            f"サーバーが起動しているか、同一ネットワークにいるか確認してください。"
        ) from exc
    except TimeoutError as exc:
        raise GenError(
            f"Ollama が {timeout} 秒以内に応答しませんでした。"
            f"--timeout を伸ばすか、タスクを小さく分割してください。"
        ) from exc

    try:
        return body["choices"][0]["message"]["content"]
    except (KeyError, IndexError) as exc:
        raise GenError(f"予期しないレスポンス形式です: {json.dumps(body)[:500]}") from exc


def extract_code(raw: str) -> str:
    """LLM の出力から、そのままファイルに書ける中身だけを取り出す。"""
    text = THINK_RE.sub("", raw).strip()

    match = FENCE_RE.match(text)
    if match:
        return match.group(1).strip() + "\n"

    # フェンスが本文の途中にある場合は最初のブロックを採用する
    blocks = re.findall(r"```[a-zA-Z0-9_+-]*\s*\n(.*?)\n?```", text, re.DOTALL)
    if blocks:
        return blocks[0].strip() + "\n"

    return text + "\n"


def sanity_check(code: str, out_path: Path | None) -> list[str]:
    """明らかにおかしい出力を早期に弾く。生成物のレビュー負荷を下げるため。"""
    warnings: list[str] = []
    if not code.strip():
        warnings.append("出力が空です。")
    if "..." in code and out_path and out_path.suffix == ".java":
        warnings.append("省略記号 '...' を含みます。部分出力の可能性があります。")
    lowered = code.lower()
    for phrase in ("以下のように", "here is the", "this code", "```"):
        if phrase in lowered:
            warnings.append(f"説明文らしき文字列を含みます: {phrase!r}")
            break
    if out_path and out_path.suffix == ".java":
        if "package " not in code:
            warnings.append("package 宣言がありません。")
    return warnings


def main() -> int:
    parser = argparse.ArgumentParser(
        description="ローカル Ollama にコード生成を委譲する",
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument("--task", required=True, help="タスクカード (Markdown) のパス")
    parser.add_argument(
        "--prompt",
        required=True,
        help="tools/llm/prompts/<name>.md の <name>（java-record, java-service, java-test, react-component）",
    )
    parser.add_argument("--out", help="出力先ファイル。省略時は標準出力")
    parser.add_argument("--repair", help="前回の失敗ログ。--out の既存内容を前回出力として差し戻す")
    parser.add_argument("--model", default=DEFAULT_MODEL)
    parser.add_argument("--base-url", default=DEFAULT_BASE_URL)
    parser.add_argument("--timeout", type=int, default=DEFAULT_TIMEOUT, help="秒 (既定 300)")
    parser.add_argument("--dry-run", action="store_true", help="ファイルに書かず標準出力に出す")
    args = parser.parse_args()

    task_path = Path(args.task)
    if not task_path.is_file():
        print(f"error: タスクカードが見つかりません: {task_path}", file=sys.stderr)
        return 2
    task = task_path.read_text(encoding="utf-8")

    out_path = Path(args.out) if args.out else None

    previous: str | None = None
    errors: str | None = None
    if args.repair:
        error_path = Path(args.repair)
        if not error_path.is_file():
            print(f"error: エラーログが見つかりません: {error_path}", file=sys.stderr)
            return 2
        errors = error_path.read_text(encoding="utf-8")
        if out_path is None or not out_path.is_file():
            print(
                "error: --repair には、前回出力を含む既存の --out ファイルが必要です",
                file=sys.stderr,
            )
            return 2
        previous = out_path.read_text(encoding="utf-8")

    try:
        system = load_system_prompt(args.prompt)
        messages = build_messages(system, task, previous, errors)
        print(
            f"[gen] model={args.model} prompt={args.prompt} task={task_path.name}"
            f"{' (repair)' if args.repair else ''}",
            file=sys.stderr,
        )
        raw = call_ollama(args.base_url, args.model, messages, args.timeout)
    except GenError as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 1

    code = extract_code(raw)

    for warning in sanity_check(code, out_path):
        print(f"[warn] {warning}", file=sys.stderr)

    if args.dry_run or out_path is None:
        sys.stdout.write(code)
        return 0

    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(code, encoding="utf-8")
    try:
        shown = out_path.relative_to(REPO_ROOT)
    except ValueError:
        shown = out_path
    print(f"[gen] wrote {shown} ({len(code.splitlines())} lines)", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
