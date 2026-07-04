FROM eclipse-temurin:17-jre-jammy

ARG JAR_FILE

ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"

RUN sed -i 's|http://ports.ubuntu.com/ubuntu-ports|http://mirrors.aliyun.com/ubuntu-ports|g' /etc/apt/sources.list \
    && apt-get update -o Acquire::Retries=5 \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends -o Acquire::Retries=5 \
        libreoffice-writer \
        libreoffice-calc \
        libreoffice-impress \
        fonts-noto-cjk \
        fontconfig \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY ${JAR_FILE} /app/app.jar

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:-} -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]
