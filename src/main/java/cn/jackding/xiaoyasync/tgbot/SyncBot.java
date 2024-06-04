package cn.jackding.xiaoyasync.tgbot;

import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * @Author Jack
 * @Date 2024/6/4 17:17
 * @Version 1.0.0
 */
public class SyncBot implements LongPollingSingleThreadUpdateConsumer {
    private TelegramClient telegramClient = new OkHttpTelegramClient("");

    @Override
    public void consume(Update update) {
        // TODO
    }

}
