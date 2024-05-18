package cn.jackding.xiaoyasync;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class XiaoyaSyncApplication {

    public static void main(String[] args) {
        SpringApplication.run(XiaoyaSyncApplication.class, args);
    }

    @Bean
    CommandLineRunner run(SyncService syncService) {
        return args -> {
            syncService.syncFiles();
        };
    }

}
