FROM eclipse-temurin:8u342-b07-jre-jammy
LABEL title="xiaoya-sync"
LABEL description="同步小雅emby媒体库"
LABEL authors="JackDing"
COPY ./target/application.jar /application.jar
VOLUME /data
VOLUME /log
ENV TZ=Asia/Shanghai
ENV runAfterStartup="0"
ENV excludeList=""
ENV threadPoolNum="99"
ENV syncUrl=""
ENV syncDir=""
ENTRYPOINT ["sh","-c","java -jar -Xms256m -Xmx512m -XX:+OptimizeStringConcat -XX:+PrintGCDetails -Xloggc:/log/gc.log  -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/log /application.jar"]