package cn.jackding.xiaoyasync;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@Slf4j
public class XiaoyaSyncApplication {

    @Value("${runAfterStartup:1}")
    private String runAfterStartup;

    @Value("${threadPoolNum:199}")
    private String threadPoolNum;

    public static void main(String[] args) {
        SpringApplication.run(XiaoyaSyncApplication.class, args);
    }

    @Bean
    CommandLineRunner run(SyncService syncService) {
        return args -> {
            System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", threadPoolNum);
            if ("1".equals(runAfterStartup)) {
                syncService.syncFiles();
            } else {
                log.info("启动立即执行任务未启用，等待定时任务处理");
            }
        };
    }

}
