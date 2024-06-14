package cn.jackding.xiaoyasync.tgbot;

import cn.jackding.xiaoyasync.SyncService;
import cn.jackding.xiaoyasync.util.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.telegram.abilitybots.api.db.DBContext;
import org.telegram.abilitybots.api.sender.MessageSender;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Slf4j
public class ResponseHandler {

    private final MessageSender sender;

    public ResponseHandler(MessageSender sender, DBContext db) {
        this.sender = sender;
    }

    /**
     * 根据用户的输入同步
     *
     * @param chatId
     * @param parameter
     * @param messageId
     */
    public void replyToSyncDdir(long chatId, String parameter, Integer messageId) {
        try {
            sender.execute(SendMessage.builder().chatId(chatId).replyToMessageId(messageId).text("==开始执行同步指定路径任务==").build());
            SyncService syncService = (SyncService) SpringContextUtil.getBean("syncService");
            syncService.syncFiles(parameter);
        } catch (Exception e) {
            log.error("", e);
        }
    }


}
