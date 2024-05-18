# xiaoya-sync
同步小雅emby媒体库，每天早上晚上六点同步，服务启动也会执行一次

## docker部署 


```
部署前参数需要修改
/volume1/docker-data/xiaoya/xiaoya修改成媒体库路径
```

docker CLI安装

```
docker run -d \
--name=xiaoya-sync \
-e TZ=Asia/Shanghai \
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
    volumes:
      - /volume1/docker-data/xiaoya/xiaoya:/data
```
