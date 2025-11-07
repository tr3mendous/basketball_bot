package com.basketballbot;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.List;

public class Keyboard {

    public static ReplyKeyboard getKeyboard() {
        KeyboardRow row = new KeyboardRow();
        row.add("Расписание");
        row.add("Результаты");
        row.add("Положение команд");
        var keyboard = new ReplyKeyboardMarkup(List.of(row));
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(true);
        return keyboard;
    }
}
