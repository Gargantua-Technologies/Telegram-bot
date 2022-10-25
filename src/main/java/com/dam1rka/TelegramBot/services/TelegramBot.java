package com.dam1rka.TelegramBot.services;

import com.dam1rka.TelegramBot.config.BotConfig;
import com.dam1rka.TelegramBot.models.BotCommands;
import com.dam1rka.TelegramBot.services.interfaces.ITelegramService;
import com.dam1rka.TelegramBot.services.telegram.StartService;
import com.dam1rka.TelegramBot.services.telegram.UnknownCommandService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class TelegramBot extends TelegramLongPollingBot {
    private final BotConfig config;
    private final List<BotCommand> listOfCommands;
    private final HashMap<String, ITelegramService> services;

    private final UnknownCommandService unknownCommandService;


    @Autowired
    public TelegramBot(BotConfig config) {
        this.config = config;
        listOfCommands = new ArrayList<>();
        services = new HashMap<>();
        for (BotCommands command : BotCommands.values()) {
            listOfCommands.add(new BotCommand(command.toString(), command.getDescription()));
            services.put(command.toString(), command.getService());
        }

        unknownCommandService = new UnknownCommandService();

        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bots command list: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getBotToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();

            ITelegramService service = services.get(messageText);

            if(Objects.nonNull(service)) {
                service.handle(update);
                try {
                    execute(service.getResult());
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            } else {
                unknownCommandService.handle(update);
                try {
                    execute(unknownCommandService.getResult());
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }

        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
        }
    }

}
