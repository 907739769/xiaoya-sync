package cn.jackding.xiaoyasync;

import cn.jackding.xiaoyasync.util.Util;
import lombok.extern.log4j.Log4j2;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.internal.StringUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URLDecoder;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @Author Jack
 * @Date 2024/5/17 22:51
 * @Version 1.0.0
 */
@Service
@Log4j2
public class SyncService {

    @Value("${syncUrl}")
    private String baseUrl;

    private String useBaseUrl;

    @Value("${mediaLibDir}")
    private String localDir;

    @Value("#{'${excludeList}'.split(',')}")
    private List<String> excludeList;

    @Value("${threadPoolNum:99}")
    private int threadPoolNum;

    @Value("${syncDir}")
    private String syncDir;

    //在这个列表里面的就会执行删除操作
    private List<String> syncList = Arrays.asList("每日更新/.*,电影/2023/.*,纪录片（已刮削）/.*,音乐/演唱会/.*,音乐/狄更斯：音乐剧 (2023)/.*".split(","));

    //这个是全部元数据的网站列表  在这个列表里面就同步全部元数据并且删除过时数据 否则不会删除
    private final List<String> allBaseUrl = Arrays.asList("https://icyou.eu.org/,https://lanyuewan.cn/,https://emby.8.net.co/,https://emby.raydoom.tk/,https://emby.kaiserver.uk/,https://embyxiaoya.laogl.top/,https://emby.xiaoya.pro/".split(","));

    private volatile String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36 Edg/125.0.0.0";

    //下载文件线程池 设置小一点 防止下载太快被风控
    private ThreadPoolExecutor executorService;

    //处理网站文件线程池
    private ThreadPoolExecutor pool;

    private volatile long currentTimeMillis;

    private CopyOnWriteArrayList<String> downloadFiles;

    private ConnectionPool connectionPool;

    private OkHttpClient client;

    private volatile String run;

    // // 创建 Pattern 对象
    private static final Pattern pattern = Pattern.compile("<a href=\"(.*?)\">(.*?)</a>\\s+(\\d{2}-[A-Za-z]{3}-\\d{4} \\d{2}:\\d{2})");

    /**
     * 同步媒体库
     */
    public void syncFiles(String syncDir) {
        if ("1".equals(run)) {
            log.debug("任务正在执行中");
            Util.sendTgMsg("任务正在执行中");
            throw new RuntimeException("任务正在执行中");
        }
        run = "1";
        //增加三次重试
        for (int i = 0; ; i++) {
            init();
            try {
                log.info("媒体库同步任务开始");
                Util.sendTgMsg("媒体库同步任务开始");
                log.info("排除列表：{}", excludeList);
                syncFilesRecursively(useBaseUrl + Util.encode(syncDir), localDir + syncDir.replace("/", File.separator).replaceAll("[:*?\"<>|]", "_"), syncDir);
                break;
            } catch (Exception e) {
                if (i < 2) {
                    Util.sleep(1);
                } else {
                    log.warn("媒体库同步任务失败");
                    Util.sendTgMsg("媒体库同步任务失败");
                    log.error("", e);
                    throw new RuntimeException(e);
                }
            }
        }

    }

    /**
     * 同步媒体库每日任务
     */
    public void syncFilesDaily() {
        if (StringUtil.isBlank(syncDir)) {
            syncDir = "每日更新/";
        }
        syncFiles(syncDir);
    }

