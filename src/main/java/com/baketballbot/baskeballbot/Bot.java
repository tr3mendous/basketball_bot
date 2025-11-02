package com.baketballbot.baskeballbot;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.PinChatMessage;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.util.List;

import static java.time.DayOfWeek.SUNDAY;

@Component
public class Bot extends TelegramLongPollingBot {

    private final static String USER_NAME = "basketball_poll_bot";
    private final static String TOKEN = "8472616578:AAGO2LsSz23DY-pITP6DeVMh7dcN3kTJJDc";
    private final static List<String> OPTIONS = List.of("Буду", "Не смогу");
    private final static String POLL_TEXT = "Тренировка в %s %s.%s";

    public Bot() {
        super(TOKEN);
    }

    @Override
    public String getBotUsername() {
        return USER_NAME;
    }

    @Override
    public void onUpdateReceived(Update update) {
    }

    @Scheduled(cron = "${cron.megastroy}")
    public void sendScheduledMessage() throws TelegramApiException {
        var date = LocalDate.now();
        //String chatId = "-1001597722413"; // Replace with the chat ID where you want to send the message
        var chatId = "-1003125449732";
        var dayOfTheWeek = SUNDAY == date.getDayOfWeek() ? "понедельник" : "четверг";
        var workoutDate = date.plusDays(1);
        var question = String.format(POLL_TEXT, dayOfTheWeek, workoutDate.getDayOfMonth(), workoutDate.getMonth().getValue());

        var poll = SendPoll.builder()
                .chatId(chatId)
                .question(question)
                .options(OPTIONS)

                .isAnonymous(false)
                .build();

        var message = execute(poll);
        var pinChatMessage = PinChatMessage.builder()
                .chatId(chatId)
                .messageId(message.getMessageId())
                .disableNotification(true)
                .build();
        execute(pinChatMessage);
    }
}
