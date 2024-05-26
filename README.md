# xiaoya-sync

注意，这个docker容器运行任务期间会占用500MB的内存

同步小雅emby媒体库，每天早上晚上六点同步，服务启动也会执行一次。扫描一次大概10分钟。

自动删除本地过时文件夹及文件（小雅媒体库网站不存在的文件及文件夹，不会删除其他目录的文件），网站上面文件的和本地
同名文件不会删除，防止网站没有nfo等文件，但是本地有nfo等文件，导致误删nfo等文件

`https://emby.xiaoya.pro/`

上面这个网站同步以下指定文件夹：
```
每日更新/
电影/2023/
纪录片（已刮削）/
音乐/演唱会/
音乐/狄更斯：音乐剧 (2023)/
```

`https://icyou.eu.org/` `https://lanyuewan.cn/` `https://emby.8.net.co/` `https://emby.raydoom.tk/` `https://emby.kaiserver.uk/` `https://embyxiaoya.laogl.top/`

以上两个网站会同步以下指定目录

```
每日更新/
电影/
纪录片（已刮削）/
音乐/
PikPak/
动漫/
电视剧/
纪录片/
综艺/
📺画质演示测试（4K，8K，HDR，Dolby）/
```

# 开发计划
- [x] 1.增加启动是否执行任务的开关
- [x] 2.增加线程数配置，增加排除列表功能
- [x] 3.增加指定同步网站配置
- [x] 4.增加线程数配置
- [x] 5.增加指定目录同步
- [x] 6.使用okhttp大幅优化同步性能（http2支持单TCP连接），降低网站服务器压力
- [ ] 7.增加tg消息推送文件同步情况
- [ ] 0.增量更新，一个专门同步网站最新文件的中心，发送消息增量了哪些文件，其他同步客户端去订阅这个消息

# 更新记录

```
20240522 降低默认线程数、降低TCP连接数支持单TCP连接、修改UA
20240523 增加日志路径挂载、日志框架改为log4j2异步日志框架，调整启动服务默认不执行任务
20240524 修改日志打印、每日任务只同步每日更新、新增任务每7天同步一次全量数据、修改更新文件逻辑，文件时间戳改成网页上面的，而不去head请求网站，减少服务器压力,未设置同步网站的情况下随机从网站池中获取同步网站
20240526 每日任务增加启动任务前的一分钟内随机等待时间、修改同步全量文件的频次为每三天

```

## docker部署 


```
部署前参数需要修改
/volume1/docker-data/xiaoya/xiaoya修改成媒体库路径
runAfterStartup  启动是否立即执行同步任务 默认不启用0，启用填1
excludeList 排除列表 默认为空 设置不进行同步及删除的目录例如每日更新/动漫/.*,每日更新/动漫剧场版/.*
syncUrl 同步网站 默认从网站池中随机选一个  可选https://icyou.eu.org/或者https://lanyuewan.cn/
syncDir 同步路径 指定同步路径 默认空 同步全站，可填入 每日更新/电影/ 或者 每日更新/  等具体的网站路径
threadPoolNum 设置线程数默认99 不建议修改
```

一键命令部署

修改`/volume1/docker-data/xiaoya/xiaoya`为你的emby媒体库目录即可

```
docker run -d \
--name=xiaoya-sync \
--network="host" \
-v /volume1/docker-data/xiaoya/xiaoya:/data \
jacksaoding/xiaoya-sync:latest
```


docker CLI安装

```
docker run -d \
--name=xiaoya-sync \
--network="host" \
-e TZ=Asia/Shanghai \
-e runAfterStartup=0 \
-e excludeList="" \
-e threadPoolNum="99" \
-e syncUrl="" \
-e syncDir="" \
-v /volume1/docker-data/xiaoya/xiaoya:/data \
-v /volume1/docker/xiaoya-sync/log:/log \
jacksaoding/xiaoya-sync:latest
```

docker compose安装

```
version: "3"
services:
  app:
    container_name: xiaoya-sync
    image: 'jacksaoding/xiaoya-sync:latest'
    network_mode: "host"
    environment:
      TZ: Asia/Shanghai
      runAfterStartup: 0
      excludeList: ""
      threadPoolNum: 99
      syncUrl: ""
      syncDir: ""
    volumes:
      - /volume1/docker-data/xiaoya/xiaoya:/data
      - /volume1/docker/xiaoya-sync/log:/log
```
