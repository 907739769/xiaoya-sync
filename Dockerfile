FROM eclipse-temurin:8u342-b07-jre-jammy
LABEL title="xiaoya-sync"
LABEL description="同步小雅emby媒体库"
LABEL authors="JackDing"
COPY ./target/application.jar /application.jar
VOLUME /data
ENV TZ=Asia/Shanghai
ENV runAfterStartup="1"
ENTRYPOINT ["sh","-c","java -jar -Xms128m -Xmx2048m /application.jar"]