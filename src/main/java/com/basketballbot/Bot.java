package com.basketballbot;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.bot.BaseAbilityBot;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.objects.Reply;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import static org.telegram.abilitybots.api.objects.Locality.ALL;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;
import static org.telegram.abilitybots.api.util.AbilityUtils.getChatId;

@Component
@Setter
@Getter
@Slf4j
public class Bot extends AbilityBot {

    private final ResponseHandler responseHandler;
    private final String userName;

    public Bot(@Value("${bot.token}") String token,
               @Value("${bot.user-name}") String userName,
               @Value("${bot.megastroy-chat-id}") long megastroyChatId,
               @Value("${bot.megastroy-schedule-url}") String megastroyScheduleUrl,
               @Value("${bot.megastroy-table-url}") String megastroyTableUrl,
               @Value("${bot.sber-schedule-url}") String sberScheduleUrl,
               @Value("${bot.sber-table-url}") String sberTableUrl) {
        super(token, userName);
        this.userName = userName;
        responseHandler = new ResponseHandler(this, megastroyChatId, megastroyScheduleUrl, megastroyTableUrl, sberScheduleUrl, sberTableUrl);
    }

    @Override
    public long creatorId() {
        return 1L;
    }

    @Override
    public void onUpdatesReceived(List<Update> updates) {
        super.onUpdatesReceived(updates);
    }

    @Override
    public void onClosing() {
        super.onClosing();
    }

    @Override
    public String getBotUsername() {
        return userName;
    }

    public Ability startBot() {
        return Ability.builder()
                .name("start")
                .locality(ALL)
                .privacy(PUBLIC)
                .action(messageContext -> responseHandler.replyToStart(messageContext.chatId()))
                .build();
    }

    public Ability stopBot() {
        return Ability.builder()
                .name("stop")
                .locality(ALL)
                .privacy(PUBLIC)
                .action(messageContext -> responseHandler.replyToStop(messageContext.chatId()))
                .build();
    }

    public Reply replyToButtons() {
        List<Predicate<Update>> conditions = List.of(update -> !update.getMessage().isCommand());
        BiConsumer<BaseAbilityBot, Update> action = (abilityBot, upd) -> responseHandler.replyToButtons(getChatId(upd), upd.getMessage());
        return Reply.of(action, conditions);
    }
}
