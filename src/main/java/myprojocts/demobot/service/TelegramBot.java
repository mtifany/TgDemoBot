package myprojocts.demobot.service;

import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import myprojocts.demobot.config.BotConfig;
import myprojocts.demobot.model.User;
import myprojocts.demobot.model.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private UserRepository userRepository;
   final BotConfig config;

   static final String HELP_TEXT =  "This is my first telegram bot \n" +
           "You can execute commands from menu or by typing the commands: \n" +
           "Type /start to see welcome message\n" +
           "Type /mydata to see your data stored \n" +
           "Type /help to see this message again\n";

    public TelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
         listOfCommands.add(new BotCommand("/start", "get welcome message"));
        listOfCommands.add(new BotCommand("/register", "registration in bot"));
        listOfCommands.add(new BotCommand("/mydata","get your data"));
        listOfCommands.add(new BotCommand("/deletedata","delete your data"));
        listOfCommands.add(new BotCommand("/help","info how to use this bot"));
        listOfCommands.add(new BotCommand("/settings","set  bot settings"));
        try {
            execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        }
        catch (TelegramApiException e) {
            log.error("Error setting bot's command list " + e.getMessage());
        }

    }


    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {

        if(update.hasMessage() && update.getMessage().hasText()){
    String message = update.getMessage().getText();
    long chatId = update.getMessage().getChatId();

    if ( message.contains("/send ") && config.getOwnerId() == chatId){
        var textToSend = EmojiParser.parseToUnicode(message.substring(6));
        var users = userRepository.findAll();
        for(User user : users){
            sendMessage(user.getChatId(),textToSend);
        }
    }

            switch (message) {
                case "/start" -> {
                    registerUser(update.getMessage());
                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                }
                case "/register" ->
                    register(chatId);

                case "/help" ->
                    sendMessage(chatId, HELP_TEXT);

                default -> sendMessage(chatId, "Sorry, command was not recognized");
            }
} else if (update.hasCallbackQuery()) {

            String callBackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callBackData.equals("YES_BUTTON")){
                String text = "You pressed YES button";
                EditMessageText messageText = new EditMessageText(text);
                messageText.setChatId(String.valueOf(chatId));
                messageText.setMessageId((int) messageId);

                try {
                    execute(messageText);
                }
                catch (TelegramApiException e) {
                    log.error("Error occurred " + e.getMessage());

                }
            } else if (callBackData.equals("NO_BUTTON")) {
                String text = "You pressed NO button";
                EditMessageText messageText = new EditMessageText(text);
                messageText.setChatId(String.valueOf(chatId));
                messageText.setMessageId((int) messageId);
                try {
                    execute(messageText);
                }
                catch (TelegramApiException e) {
                    log.error("Error occurred " + e.getMessage());

                }

            }

        }
    }

    private void register( long chatId) {

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId) );
        message.setText("do you really want to register?");

        InlineKeyboardMarkup inlineMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
         List<InlineKeyboardButton> inLineButtons = new ArrayList<>();

         var yesButton = new InlineKeyboardButton();
        yesButton.setText("Yes");
        yesButton.setCallbackData("YES_BUTTON");

        var noButton = new InlineKeyboardButton();
        noButton.setText("No");
        noButton.setCallbackData("NO_BUTTON");


        inLineButtons.add(yesButton);
        inLineButtons.add(noButton);

        rowsInLine.add(inLineButtons);
        inlineMarkup.setKeyboard(rowsInLine);

        message.setReplyMarkup(inlineMarkup);

        try {
            execute(message);
        }
        catch (TelegramApiException e) {
            log.error("Error occurred " + e.getMessage());

        }

    }

    private void registerUser(Message msg) {

    if (userRepository.findById(msg.getChatId()).isEmpty()){

        var chatId = msg.getChatId();
        var chat = msg.getChat();
        User user = new User();

        user.setChatId(chatId);
        user.setFirstName(chat.getFirstName());
        user.setLastName(chat.getLastName());
        user.setUserName(chat.getUserName());
        user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

        userRepository.save(user);
        log.info("user saved: " + user) ;
    }
    }

    private void startCommandReceived(long chatId, String name){

        String answer = EmojiParser.parseToUnicode("Hi " + name + ", this is my telegram bot! " + ":blush:");
        log.error("Replied to user " + name);

        sendMessage(chatId,answer);
    }

    private void sendMessage(long chatId, String textToSend){

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        message.setReplyMarkup(replyKeyboardMarkup());
        try {
            execute(message);
        }
         catch (TelegramApiException e) {
             log.error("Error occurred " + e.getMessage());

         }

    }

    ReplyKeyboardMarkup replyKeyboardMarkup (){
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add("weather");
        row.add("random joke");

        keyboardRows.add(row);
        KeyboardRow row2 = new KeyboardRow();
        row2.add("register");
        row2.add("check my data");
        row2.add("delete my data ");
        keyboardRows.add(row2);

        KeyboardRow row3 = new KeyboardRow();
        row3.add("row3");
        row3.add("row3-1");
        keyboardRows.add(row3);
        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }
}
