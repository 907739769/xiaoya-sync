package cn.jackding.xiaoyasync;

import cn.jackding.xiaoyasync.util.Util;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * @Author Jack
 * @Date 2024/5/24 13:32
 * @Version 1.0.0
 */
@Log4j2
@Service
public class ScheduledTask {

    @Autowired
    private SyncService syncService;

    /**
     * 每日更新
     */
    @Scheduled(cron = "0 0 6,18 * * ?")
    public void syncUpdatedDaily() {
        Util.randomSleep();
        syncService.syncFiles("每日更新/");
    }

    /**
     * 全量同步
     */
    @Scheduled(fixedDelay = 1000 * 60 * 60 * 24 * 3, initialDelay = 1000 * 60 * 60 * 24 * 3)
    public void syncAll() {
        syncService.syncFiles("");
    }


}
