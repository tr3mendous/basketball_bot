package com.basketballbot;

import com.aspose.html.HTMLDocument;
import com.aspose.html.dom.Text;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gui.ava.html.image.generator.HtmlImageGenerator;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static com.basketballbot.Constants.FUTURE_GAME_STATUS;
import static com.basketballbot.Constants.MEGASTROY_ID;
import static com.basketballbot.Constants.PASSED_GAME_STATUS;
import static com.basketballbot.Constants.RESULTS;
import static com.basketballbot.Constants.SBER_ID;
import static com.basketballbot.Constants.SCHEDULE;
import static com.basketballbot.Constants.TABLE;

@AllArgsConstructor
public class ResponseHandler {

    private final Bot bot;
    private final long megastroyChatId;
    private final String megastroyScheduleUrl;
    private final String megastroyTableUrl;
    private final String sberScheduleUrl;
    private final String sberTableUrl;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @SneakyThrows
    public void replyToStart(long chatId) {
        var message = getSendMessage(chatId, "Выберите информацию для просмотра");
        message.setReplyMarkup(Keyboard.getKeyboard());
        bot.execute(message);
    }

    @SneakyThrows
    public void replyToStop(long chatId) {
        var message = getSendMessage(chatId, "Бот выключил меню, для повторного запуска введите команду /start");
        message.setReplyMarkup(new ReplyKeyboardRemove(true));
        bot.execute(message);
    }

    public void replyToButtons(long chatId, Message message) {
        var answer = message.getText();
        switch (answer) {
            case SCHEDULE -> replyToSchedule(chatId);
            case RESULTS -> replyToResults(chatId);
            case TABLE -> replyToTable(chatId);
        }
    }

    @SneakyThrows
    private void replyToSchedule(long chatId) {
        var isMegastroy = megastroyChatId == chatId;
        replyToGames(chatId, isMegastroy ? megastroyScheduleUrl : sberScheduleUrl, isMegastroy ? MEGASTROY_ID : SBER_ID, FUTURE_GAME_STATUS);
    }

    @SneakyThrows
    private void replyToResults(long chatId) {
        var isMegastroy = megastroyChatId == chatId;
        replyToGames(chatId, isMegastroy ? megastroyScheduleUrl : sberScheduleUrl, megastroyChatId == chatId ? MEGASTROY_ID : SBER_ID, PASSED_GAME_STATUS);
    }


    @SneakyThrows
    private void replyToTable(long chatId) {
        var isMegastroy = megastroyChatId == chatId;
        var tablePlaces = mapper.readValue(getResponseBody(isMegastroy ? megastroyTableUrl : sberTableUrl), new TypeReference<List<Table>>() {
        });

        var document = new HTMLDocument();
        var style = document.createElement("style");
        style.setTextContent("table, th, td { border: 10px solid #0000ff; }");
        var head = document.getElementsByTagName("head").get_Item(0);
        head.appendChild(style);
        var body = document.getBody();
        int cols = 5;
        int rows = tablePlaces.size();
        var table = document.createElement("table");
        var tableBody = document.createElement("tbody");
        table.appendChild(tableBody);
        var row = document.createElement("tr");
        tableBody.appendChild(row);

        for (int j = 1; j < cols + 1; j++) {
            var header = document.createElement("th style = \"text-align: center;\"");
            Text title = null;
            switch (j) {
                case 1 -> title = document.createTextNode("Место");
                case 2 -> title = document.createTextNode("Название команды");
                case 3 -> title = document.createTextNode("Игр всего");
                case 4 -> title = document.createTextNode("Победы");
                case 5 -> title = document.createTextNode("Поражения");
            }
            header.appendChild(title);
            row.appendChild(header);
        }
        for (int i = 0; i < rows; i++) {
            var rowData = i % 2 == 0 ? document.createElement("tr") : document.createElement("tr style = \"background-color: #D3D3D3;\"");
            tableBody.appendChild(rowData);

            for (int j = 1; j < cols + 1; j++) {
                var cell = document.createElement("td style = \"text-align: center;\"");
                Text title = null;
                switch (j) {
                    case 1 -> title = document.createTextNode(tablePlaces.get(i).getPlace().toString());
                    case 2 -> title = document.createTextNode(tablePlaces.get(i).getTeamName().getTeamName());
                    case 3 -> title = document.createTextNode(tablePlaces.get(i).getGames().toString());
                    case 4 -> title = document.createTextNode(tablePlaces.get(i).getWon().toString());
                    case 5 -> title = document.createTextNode(tablePlaces.get(i).getLost().toString());
                }
                cell.appendChild(title);
                rowData.appendChild(cell);
            }
        }
        body.appendChild(table);

        var imageGenerator = new HtmlImageGenerator();
        imageGenerator.loadHtml(table.getOuterHTML());
        var image = imageGenerator.getBufferedImage();
        var outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        var file = new InputFile(new ByteArrayInputStream(outputStream.toByteArray()), "1");

        var photo = SendPhoto.builder()
                .chatId(chatId)
                .photo(file)
                .disableNotification(true)
                .build();
        bot.execute(photo);
    }

    private SendMessage getSendMessage(long chatId, String text) {
        return SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .disableNotification(true)
                .parseMode("HTML")
                .build();
    }

    @SneakyThrows
    private void replyToGames(long chatId, String url, Integer teamId, Integer gameStatus) {
        var gamesList = mapper.readValue(getResponseBody(url), new TypeReference<List<Game>>() {
                }).stream()
                .filter(game -> teamId.equals(game.getFirstTeamId()) || teamId.equals(game.getSecondTeamId()))
                .toList();
        if (gamesList.isEmpty()) {
            bot.execute(getSendMessage(chatId, "Нет расписания ближайших игр"));
        } else {
            var games = gamesList.stream()
                    .filter(game -> gameStatus.equals(game.getStatus()))
                    .map(game -> {
                        var firstPart = game.getGameDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) +
                                ", " +
                                game.getGameTime() +
                                ", " +
                                game.getGameDate().getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("ru", "RU")) +
                                ", " +
                                game.getFirstTeamName();
                        var secondPart = FUTURE_GAME_STATUS.equals(gameStatus) ? ":" : " " +
                                game.getFirstTeamScore() +
                                ":" +
                                game.getSecondTeamScore() +
                                " ";
                        var thirdPart = game.getSecondTeamName() +
                                ", " +
                                game.getCourt().replace("BasketHall. ", "");

                        return firstPart + secondPart + thirdPart + System.lineSeparator();
                    })
                    .collect(Collectors.joining(System.lineSeparator()));
            bot.execute(getSendMessage(chatId, games));
        }
    }

    private String getResponseBody(String url) throws IOException, InterruptedException {
        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }
}