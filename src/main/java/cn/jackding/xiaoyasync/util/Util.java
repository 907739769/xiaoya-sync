package cn.jackding.xiaoyasync.util;

import cn.jackding.xiaoyasync.config.Config;
import cn.jackding.xiaoyasync.tgbot.SyncBot;
import cn.jackding.xiaoyasync.tgbot.TgSendMsg;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @Author Jack
 * @Date 2024/5/24 13:41
 * @Version 1.0.0
 */
@Log4j2
public class Util {

    public static final Random random = new Random();

    public static String encode(String str) {
        try {
            return URLEncoder.encode(str, "UTF-8").replace("+", "%20").replace("%2F", "/");
        } catch (UnsupportedEncodingException e) {
            log.error("", e);
        }
        return str;
    }

    public static String decode(String str) {
        try {
            return URLDecoder.decode(str, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error("", e);
        }
        return str;
    }

    /**
     * 获取一个随机的userAgent
     *
     * @return
     */
    public static String userAgent() {
        int firstNum = random.nextInt(20) + 55;
        int thirdNum = random.nextInt(3800);
        int fourthNum = random.nextInt(140);
        List<String> osTypes = Arrays.asList("(Windows NT 6.1; WOW64),(Windows NT 10.0; WOW64),(X11; Linux x86_64),(Macintosh; Intel Mac OS X 10_14_5)".split(","));
        String chromeVersion = String.format("Chrome/%d.0.%d.%d", firstNum, thirdNum, fourthNum);
        return "Mozilla/5.0 " + osTypes.get(random.nextInt(osTypes.size())) + " AppleWebKit/537.36 (KHTML, like Gecko) " + chromeVersion + " Safari/537.36";
    }

    public static void sleep(long l) {
        try {
            TimeUnit.SECONDS.sleep(l);
        } catch (InterruptedException e) {
            log.error("", e);
        }
    }

    public static void randomSleep() {
        try {
            TimeUnit.SECONDS.sleep(random.nextInt(60));
        } catch (InterruptedException e) {
            log.error("", e);
        }
    }

    public static void sendTgMsg(String msg) {
        new TgSendMsg().sendMsg(msg);
    }

    public static void initBot() {
        if (StringUtils.isBlank(Config.tgUserId) || StringUtils.isBlank(Config.tgToken)) {
            return;
        }
        TelegramBotsApi telegramBotsApi;
        try {
            telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        } catch (TelegramApiException e) {
            log.error("", e);
            return;
        }
        DefaultBotOptions botOptions = new DefaultBotOptions();
//        botOptions.setProxyHost(Config.telegramBotProxyHost);
//        botOptions.setProxyPort(Config.telegramBotProxyPort);
//        botOptions.setProxyType(DefaultBotOptions.ProxyType.HTTP);
        //使用AbilityBot创建的事件响应机器人
        try {
            telegramBotsApi.registerBot(new SyncBot(botOptions));
        } catch (TelegramApiException e) {
            log.error("", e);
        }
    }

}
