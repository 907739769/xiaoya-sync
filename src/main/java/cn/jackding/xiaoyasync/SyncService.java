package cn.jackding.xiaoyasync;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Author Jack
 * @Date 2024/5/17 22:51
 * @Version 1.0.0
 */
@Service
@Slf4j
public class SyncService {

    @Value("${sync.url}")
    private String baseUrl;

    @Value("${mediaLibDir}")
    private String localDir;

    @Value("#{'${excludeList}'.split(',')}")
    private List<String> excludeList;

    private final List<String> syncList = Arrays.asList("每日更新/.*,电影/2023/.*,纪录片（已刮削）/.*,音乐/演唱会/.*,音乐/狄更斯：音乐剧 (2023)/.*".split(","));

    private final String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    private final ExecutorService executorService = Executors.newFixedThreadPool(19);

    @Scheduled(cron = "0 0 6,18 * * ?")
    public void syncFiles() {
        CopyOnWriteArrayList<String> downloadFiles = new CopyOnWriteArrayList<>();
        long currentTimeMillis = System.currentTimeMillis();
        try {
            log.info("媒体库同步任务开始");
            log.info("排除列表：{}", excludeList);
            syncFilesRecursively(baseUrl, localDir, "", downloadFiles);
        } catch (Exception e) {
            log.warn("媒体库同步任务失败");
            log.error("", e);
            return;
        } finally {
            executorService.shutdown();
            log.info("媒体库同步任务耗时：{}ms", System.currentTimeMillis() - currentTimeMillis);
        }
        log.info("媒体库同步任务完成，正在下载剩下的文件");
        try {
            // 等待所有任务完成或超时（这里设置超时时间为 1 小时）
            if (!executorService.awaitTermination(1, TimeUnit.HOURS)) {
                // 如果超时，输出提示信息
                log.error("剩余文件下载任务超过1小时超时，放弃");
            }
            log.info("下载剩下的文件完成");
        } catch (InterruptedException e) {
            // 如果等待过程中发生中断，输出错误信息
            log.error("下载剩下的文件被中断");
            log.error("", e);
        } finally {
            if (!downloadFiles.isEmpty()) {
                log.info("以下是下载的文件");
                for (String fileName : downloadFiles) {
                    log.info(fileName);
                }
                log.info("以上是下载的文件");
            } else {
                log.info("没有新的内容更新");
            }
            log.info("媒体库同步任务全部完成耗时：{}ms", System.currentTimeMillis() - currentTimeMillis);
        }

    }

