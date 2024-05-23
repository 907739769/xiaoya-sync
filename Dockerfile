FROM eclipse-temurin:8u342-b07-jre-jammy
LABEL title="xiaoya-sync"
LABEL description="同步小雅emby媒体库"
LABEL authors="JackDing"
COPY ./target/application.jar /application.jar
VOLUME /data
VOLUME /log
ENV TZ=Asia/Shanghai
ENV runAfterStartup="1"
ENV excludeList=""
ENV threadPoolNum="99"
ENV syncUrl="https://emby.xiaoya.pro/"
ENV syncDir=""
ENTRYPOINT ["sh","-c","java -jar -Xms128m -Xmx4095m /application.jar"]