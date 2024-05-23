package cn.jackding.xiaoyasync;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@Log4j2
public class XiaoyaSyncApplication {

    @Value("${runAfterStartup:1}")
    private String runAfterStartup;

    public static void main(String[] args) {
        System.setProperty("Log4jContextSelector", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");
        SpringApplication.run(XiaoyaSyncApplication.class, args);
    }

    /**
     * 启动服务立即执行任务
     *
     * @param syncService
     * @return
     */
    @Bean
    CommandLineRunner run(SyncService syncService) {
        return args -> {
            if ("1".equals(runAfterStartup)) {
                syncService.syncFiles();
            } else {
                log.info("启动立即执行任务未启用，等待定时任务处理");
            }
        };
    }

}