    private void syncFilesRecursively(String currentUrl, String localDir, String relativePath, List<String> downloadFiles) {
        //获取网站上面的目录文件
        Set<String> remoteFiles = fetchFileList(currentUrl);
        Set<String> localFiles = new HashSet<>();
        //本地路径加上分隔符
        String currentLocalDir = localDir.endsWith(File.separator) ? localDir : localDir + File.separator;
        File localDirectory = new File(currentLocalDir);

        //本地路径不存在就创建
        if (!localDirectory.exists()) {
            if (localDirectory.mkdirs()) {
                log.info("创建文件夹成功：{}", localDir);
            } else {
                log.warn("创建文件夹失败：{}", localDir);
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

        // 下载或者更新文件
        remoteFiles.parallelStream().forEach(file -> {

            //不在排除列表里面
            if (!exclude(relativePath + file)) {
                if (file.endsWith("/")) {
                    String localDirName = file.replace("/", "").replaceAll("[\\\\/:*?\"<>|]", "_");
                    // It's a directory, recursively sync
                    syncFilesRecursively(currentUrl + encode(file.replace("/", "")).replace("+", "%20") + "/", currentLocalDir + localDirName, relativePath + file, downloadFiles);
                } else {
                    String localFileName = file.replaceAll("[\\\\/:*?\"<>|]", "_");
                    if (!localFiles.contains(localFileName) || isRemoteFileUpdated(currentUrl, currentLocalDir, encode(file).replace("+", "%20"), localFileName)) {
                        executorService.submit(() -> downloadFile(currentUrl, currentLocalDir, encode(file).replace("+", "%20"), localFileName, downloadFiles));
                    }
                }
            } else {
                log.info("排除路径：{}", relativePath + file);
            }
        });


        //处理成和本地一样的格式 好对比 不然不好对比 本地对特殊字符处理了
        remoteFiles = remoteFiles.stream().map(file -> {
            if (file.endsWith("/")) {
                return file.replace("/", "").replaceAll("[\\\\/:*?\"<>|]", "_") + "/";
            } else {
                //去掉特殊字符  去掉后缀，防止删除同名的nfo等文件
                return file.replaceAll("[\\\\/:*?\"<>|]", "_").substring(0, file.lastIndexOf('.'));
            }
        }).collect(Collectors.toSet());

        // 删除网站上面不存在的本地文件 本地有但是网站上没有的文件 只会删除名单中的文件和文件夹
        for (String file : localFiles) {
            if (!file.endsWith("/")) {
                file = file.substring(0, file.lastIndexOf('.'));
            }
            //远程没有本地这个文件名称  而且在处理列表里面  不在排除列表里面
            if (!remoteFiles.contains(file) && shouldDelete(relativePath + file) && !exclude(relativePath + file)) {
                File localFile = new File(currentLocalDir, file);
                if (localFile.isDirectory()) {
                    deleteDirectory(localFile);
                    if (!localFile.exists()) {
                        log.info("删除过时文件夹成功currentLocalDir:{} Delete fail: {}", currentLocalDir, file);
                    } else {
                        log.warn("删除过时文件夹失败currentLocalDir:{} Deleted: {}", currentLocalDir, file);
                    }
                } else {
                    if (localFile.delete()) {
                        log.info("删除过时文件成功currentLocalDir:{} Deleted: {}", currentLocalDir, file);
                    } else {
                        log.warn("删除过时文件失败currentLocalDir:{} Delete fail: {}", currentLocalDir, file);
                    }
                }
            }
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

    private Set<String> fetchFileList(String url) {
        String decodeUrl = decode(url);
        log.info("开始获取网站文件目录：{}", decodeUrl);
        Set<String> files = new HashSet<>();
        //如果失败尝试获取三次
        for (int i = 0; ; i++) {
            try {
                Document doc = Jsoup.connect(url)
                        .header("User-Agent", userAgent)
                        .get();
                Elements links = doc.select("a[href]");
                for (Element link : links) {
                    String file = link.attr("href");
                    if (!file.equals("../")) {
                        // 使用UTF-8解码中文文件名
                        file = java.net.URLDecoder.decode(file, "UTF-8");
                        files.add(file);
                    }
                }
                log.info("获取网站文件目录成功：{}", decodeUrl);
                return files;
            } catch (IOException e) {
                if (i < 2) {
                    log.warn("第{}次获取{}失败", i + 1, decodeUrl);
                    sleep(1);
                } else {
                    log.warn("第{}次获取{}还是失败，放弃", i + 1, decodeUrl);
                    log.error("", e);
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void downloadFile(String currentUrl, String localDir, String file, String localFileName, List<String> downloadFiles) {
        URL website;
        HttpURLConnection connection;
        try {
            website = new URL(currentUrl + file);
            connection = (HttpURLConnection) website.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", userAgent);
        } catch (IOException e) {
            log.warn("下载文件失败localDir:{} Download fail: {}", localDir, localFileName);
            log.error("", e);
            return;
        }
        for (int i = 0; ; i++) {

            try (
                    ReadableByteChannel rbc = Channels.newChannel(connection.getInputStream());
                    FileOutputStream fos = new FileOutputStream(new File(localDir, localFileName));
                    FileChannel fileChannel = fos.getChannel()
            ) {
                fileChannel.transferFrom(rbc, 0, Long.MAX_VALUE);
                log.info("下载文件成功localDir:{} Downloaded: {}", localDir, localFileName);
                downloadFiles.add(localDir.endsWith(File.separator) ? localDir : localDir + File.separator + localFileName);
                break;
            } catch (IOException e) {
                String decodeCurrentUrl = decode(currentUrl);
                if (i < 2) {
                    log.warn("第{}次下载{}失败", i + 1, decodeCurrentUrl + localFileName);
                    sleep(1);
                } else {
                    log.warn("第{}次下载{}还是失败，放弃", i + 1, decodeCurrentUrl + localFileName);
                    log.warn("下载文件失败localDir:{} Download fail: {}", localDir, localFileName);
                    log.error("", e);
                    break;
                }
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

    private boolean isRemoteFileUpdated(String baseUrl, String localDir, String file, String localFileName) {
        try {
            URL url = new URL(baseUrl + file);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            long remoteLastModified = connection.getLastModified();

            File localFile = new File(localDir, localFileName);
            long localLastModified = localFile.lastModified();

            return remoteLastModified > localLastModified;
        } catch (IOException e) {
            log.error("", e);
            return false;
        }
    }

    private String encode(String str) {
        try {
            return URLEncoder.encode(str, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error("", e);
        }
        return str;
    }

    private String decode(String str) {
        try {
            return URLDecoder.decode(str, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error("", e);
        }
        return str;
    }

    private void sleep(long l) {
        try {
            TimeUnit.SECONDS.sleep(l);
        } catch (InterruptedException e) {
            log.error("", e);
        }
    }

}