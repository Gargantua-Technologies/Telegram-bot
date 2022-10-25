package com.dam1rka.TelegramBot.services.telegram;

import com.dam1rka.TelegramBot.entities.UserEntity;
import com.dam1rka.TelegramBot.repositories.UserRepository;
import com.dam1rka.TelegramBot.services.interfaces.TelegramServiceImpl;
import com.vdurmont.emoji.EmojiParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class RegistrationService extends TelegramServiceImpl {
    private final UserRepository userRepository;
    @Autowired
    public RegistrationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    SendMessage message = new SendMessage();

    @Override
    public void handleCommand(Update update) {
        super.handleCommand(update);

        if(Objects.nonNull(userRepository.findByTelegramId(update.getMessage().getFrom().getId()))) {
            message.setText(EmojiParser.parseToUnicode("Your already registered :open_mouth:"));
            message.setChatId(update.getMessage().getChatId());
        } else {
            ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
            message.setReplyMarkup(keyboardMarkup);

            List<KeyboardRow> keyboardRows = new ArrayList<>();
            {
                KeyboardRow keyboardRow = new KeyboardRow();
                KeyboardButton requestContact = new KeyboardButton();
                requestContact.setText("Send contact");
                requestContact.setRequestContact(true);
                keyboardRow.add(requestContact);
                keyboardRows.add(keyboardRow);
            }

            keyboardMarkup.setKeyboard(keyboardRows);
            keyboardMarkup.setResizeKeyboard(true);
            keyboardMarkup.setOneTimeKeyboard(true);

            message.setText(EmojiParser.parseToUnicode("Please, send your contact for complete registration :innocent:"));
            message.setChatId(update.getMessage().getChatId());
        }
    }

    @Override
    public boolean handleOther(Update update) {
        if(update.getMessage().hasContact()) {
            Contact contact = update.getMessage().getContact();

            if(Objects.isNull(userRepository.findByTelegramId(contact.getUserId()))) {
                UserEntity user = new UserEntity();
                user.setCreated(LocalDateTime.now());
                user.setTelegramId(contact.getUserId());
                user.setPhone(contact.getPhoneNumber());
                user.setUsername(update.getMessage().getChat().getUserName());
                userRepository.save(user);
                message.setText("Registration completed!");
            } else {
                message.setText("You already registered!");
            }
            message.setReplyMarkup(new ReplyKeyboardRemove(true));
            message.setChatId(update.getMessage().getChatId());
            return true;
        }
        return false;
    }

    @Override
    public BotApiMethod getResult() {
        return message;
    }
}
