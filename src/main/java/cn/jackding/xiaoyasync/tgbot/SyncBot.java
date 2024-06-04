package cn.jackding.xiaoyasync.tgbot;

import cn.jackding.xiaoyasync.SyncService;
import cn.jackding.xiaoyasync.config.Config;
import cn.jackding.xiaoyasync.util.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.telegrambots.bots.DefaultBotOptions;

import static org.telegram.abilitybots.api.objects.Locality.USER;
import static org.telegram.abilitybots.api.objects.Privacy.CREATOR;

/**
 * @Author Jack
 * @Date 2024/6/4 21:33
 * @Version 1.0.0
 */
@Slf4j
public class SyncBot extends AbilityBot {


    public SyncBot() {
        super(Config.tgToken, "");
    }

    public SyncBot(DefaultBotOptions options) {
        super(Config.tgToken, "bot", options);
    }

    @Override
    public long creatorId() {
        return Long.parseLong(Config.tgUserId);
    }

    public Ability sync() {
        return Ability.builder()
                .name("sync")
                .info("同步媒体库")
                .privacy(CREATOR)
                .locality(USER)
                .input(0)
                .action(ctx -> {
                    silent.send("==开始执行同步任务==", ctx.chatId());
                    SyncService syncService = (SyncService) SpringContextUtil.getBean("syncService");
                    syncService.syncFilesDaily();
                })
                .build();
    }

    public Ability syncDir() {
        return Ability.builder()
                .name("syncdir")
                .info("同步指定媒体库路径")
                .privacy(CREATOR)
                .locality(USER)
                .input(0)
                .action(ctx -> {
                    String parameter;
                    try {
                        parameter = ctx.firstArg();
                    } catch (Exception e) {
                        silent.send("请加上路径参数", ctx.chatId());
                        log.error("", e);
                        return;
                    }
                    if (StringUtils.isBlank(parameter)) {
                        silent.send("请加上路径参数", ctx.chatId());
                        return;
                    }
                    silent.send("==开始执行同步指定路径任务==", ctx.chatId());
                    SyncService syncService = (SyncService) SpringContextUtil.getBean("syncService");
                    syncService.syncFiles(parameter);
                })
                .build();
    }

}
