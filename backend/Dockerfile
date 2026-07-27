# syntax=docker/dockerfile:1

# ============================================================
# build: layered jar を作る
# ============================================================
FROM gradle:9.6-jdk25 AS build
WORKDIR /workspace

# 依存解決の結果をレイヤーキャッシュに載せるため、ビルド定義だけ先にコピーする。
# アプリコードだけ変えたときに依存の再取得が走らないようにするため。
COPY --chown=gradle:gradle settings.gradle.kts ./
COPY --chown=gradle:gradle gradle/ gradle/
COPY --chown=gradle:gradle backend/build.gradle.kts backend/
RUN gradle :backend:dependencies --no-daemon --quiet || true

COPY --chown=gradle:gradle backend/src backend/src
RUN gradle :backend:bootJar --no-daemon

# ============================================================
# extract: jar をレイヤーに分解する
# ============================================================
FROM eclipse-temurin:25-jre-alpine AS extract
WORKDIR /src
COPY --from=build /workspace/backend/build/libs/*.jar app.jar
# 展開先は空である必要があるため、jar の置き場とは別ディレクトリを指定する
RUN java -Djarmode=tools -jar app.jar extract --layers --destination /extracted

# ============================================================
# runtime
# ============================================================
FROM eclipse-temurin:25-jre-alpine AS runtime

RUN addgroup -S app && adduser -S -G app app
WORKDIR /app

# 変更頻度の低いレイヤーから順にコピーする。アプリコードだけの変更なら
# 依存レイヤーが再利用され、イメージの再ビルドと転送が速くなる。
#
# レイヤーは /extracted 直下に展開される（app/ のような中間ディレクトリは無い）。
# dependencies/lib/ に依存 jar、application/ に app.jar が入る。
# spring-boot-loader と snapshot-dependencies は現状空だが、SNAPSHOT 依存を
# 使い始めたときに効くようコピー対象に残す。
COPY --from=extract --chown=app:app /extracted/dependencies/ ./
COPY --from=extract --chown=app:app /extracted/spring-boot-loader/ ./
COPY --from=extract --chown=app:app /extracted/snapshot-dependencies/ ./
COPY --from=extract --chown=app:app /extracted/application/ ./

USER app
EXPOSE 8080

# コンテナに割り当てられたメモリを JVM が正しく認識するよう明示する
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"

HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=5 \
    CMD wget -qO- http://localhost:8080/api/v1/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
