package com.basketballbot;

import java.util.List;

public class Constants {

    public static final List<String> OPTIONS = List.of("Буду", "Не смогу");
    public static final List<String> GAME_OPTIONS = List.of("Буду", "Не смогу", "Просто смотрю");
    public static final String POLL_TEXT = "Тренировка %s %s %s";
    public static final String GAME_POLL_TEXT = "Игра %s %s %s, %s, %s";
    public static final String SCHEDULE = "Расписание";
    public static final String RESULTS = "Результаты";
    public static final String TABLE = "Положение команд";
    public static final Integer PASSED_GAME_STATUS = 1;
    public static final Integer FUTURE_GAME_STATUS = 0;
    public static final Integer MEGASTROY_ID = 23713;
    public static final Integer SBER_ID = 15936;
}
