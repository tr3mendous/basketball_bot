package com.basketballbot;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.db.DBContext;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.PinChatMessage;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.UnpinChatMessage;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.basketballbot.Constants.*;
import static com.basketballbot.MessageType.*;
import static com.basketballbot.Team.MEGASTROY;
import static com.basketballbot.Team.SBER;
import static java.lang.String.format;
import static java.time.DayOfWeek.*;

@Component
@Slf4j
public class ScheduleService {

    private final Bot bot;
    private final long megastroyChatId;
    private final String megastroyScheduleUrl;
    private final long sberChatId;
    private final String sberScheduleUrl;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final Map<MessageType, Message> messages;

    public ScheduleService(Bot bot,
                           @Value("${bot.megastroy-chat-id}") long megastroyChatId,
                           @Value("${bot.sber-chat-id}") long sberChatId,
                           @Value("${bot.megastroy-schedule-url}") String megastroyScheduleUrl,
                           @Value("${bot.sber-schedule-url}") String sberScheduleUrl,
                           DBContext db) {
        this.bot = bot;
        this.megastroyChatId = megastroyChatId;
        this.sberChatId = sberChatId;
        this.megastroyScheduleUrl = megastroyScheduleUrl;
        this.sberScheduleUrl = sberScheduleUrl;
        this.messages = db.getMap("messages");
    }

    @Scheduled(cron = "${cron.megastroy-poll}")
    public void sendMegastroyScheduledPoll() throws TelegramApiException {
        var workoutDate = getWorkoutDate();
        String dayOfTheWeek;
        String time;
        if (MONDAY == workoutDate.getDayOfWeek()) {
            dayOfTheWeek = MONDAY.getDisplayName(TextStyle.FULL, new Locale("ru", "RU"));
            time = "19:00";
        } else {
            dayOfTheWeek = THURSDAY.getDisplayName(TextStyle.FULL, new Locale("ru", "RU"));
            time = "20:30";
        }

        sendPoll(megastroyChatId, workoutDate, dayOfTheWeek, time, MEGASTROY);
    }

    @Scheduled(cron = "${cron.megastroy-unpin}")
    public void unpinMegastroyScheduledPoll() throws TelegramApiException {
        unpinPoll(megastroyChatId, messages.get(MEGASTROY_POLL_MESSAGE), MEGASTROY);
    }

    @Scheduled(cron = "${cron.sber-poll}")
    public void sendSberScheduledPoll() throws TelegramApiException {
        sendPoll(sberChatId, getWorkoutDate(), WEDNESDAY.getDisplayName(TextStyle.FULL, new Locale("ru", "RU")), "19:00", SBER);
    }

    @Scheduled(cron = "${cron.sber-unpin}")
    public void unpinSberScheduledPoll() throws TelegramApiException {
        unpinPoll(sberChatId, messages.get(SBER_POLL_MESSAGE), SBER);
    }

    @Scheduled(cron = "${cron.game-poll}")
    public void sendMegastroyScheduledGamePoll() throws IOException, InterruptedException, TelegramApiException {
        sendGamePoll(megastroyScheduleUrl, MEGASTROY_ID, megastroyChatId);
    }

    @Scheduled(cron = "${cron.game-poll}")
    public void sendSberScheduledGamePoll() throws IOException, InterruptedException, TelegramApiException {
        sendGamePoll(sberScheduleUrl, SBER_ID, sberChatId);
    }

    @Scheduled(cron = "${cron.unpin-game-poll}")
    public void unpinGamePoll() throws TelegramApiException {
        var date = LocalDate.now();
        var megastroyGamePollMessage = messages.get(MEGASTROY_GAME_POLL_MESSAGE);
        if (date.equals(getGamePollDate(megastroyGamePollMessage))) {
            unpinPoll(megastroyChatId, megastroyGamePollMessage, MEGASTROY);
        }
        var sberGamePollMessage = messages.get(SBER_GAME_POLL_MESSAGE);
        if (date.equals(getGamePollDate(sberGamePollMessage))) {
            unpinPoll(sberChatId, sberGamePollMessage, SBER);
        }
    }

    private void sendPoll(long chatId, LocalDate workoutDate, String dayOfTheWeek, String time, Team team)
            throws TelegramApiException {
        var question = format(POLL_TEXT, dayOfTheWeek, workoutDate.format(DateTimeFormatter.ofPattern("dd.MM")), time);

        var poll = SendPoll.builder()
                .chatId(chatId)
                .question(question)
                .options(OPTIONS)
                .isAnonymous(false)
                .build();
        var message = bot.execute(poll);

        var pinMessage = PinChatMessage.builder()
                .chatId(chatId)
                .messageId(message.getMessageId())
                .build();
        bot.execute(pinMessage);

        if (MEGASTROY == team) {
            messages.put(MEGASTROY_POLL_MESSAGE, message);
        } else {
            messages.put(SBER_POLL_MESSAGE, message);
        }
        log.info("Poll was sent to {} team chat on {}", team, LocalDate.now());
    }

    private void unpinPoll(long chatId, Message message, Team team) throws TelegramApiException {
        var unpinMessage = UnpinChatMessage.builder()
                .chatId(chatId)
                .messageId(message.getMessageId())
                .build();
        bot.execute(unpinMessage);
        log.info("Poll was unpinned from {} team chat on {}", team, LocalDate.now());
    }

    private LocalDate getWorkoutDate() {
        return LocalDate.now().plusDays(1);
    }

    private void sendGamePoll(String url, Integer teamId, long chatId) throws IOException, InterruptedException, TelegramApiException {
        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
        var nextGame = mapper.readValue(response, new TypeReference<List<Game>>() {
                }).stream()
                .filter(game -> teamId.equals(game.getFirstTeamId()) || teamId.equals(game.getSecondTeamId()))
                .filter(game -> FUTURE_GAME_STATUS.equals(game.getStatus()))
                .filter(game -> LocalDate.now().equals(game.getGameDate().minusDays(1)))
                .min(Comparator.comparing(Game::getGameDate));
        if (nextGame.isPresent()) {
            var game = nextGame.get();
            var formatter = DateTimeFormatter.ofPattern("dd.MM");
            var question = format(GAME_POLL_TEXT,
                    game.getGameDate().getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("ru", "RU")),
                    game.getGameDate().format(formatter),
                    game.getGameTime(),
                    teamId.equals(game.getFirstTeamId()) ? game.getSecondTeamName() : game.getFirstTeamName(),
                    game.getCourt().replace("BasketHall. ", "")
            );
            var poll = SendPoll.builder()
                    .chatId(chatId)
                    .question(question)
                    .options(GAME_OPTIONS)
                    .isAnonymous(false)
                    .build();
            var message = bot.execute(poll);

            var pinMessage = PinChatMessage.builder()
                    .chatId(chatId)
                    .messageId(message.getMessageId())
                    .build();
            bot.execute(pinMessage);

            if (MEGASTROY_ID.equals(teamId)) {
                messages.put(MEGASTROY_GAME_POLL_MESSAGE, message);
            } else {
                messages.put(SBER_GAME_POLL_MESSAGE, message);
            }
            log.info("Game poll was sent to {} team chat on {}", MEGASTROY_ID.equals(teamId) ? MEGASTROY : SBER, LocalDate.now());
        }
    }

    private LocalDate getGamePollDate(Message message) {
        var unixGamePollDate = message.getDate() * 1000L;
        var instant = Instant.ofEpochMilli(unixGamePollDate);
        return LocalDate.ofInstant(instant, ZoneId.systemDefault());
    }
}
