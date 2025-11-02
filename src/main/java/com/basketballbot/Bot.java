package com.basketballbot;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.PinChatMessage;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.UnpinChatMessage;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;

import static com.basketballbot.Team.MEGASTROY;
import static com.basketballbot.Team.SBER;
import static java.lang.String.format;
import static java.time.DayOfWeek.MONDAY;

@Component
@Setter
@Getter
@Slf4j
public class Bot extends TelegramLongPollingBot {

    private static final String USER_NAME = "basketball_poll_bot";
    private static final List<String> OPTIONS = List.of("Буду", "Не смогу");
    private static final String POLL_TEXT = "Тренировка в %s %s.%s %s";

    private final String megastroyChatId;
    private final String sberChatId;
    private final String botUrl;
    private static Integer megastroyMessageId;
    private static Integer sberMessageId;

    public Bot(@Value("${bot.token}") String token,
               @Value("${bot.megastroy-chat-id}") String megastroyChatId,
               @Value("${bot.sber-chat-id}") String sberChatId,
               @Value("${bot.url}") String botUrl) {
        super(token);
        this.megastroyChatId = megastroyChatId;
        this.sberChatId = sberChatId;
        this.botUrl = botUrl;
    }

    @Override
    public String getBotUsername() {
        return USER_NAME;
    }

    @Override
    public void onUpdateReceived(Update update) {
    }

    @Scheduled(cron = "${cron.megastroy-poll}")
    public void sendMegastroyScheduledPoll() throws TelegramApiException {
        var workoutDate = getWorkoutDate();
        String dayOfTheWeek;
        String time;
        if (MONDAY == workoutDate.getDayOfWeek()) {
            dayOfTheWeek = "понедельник";
            time = "19:00";
        } else {
            dayOfTheWeek = "четверг";
            time = "20:30";
        }

        sendPoll(megastroyChatId, workoutDate, dayOfTheWeek, time, MEGASTROY);
    }

    @Scheduled(cron = "${cron.megastroy-unpin}")
    public void unpinMegastroyScheduledPoll() throws TelegramApiException {
        unpinPoll(megastroyChatId, megastroyMessageId, MEGASTROY);
    }

    @Scheduled(cron = "${cron.sber-poll}")
    public void sendSberScheduledPoll() throws TelegramApiException {
        sendPoll(sberChatId, getWorkoutDate(), "среду", "19:00", SBER);
    }

    @Scheduled(cron = "${cron.sber-unpin}")
    public void unpinSberScheduledPoll() throws TelegramApiException {
        unpinPoll(sberChatId, sberMessageId, SBER);
    }

    @Scheduled(cron = "${cron.ping}")
    public void scheduledPing() {
        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder()
                .uri(URI.create(botUrl + "/ping"))
                .GET()
                .build();
        try {
            client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Scheduled(cron = "${cron.google-ping}")
    public void scheduledRequest() {
        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.google.com"))
                .GET()
                .build();
        try {
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != HttpStatus.OK.value()) {
                throw new RuntimeException();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        log.info("Google ping was successful");
    }

    private void sendPoll(String chatId, LocalDate workoutDate, String dayOfTheWeek, String time, Team team)
            throws TelegramApiException {
        var question = format(POLL_TEXT, dayOfTheWeek, workoutDate.getDayOfMonth(), workoutDate.getMonth().getValue(), time);

        var poll = SendPoll.builder()
                .chatId(chatId)
                .question(question)
                .options(OPTIONS)
                .isAnonymous(false)
                .build();
        var messageId = execute(poll).getMessageId();

        var pinMessage = PinChatMessage.builder()
                .chatId(chatId)
                .messageId(messageId)
                .build();
        execute(pinMessage);

        if (MEGASTROY == team) {
            megastroyMessageId = messageId;
        } else {
            sberMessageId = messageId;
        }
        log.info("Poll was sent to {} team chat on {}", team, LocalDate.now());
    }

    private void unpinPoll(String chatId, Integer messageId, Team team) throws TelegramApiException {
        var pinMessage = UnpinChatMessage.builder()
                .chatId(chatId)
                .messageId(messageId)
                .build();
        execute(pinMessage);
        log.info("Poll was unpinned from {} team chat on {}", team, LocalDate.now());
    }

    private LocalDate getWorkoutDate() {
        return LocalDate.now().plusDays(1);
    }
}