    public void syncFilesRecursively(String currentUrl, String localDir, String relativePath) {
        //获取网站上面的目录文件
        Map<String, String> remoteFiles = fetchFileList(currentUrl);
        Set<String> localFiles = new HashSet<>();
        //本地路径加上分隔符
        String currentLocalDir = localDir.endsWith(File.separator) ? localDir : localDir + File.separator;
        File localDirectory = new File(currentLocalDir);

        //本地路径不存在就创建
        if (!localDirectory.exists()) {
            if (localDirectory.mkdirs()) {
                log.debug("创建文件夹成功：{}", localDir);
            } else {
                log.error("创建文件夹失败：{}", localDir);
                return;
            }
        }

        //本地文件如果是目录加上后缀 方便后面和网站上面的名字对比
        for (File file : localDirectory.listFiles()) {
            if (file.isDirectory()) {
                localFiles.add(file.getName() + "/");
            } else {
                localFiles.add(file.getName());
            }
        }

        remoteFiles.forEach((file, date) -> pool.submit(() -> {
            //不在排除列表里面
            if (!exclude(relativePath + file)) {
                if (file.endsWith("/")) {
                    String localDirName = file.substring(0, file.length() - 1).replaceAll("[\\\\/:*?\"<>|]", "_");
                    // 如果是文件夹  递归调用自身方法
                    syncFilesRecursively(currentUrl + Util.encode(file), currentLocalDir + localDirName, relativePath + file);
                } else {
                    String localFileName = file.replaceAll("[\\\\/:*?\"<>|]", "_");
                    if (!localFiles.contains(localFileName) || isRemoteFileUpdated(date, currentLocalDir, localFileName)) {
                        executorService.submit(() -> downloadFile(currentUrl, currentLocalDir, Util.encode(file), localFileName));
                    }
                }
            } else {
                log.info("排除路径不处理：{}", relativePath + file);
            }
        }));

        //处理成和本地一样的格式 好对比 不然不好对比 本地对特殊字符处理了
        Set<String> remoteFileLinks = remoteFiles.keySet().stream().map(file -> {
            if (file.endsWith("/")) {
                return file.substring(0, file.length() - 1).replaceAll("[\\\\/:*?\"<>|]", "_") + "/";
            } else {
                //去掉特殊字符  去掉后缀，防止删除同名的nfo等文件
                return file.replaceAll("[\\\\/:*?\"<>|]", "_").substring(0, file.contains(".") ? file.lastIndexOf('.') : file.length());
            }
        }).collect(Collectors.toSet());

        // 删除网站上面不存在的本地文件 本地有但是网站上没有的文件 只会删除名单中的文件和文件夹
        for (String file : localFiles) {
            String fileName = file;
            if (!file.endsWith("/")) {
                fileName = file.contains(".") ? file.substring(0, file.lastIndexOf('.')) : file;
            }
            //远程没有本地这个文件名称  而且在处理列表里面  不在排除列表里面
            if (!remoteFileLinks.contains(fileName) && shouldDelete(relativePath + file) && !exclude(relativePath + file)) {
                File localFile = new File(currentLocalDir, file);
                if (localFile.isDirectory()) {
                    deleteDirectory(localFile);
                    if (!localFile.exists()) {
                        log.debug("删除过时文件夹成功currentLocalDir:{} Delete fail: {}", currentLocalDir, file);
                    } else {
                        log.warn("删除过时文件夹失败currentLocalDir:{} Deleted: {}", currentLocalDir, file);
                    }
                } else {
                    if (localFile.delete()) {
                        log.debug("删除过时文件成功currentLocalDir:{} Deleted: {}", currentLocalDir, file);
                    } else {
                        log.warn("删除过时文件失败currentLocalDir:{} Delete fail: {}", currentLocalDir, file);
                    }
                }
            }
        }
    }

