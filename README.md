# xiaoya-sync

注意，这个docker容器会占用1GB的内存

同步小雅emby媒体库，每天早上晚上六点同步，服务启动也会执行一次。扫描一次大概40分钟。

自动删除本地过时文件夹及文件（小雅媒体库网站不存在的文件及文件夹，不会删除其他目录的文件），
只会同步以下指定文件夹：
```
每日更新/
电影/2023/
纪录片（已刮削）/
音乐/演唱会/
音乐/狄更斯：音乐剧 (2023)/
```

# 开发计划
- [x] 1.增加启动是否执行任务的开关
- [ ] 2.增加增量更新，确认可行性
- [ ] 3...

## docker部署 


```
部署前参数需要修改
/volume1/docker-data/xiaoya/xiaoya修改成媒体库路径
runAfterStartup  启动是否立即执行默认1启用，不启用填0
```

docker CLI安装

```
docker run -d \
--name=xiaoya-sync \
-e TZ=Asia/Shanghai \
-e runAfterStartup=1 \
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
    volumes:
      - /volume1/docker-data/xiaoya/xiaoya:/data
```
