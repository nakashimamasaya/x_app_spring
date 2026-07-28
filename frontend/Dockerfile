# syntax=docker/dockerfile:1

# ============================================================
# build: 静的ファイルを生成する
# ============================================================
FROM node:24-alpine AS build
WORKDIR /app

# 依存だけ先に入れてレイヤーキャッシュに載せる
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

COPY frontend/ ./
COPY api/ /api/

# ブラウザが叩く API のアドレスはビルド時に埋め込まれる。
# デプロイ先が決まったらここを差し替える
ARG VITE_API_BASE_URL=http://localhost:8080/api/v1
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

# 型と仕様がズレたままビルドが通らないよう、生成し直してから型検査する
RUN npm run gen:api && npm run build

# ============================================================
# runtime: nginx で配信する
# ============================================================
FROM nginx:1.29-alpine AS runtime

COPY --from=build /app/dist /usr/share/nginx/html
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=10s --timeout=3s --start-period=5s --retries=3 \
    # localhost だと IPv6 (::1) に解決されるが nginx は IPv4 のみ listen するため
    # 接続拒否になる。明示的に 127.0.0.1 を指す
    CMD wget -qO- http://127.0.0.1/ >/dev/null || exit 1