    public void init() {
        currentTimeMillis = System.currentTimeMillis();
        downloadFiles = new CopyOnWriteArrayList<>();
        if (null == pool || pool.isShutdown() || pool.isTerminated() || pool.isTerminating()) {
            pool = new ThreadPoolExecutor(
                    threadPoolNum, // corePoolSize
                    threadPoolNum, // maximumPoolSize
                    30, // keepAliveTime
                    TimeUnit.SECONDS, // unit
                    new LinkedBlockingQueue<>()); // workQueue
        }
        if (null == executorService || executorService.isShutdown() || executorService.isTerminated() || executorService.isTerminating()) {
            executorService = new ThreadPoolExecutor(
                    10, // corePoolSize
                    10, // maximumPoolSize
                    30, // keepAliveTime
                    TimeUnit.SECONDS, // unit
                    new LinkedBlockingQueue<>()); // workQueue
        }
        // 创建 OkHttpClient 实例
        if (null == connectionPool) {
            connectionPool = new ConnectionPool(60, 30, TimeUnit.SECONDS);
        }
        if (null == client) {
            client = new OkHttpClient.Builder()
                    .readTimeout(60, TimeUnit.SECONDS)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .connectionPool(connectionPool)
                    .build();
        }
        userAgent = Util.userAgent();
        if (StringUtil.isBlank(baseUrl)) {
            int randomNumber = Util.random.nextInt(allBaseUrl.size());
            useBaseUrl = allBaseUrl.get(randomNumber);
        } else {
            useBaseUrl = baseUrl;
        }
        useBaseUrl = useBaseUrl.endsWith("/") ? useBaseUrl : useBaseUrl + "/";
        if (StringUtils.isNotBlank(syncDir)) {
            syncDir = syncDir.endsWith("/") ? syncDir : syncDir + "/";
        }
        //本地路径加上分隔符
        localDir = localDir.endsWith(File.separator) ? localDir : localDir + File.separator;
        //如果是这些网站 同步的文件会多一些
        if (allBaseUrl.contains(useBaseUrl)) {
            syncList = Arrays.asList("每日更新/.*,电影/.*,纪录片（已刮削）/.*,音乐/.*,PikPak/.*,动漫/.*,电视剧/.*,纪录片/.*,综艺/.*,\uD83D\uDCFA画质演示测试（4K，8K，HDR，Dolby）/".split(","));
        }
    }

    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }

    private Map<String, String> fetchFileList(String url) {
        String decodeUrl = Util.decode(url);
        log.debug("开始获取网站文件目录：{}", decodeUrl);
        Set<String> files = new HashSet<>();
        // 创建 GET 请求
        Request getRequest = new Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .build();
        //如果失败尝试获取三次
        for (int i = 0; ; i++) {
            try (Response getResponse = client.newCall(getRequest).execute()) {
                if (!getResponse.isSuccessful() || null == getResponse.body()) {
                    if (null != getResponse.body()) {
                        log.error(getResponse.body().string());
                    }
                    throw new RuntimeException();
                }
                // 创建 Matcher 对象
                Matcher matcher = pattern.matcher(getResponse.body().string());
                // 创建 Map 用于存放链接和对应的日期
                Map<String, String> linkDateMap = new HashMap<>();
                // 查找匹配的链接和日期
                while (matcher.find()) {
                    String link = matcher.group(1);
                    link = URLDecoder.decode(link, "UTF-8");
                    String date = matcher.group(3);
                    linkDateMap.put(link, date);
                }
                log.debug("获取网站文件目录成功：{}", decodeUrl);
                return linkDateMap;
            } catch (Exception e) {
                if (i < 2) {
                    log.debug("第{}次获取{}失败", i + 1, decodeUrl);
                    Util.sleep(1);
                } else {
                    log.warn("第{}次获取{}还是失败，放弃", i + 1, decodeUrl);
                    log.error("", e);
                    throw new RuntimeException(e);
                }
                userAgent = Util.userAgent();
            }
        }
    }

    private void downloadFile(String currentUrl, String localDir, String file, String localFileName) {

        // 创建 GET 请求
        Request getRequest = new Request.Builder()
                .url(currentUrl + file)
                .header("User-Agent", userAgent)
                .build();
        // 发送 GET 请求并处理响应
        for (int i = 0; ; i++) {
            try (Response getResponse = client.newCall(getRequest).execute()) {
                assert getResponse.body() != null;
                try (ReadableByteChannel rbc = Channels.newChannel(getResponse.body().byteStream());
                     FileOutputStream fos = new FileOutputStream(new File(localDir, localFileName));
                     FileChannel fileChannel = fos.getChannel()) {
                    if (!getResponse.isSuccessful()) {
                        throw new RuntimeException();
                    }
                    fileChannel.transferFrom(rbc, 0, Long.MAX_VALUE);
                    log.debug("下载文件成功localDir:{} Downloaded: {}", localDir, localFileName);
                    downloadFiles.add(localDir.endsWith(File.separator) ? localDir + localFileName : localDir + File.separator + localFileName);
                    break;
                }
            } catch (Exception e) {
                String decodeCurrentUrl = Util.decode(currentUrl);
                if (i < 2) {
                    log.debug("第{}次下载{}失败", i + 1, decodeCurrentUrl + localFileName);
                    Util.sleep(1);
                } else {
                    log.warn("第{}次下载{}还是失败，放弃", i + 1, decodeCurrentUrl + localFileName);
                    log.warn("下载文件失败localDir:{} Download fail: {}", localDir, localFileName);
                    log.error("", e);
                    break;
                }
                userAgent = Util.userAgent();
            }
        }

    }

    private boolean shouldDelete(String relativePath) {
        for (String pattern : syncList) {
            if (relativePath.matches(pattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean exclude(String relativePath) {
        for (String pattern : excludeList) {
            if (relativePath.matches(pattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean isRemoteFileUpdated(String remoteFileDate, String localDir, String localFileName) {
        File localFile = new File(localDir, localFileName);
        if (localFile.length() < 10) {
            return true;
        }
        long localLastModified = localFile.lastModified();

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy hh:mm", Locale.ENGLISH);
        Date date;
        try {
            date = dateFormat.parse(remoteFileDate);
        } catch (ParseException e) {
            log.error("", e);
            return false;
        }

        long remoteLastModified = date.getTime();
        if (allBaseUrl.contains(useBaseUrl)) {
            remoteLastModified = remoteLastModified + 28800000;
        }
        if (remoteLastModified > localLastModified) {
            log.debug("更新文件localDir:{} localFileName: {}", localDir, localFileName);
        }
        return remoteLastModified > localLastModified;

    }

    /**
     * 定时任务每20秒执行一次
     */
    @Scheduled(fixedRate = 20000, initialDelay = 20000)
    public void checkThreadPoolStatus() {

        if (null == executorService || null == pool) {
            return;
        }
        //线程池是否关闭
        if (executorService.isTerminated() || executorService.isShutdown() || pool.isTerminated() || pool.isShutdown()) {
            return;
        }

        if (!"1".equals(run)) {
            return;
        }

        if (pool.getActiveCount() == 0 && pool.getQueue().isEmpty() && executorService.getActiveCount() == 0 && executorService.getQueue().isEmpty()) {
            if (!downloadFiles.isEmpty()) {
                Collections.sort(downloadFiles);
                log.debug("以下是下载的文件");
                for (String fileName : downloadFiles) {
                    log.debug(fileName);
                }
                log.debug("以上是下载的文件");
                log.info("共下载{}个文件", downloadFiles.size());
                Util.sendTgMsg("共下载" + downloadFiles.size() + "个文件");
            } else {
                log.info("没有新的内容更新");
                Util.sendTgMsg("没有新的内容更新");
            }
            long milliseconds = (System.currentTimeMillis() - currentTimeMillis) < 40000 ? (System.currentTimeMillis() - currentTimeMillis) : (System.currentTimeMillis() - currentTimeMillis - 40000);
            long hours = TimeUnit.MILLISECONDS.toHours(milliseconds);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) - TimeUnit.HOURS.toMinutes(hours);
            long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds));
            log.info("媒体库同步任务全部完成耗时：{}小时{}分钟{}秒", hours, minutes, seconds);
            Util.sendTgMsg("媒体库同步任务全部完成耗时：" + hours + "小时" + minutes + "分钟" + seconds + "秒");
            currentTimeMillis = 0;
            downloadFiles.clear();
            downloadFiles = null;
            run = null;
        }

    }

    /**
     * 服务停止的时候销毁线程池
     */
    @PreDestroy
    public void onDestroy() {
        if (null != pool) {
            pool.shutdown();
        }
        if (null != executorService) {
            executorService.shutdown();
        }
        if (null != pool) {
            try {
                if (!pool.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                }
            } catch (InterruptedException e) {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        if (null != executorService) {
            try {
                if (!executorService.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

    }

}