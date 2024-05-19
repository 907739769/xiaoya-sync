# xiaoya-sync

注意，这个docker容器会占用1GB的内存

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

`https://icyou.eu.org/` `https://lanyuewan.cn/`

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
```

# 开发计划
- [x] 1.增加启动是否执行任务的开关
- [x] 2.增加线程数配置，增加排除列表功能
- [x] 3.增加指定同步网站配置
- [x] 4.增加线程数配置
- [x] 5.增加指定目录同步

## docker部署 


```
部署前参数需要修改
/volume1/docker-data/xiaoya/xiaoya修改成媒体库路径
runAfterStartup  启动是否立即执行默认1启用，不启用填0
excludeList 排除列表 默认为空 不进行同步及删除的目录例如每日更新/动漫/.*,每日更新/动漫剧场版/.*
threadPoolNum 设置线程数默认199，设置越大占用内存CPU越高，同步速度相应会快一些 每增多200线程多占用1G内存
syncUrl 同步网站 默认https://emby.xiaoya.pro/  可选https://icyou.eu.org/或者https://lanyuewan.cn/
syncDir 同步路径 指定同步路径 默认空 同步全站，可填入 每日更新/电影/ 或者 每日更新/  等具体的网站路径
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
-e runAfterStartup=1 \
-e excludeList="" \
-e threadPoolNum="199" \
-e syncUrl="https://emby.xiaoya.pro/" \
-e syncDir="" \
-v /volume1/docker-data/xiaoya/xiaoya:/data \
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
      runAfterStartup: 1
      excludeList: ""
      threadPoolNum: 199
      syncUrl: "https://emby.xiaoya.pro/"
      syncDir: ""
    volumes:
      - /volume1/docker-data/xiaoya/xiaoya:/data
```
