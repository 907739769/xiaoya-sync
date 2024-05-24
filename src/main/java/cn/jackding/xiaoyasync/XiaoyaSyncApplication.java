package cn.jackding.xiaoyasync;

import lombok.extern.log4j.Log4j2;
import org.jsoup.internal.StringUtil;
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

    @Value("${syncDir}")
    private String syncDir;

    public static void main(String[] args) {
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
                if(StringUtil.isBlank(syncDir)){
                    syncDir="每日更新/";
                }
                syncService.syncFiles(syncDir);
            } else {
                log.info("启动立即执行任务未启用，等待定时任务处理");
            }
        };
    }

}
