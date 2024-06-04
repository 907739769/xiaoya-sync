package cn.jackding.xiaoyasync.tgbot;

import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * 机器人注册中心
 *
 * @Author Jack
 * @Date 2022/9/2 13:54
 * @Version 1.0.0
 */
public class Telegram {

    public static void registerBot() {
        String botToken = "12345:YOUR_TOKEN";
        try {
            TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication();
            botsApplication.registerBot(botToken, new SyncBot());
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
//        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
//        // Set up Http proxy
//        DefaultBotOptions botOptions = new DefaultBotOptions();
//
//        botOptions.setProxyHost(Config.telegramBotProxyHost);
//        botOptions.setProxyPort(Config.telegramBotProxyPort);
//        botOptions.setProxyType(DefaultBotOptions.ProxyType.HTTP);
//        //使用AbilityBot创建的事件响应机器人
//        telegramBotsApi.registerBot(new MovieAbilityBot(Config.telegramBotToken, Config.telegramBotName, botOptions));
    }

}
