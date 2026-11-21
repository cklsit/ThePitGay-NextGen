# syntax=docker/dockerfile:1.7

###############################################################################
# 阶段 1：编译插件（产出 ThePitGay-V.jar）
###############################################################################
FROM eclipse-temurin:21-jdk AS builder

ENV DEBIAN_FRONTEND=noninteractive

RUN set -eux; \
    apt-get update; \
    apt-get install -y --no-install-recommends curl ca-certificates unzip; \
    rm -rf /var/lib/apt/lists/*

WORKDIR /src

# Gradle 依赖与 wrapper 缓存
ENV GRADLE_USER_HOME=/gradle-cache

COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
RUN chmod +x gradlew

COPY libs ./libs
COPY vendor ./vendor
COPY core ./core

# Paper 1.21.8 NMS 编译类路径（paper-api 不含 net.minecraft.*）
ARG PAPER_BUILD=60
ARG PAPER_URL=https://fill-data.papermc.io/v1/objects/8de7c52c3b02403503d16fac58003f1efef7dd7a0256786843927fa92ee57f1e/paper-1.21.8-60.jar
ARG PAPER_SHA256=8de7c52c3b02403503d16fac58003f1efef7dd7a0256786843927fa92ee57f1e
RUN set -eux; \
    mkdir -p /tmp/paper /src/.gradle/paper-nms; \
    curl -fsSL -o /tmp/paper/paper.jar "$PAPER_URL"; \
    echo "${PAPER_SHA256}  /tmp/paper/paper.jar" | sha256sum -c -; \
    cd /src/.gradle/paper-nms; \
    java -Dpaperclip.patchonly=true -jar /tmp/paper/paper.jar; \
    test -f versions/1.21.8/paper-1.21.8.jar; \
    rm -rf /tmp/paper

RUN ./gradlew :core:shadowJar --no-daemon --stacktrace

###############################################################################
# 阶段 2：运行时镜像（Paper + 插件 + 内置地图）
###############################################################################
FROM eclipse-temurin:21-jre

LABEL org.opencontainers.image.title="ThePitGay-NextGen" \
      org.opencontainers.image.description="Paper 1.21.8 pit server with ThePitGay-NextGen, LuckPerms and bundled pit_arena map" \
      org.opencontainers.image.source="https://github.com/cklsit/ThePitGay-NextGen" \
      org.opencontainers.image.licenses="MIT"

ARG PAPER_URL=https://fill-data.papermc.io/v1/objects/8de7c52c3b02403503d16fac58003f1efef7dd7a0256786843927fa92ee57f1e/paper-1.21.8-60.jar
ARG PAPER_SHA256=8de7c52c3b02403503d16fac58003f1efef7dd7a0256786843927fa92ee57f1e
ARG LUCKPERMS_URL=https://cdn.modrinth.com/data/Vebnzrzj/versions/b0mk8uS6/LuckPerms-Bukkit-5.5.71.jar
ARG LUCKPERMS_SHA512=188a91f0a543d23bfda32385fca6db63d61e49c8a422bd452a260bd9cbc6a7d7fe45071199e9fca8f3ce43c2b41ee84fd315bd15464577028ff3951a7d4fab27

RUN set -eux; \
    apt-get update; \
    apt-get install -y --no-install-recommends unzip curl ca-certificates; \
    rm -rf /var/lib/apt/lists/*; \
    mkdir -p /opt/server/plugins-src; \
    curl -fsSL -o /opt/server/paper.jar "$PAPER_URL"; \
    echo "${PAPER_SHA256}  /opt/server/paper.jar" | sha256sum -c -; \
    curl -fsSL -o /opt/server/plugins-src/LuckPerms-Bukkit.jar "$LUCKPERMS_URL"; \
    echo "${LUCKPERMS_SHA512}  /opt/server/plugins-src/LuckPerms-Bukkit.jar" | sha512sum -c -

COPY --from=builder /src/core/build/libs/ThePitGay-V.jar /opt/server/plugins-src/ThePitGay-V.jar
COPY docker/entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

ENV DATA_DIR=/data \
    MEMORY=4G \
    ONLINE_MODE=false \
    WHITE_LIST=false \
    ALLOW_NETHER=false \
    LEVEL_NAME=pit_arena \
    SERVER_PORT=25565 \
    MAX_PLAYERS=100

VOLUME ["/data"]
EXPOSE 25565/tcp

WORKDIR /data

HEALTHCHECK --interval=30s --timeout=10s --start-period=240s --retries=5 \
    CMD ["bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/${SERVER_PORT:-25565}"]

STOPSIGNAL SIGTERM

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
