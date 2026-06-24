FROM eclipse-temurin:17-jre-jammy

ARG JAR_FILE

ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"

WORKDIR /app
COPY ${JAR_FILE} /app/app.jar

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:-} -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]
