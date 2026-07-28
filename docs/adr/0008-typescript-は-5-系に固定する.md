# 0008. TypeScript は 5 系に固定する

- ステータス: 採用
- 日付: 2026-07-28

## 背景

このプロジェクトは「言語・フレームワークは最新版を採用する」方針で進めている。
`create-vite` の雛形は TypeScript 6.0.3 を入れてきた。

しかし `openapi-typescript`（`openapi.yaml` から TS 型を生成するツール）は、
最新の 7.13.0 でも peer dependency が `typescript@^5.x` のままで、`next` タグの
プレリリース版も同様だった。npm はインストールを拒否した。

## 決定

フロントエンドの TypeScript を **5.9.x に固定**する。`openapi-typescript` が
TS 6 に対応した時点で引き上げる。

## 理由

**`--legacy-peer-deps` で押し通す選択肢を捨てた。**

`openapi-typescript` は TypeScript の Compiler API を内部で使って型を生成する。
peer 範囲の逸脱が「宣言上の不整合」で済まず、生成される型が壊れる可能性が実在する。
そして生成された型は**フロントエンド全体の型安全性の土台**であり、
ここが静かに壊れると影響範囲が広く、検出も遅れる。

**TypeScript のバージョンを下げるコストは小さい。** TS 5.9 は安定版で、
Vite 8 も React 19 も問題なく動く。TS 6 の新機能に依存した実装はまだ無い。

一方 `openapi-typescript` を捨てて手書きの型に切り替えるのは、
spec-first（[0001](./0001-bff-を採用しない.md) 以降の一貫した方針）を放棄することになり、
CI の `contract-drift` 検査も成立しなくなる。こちらのコストの方が明確に大きい。

## 結果

- 「最新版を採用する」方針から意図的に逸脱した箇所が 1 つできた
- Renovate は major 更新を Dependency Dashboard 承認制にしているので、
  TS 6 への引き上げが勝手に走ることはない
- 引き上げの前提は `openapi-typescript` の peer が `^6` を含むようになること

## 見直す条件

- `openapi-typescript` が TypeScript 6 に対応したとき
- TS 6 固有の機能が必要になったとき（その場合は型生成ツールの代替も併せて検討する）
