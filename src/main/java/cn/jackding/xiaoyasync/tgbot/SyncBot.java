package cn.jackding.xiaoyasync.tgbot;

import cn.jackding.xiaoyasync.SyncService;
import cn.jackding.xiaoyasync.config.Config;
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
public class SyncBot extends AbilityBot {


    public SyncBot() {
        super(Config.tgToken, "");
    }

    public SyncBot(DefaultBotOptions options) {
        super(Config.tgToken, "", options);
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
                    silent.send("开始执行同步任务", ctx.chatId());
                    new SyncService().syncFiles("每日更新/");
                    silent.send("同步任务执行完成", ctx.chatId());
                })
                .build();
    }

    public Ability syncDir() {
        return Ability.builder()
                .name("syncDir")
                .info("同步指定媒体库路径")
                .privacy(CREATOR)
                .locality(USER)
                .input(0)
                .action(ctx -> {
                    String parameter = ctx.firstArg();
                    if (StringUtils.isBlank(parameter)) {
                        silent.send("请加上路径参数", ctx.chatId());
                        return;
                    }
                    silent.send("开始执行同步指定路径任务", ctx.chatId());
                    new SyncService().syncFiles(parameter);
                    silent.send("同步指定路径任务执行完成", ctx.chatId());
                })
                .build();
    }

}
