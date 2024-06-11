FROM eclipse-temurin:8u412-b08-jdk-ubi9-minimal
LABEL title="xiaoya-sync"
LABEL description="同步小雅emby媒体库"
LABEL authors="JackDing"
COPY ./target/application.jar /xiaoyasync.jar
VOLUME /data
VOLUME /log
ENV TZ=Asia/Shanghai
ENV runAfterStartup="0"
ENV excludeList=""
ENV threadPoolNum="99"
ENV syncUrl=""
ENV syncDir=""
ENV tgToken=""
ENV tgUserId=""
ENV JAVA_OPTS=""
ENTRYPOINT ["sh","-c","java -jar $JAVA_OPTS -XX:+OptimizeStringConcat -XX:+PrintGCDetails -Xloggc:/log/gc.log  -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/log /xiaoyasync.jar"]