package com.example.mate_telegram_bot;

import com.example.mate_telegram_bot.dto.VacancyDto;
import com.example.mate_telegram_bot.service.VacancyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@PropertySource("classpath:telegram.properties")
public class VacanciesBot extends TelegramLongPollingBot {
    @Autowired
    private VacancyService vacancyService;

    @Value("${bot.token}")
    private String botToken;

    @Value("${bot.name}")
    private String botName;

    private final Map<Long, String> lastShownVacancyLevel = new HashMap<>(); //as base for last user doing in vacancies hierarchy


    public VacanciesBot() {
        super();
    }

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override //main logic for message (events) from TelegramBot
    public void onUpdateReceived(Update update) {
        try { //added check on Runtime Exception
            //killed NullPointerException
            if (update.getMessage() != null) {
                handleStartCommand(update); //call method
            }
            if (update.getCallbackQuery() != null) { //if not null
                String callbackData = update.getCallbackQuery().getData(); //get data after push on button "Junior"

                if ("showJuniorVacancies".equals(callbackData)) { //check - user push on button "Junior"?
                    showJuniorVacancies(update); //call method with logic for button "Junior"
                } else if ("showMiddleVacancies".equals(callbackData)) { //check - user push on button "Middle"?
                    showMiddleVacancies(update); //call method with logic for button "Middle"
                } else if ("showSeniorVacancies".equals(callbackData)) { //check - user push on button "Senior"?
                    showSeniorVacancies(update); //call method with logic for button "Senior"
                } else if (callbackData.startsWith("vacancyId=")) { //for showing the value of button with vacancy
                    String id = callbackData.split("=")[1]; //take second element from divided callbackData
                    showVacancyDescription(id, update);
                } else if ("backToVacancies".equals(callbackData)) {
                    handleBackToVacanciesCommand(update);
                } else if ("backToStartMenu".equals(callbackData)) {
                    handleBackToStartCommand(update);
                }
            }
        } catch (Exception e) { //throw exceptions
            throw new RuntimeException("Can't send message to user!", e);
        }
    }

    private void handleBackToVacanciesCommand(Update update) throws TelegramApiException {
        Long chatId = update.getCallbackQuery().getMessage().getChatId(); //get user id
        String level = lastShownVacancyLevel.get(chatId); //get level of hierarchy vacancies by user chat id

        if ("junior".equals((level))) { //if user on "junior" level
            showJuniorVacancies(update);
        } else if ("middle".equals(level)) { //if user on "middle" level
            showMiddleVacancies(update);
        } else if ("senior".equals(level)) { //if user on "senior" level
            showSeniorVacancies(update);
        }
    }

    private void handleBackToStartCommand(Update update) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText("Choose title:");
        sendMessage.setChatId(update.getCallbackQuery().getMessage().getChatId());
        sendMessage.setReplyMarkup(getStartMenu());

        execute(sendMessage);
    }

    private void showVacancyDescription(String id, Update update) throws TelegramApiException {
        VacancyDto vacancyDto = vacancyService.get(id); //get id for object
        SendMessage sendMessage = new SendMessage(); //create object for message to users on telegram
        sendMessage.setChatId(update.getCallbackQuery().getMessage().getChatId()); //get user(which push button) id
        String vacancyInfo = """
                *Title:* %s
                *Company:* %s
                *Short Description:* %s
                *Description:* %s
                *Salary:* %s
                *Link:* [%s](%s)
                """.formatted(
                    escapeMarkdownReservedChars(vacancyDto.getTitle()),
                    escapeMarkdownReservedChars(vacancyDto.getCompany()),
                    escapeMarkdownReservedChars(vacancyDto.getShortDescription()),
                    escapeMarkdownReservedChars(vacancyDto.getLongDescription()),
                    vacancyDto.getSalary().isBlank() ? "Not specified" : escapeMarkdownReservedChars(vacancyDto.getSalary()),
                    "Click here for more details",
                    escapeMarkdownReservedChars(vacancyDto.getLink())
        );
        sendMessage.setText(vacancyInfo); //text for message
        sendMessage.setParseMode(ParseMode.MARKDOWNV2);
        sendMessage.setReplyMarkup(getBackToVacanciesMenu());

        execute(sendMessage); //to do
    }

    private String escapeMarkdownReservedChars(String text) {
        return text
                .replace("-", "\\-")
                .replace("_", "\\_")
                .replace("*", "\\*")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("~", "\\~")
                .replace("`", "\\`")
                .replace(">", "\\>")
                .replace("#", "\\#")
                .replace("+", "\\+")
                .replace(".", "\\.")
                .replace("!", "\\!");
    }

    private ReplyKeyboard getBackToVacanciesMenu() { //menu to back to title menu
        List<InlineKeyboardButton> row = new ArrayList<>(); //list for back-button

        InlineKeyboardButton backToVacanciesButton = new InlineKeyboardButton();
        backToVacanciesButton.setText("Back to vacancies");
        backToVacanciesButton.setCallbackData("backToVacancies");
        row.add(backToVacanciesButton);

        InlineKeyboardButton backToStartMenuButton = new InlineKeyboardButton();
        backToStartMenuButton.setText("Back to start menu");
        backToStartMenuButton.setCallbackData("backToStartMenu");
        row.add(backToStartMenuButton);

        InlineKeyboardButton getChatGptButton = new InlineKeyboardButton();
        getChatGptButton.setText("Get cover letter");
        getChatGptButton.setUrl("https://chat.openai.com/");
        row.add(getChatGptButton);

        return new InlineKeyboardMarkup(List.of(row)); //return line with back-buttons
    }

    private void showJuniorVacancies(Update update) throws TelegramApiException { //method with logic for button "Junior"
        SendMessage sendMessage = new SendMessage(); //create object for message to users on telegram
        sendMessage.setText("Please choose vacancy:"); //text for message
        Long chatId = update.getCallbackQuery().getMessage().getChatId(); //get user(which push button) id
        sendMessage.setChatId(chatId);
        sendMessage.setReplyMarkup(getJuniorVacanciesMenu()); //menu for junior vacancies

        execute(sendMessage); //to do

        lastShownVacancyLevel.put(chatId, "junior");
    }

    private void showMiddleVacancies(Update update) throws TelegramApiException { //method with logic for button "Middle"
        SendMessage sendMessage = new SendMessage(); //create object for message to users on telegram
        sendMessage.setText("Please choose vacancy:"); //text for message
        Long chatId = update.getCallbackQuery().getMessage().getChatId(); //get user(which push button) id
        sendMessage.setChatId(chatId);
        sendMessage.setReplyMarkup(getMiddleVacanciesMenu()); //menu for middle vacancies

        execute(sendMessage); //to do

        lastShownVacancyLevel.put(chatId, "middle");
    }

    private void showSeniorVacancies(Update update) throws TelegramApiException { //method with logic for button "Senior"
        SendMessage sendMessage = new SendMessage(); //create object for message to users on telegram
        sendMessage.setText("Please choose vacancy:"); //text for message
        Long chatId = update.getCallbackQuery().getMessage().getChatId(); //get user(which push button) id
        sendMessage.setChatId(chatId);
        sendMessage.setReplyMarkup(getSeniorVacanciesMenu()); //menu for senior vacancies

        execute(sendMessage); //to do

        lastShownVacancyLevel.put(chatId, "senior");
    }

    private ReplyKeyboard getJuniorVacanciesMenu() {
        List<VacancyDto> vacancies = vacancyService.getJuniorVacancies(); //list with only junior vacancies

        return getVacanciesMenu(vacancies); //call method for list of junior vacancies
    }

    private ReplyKeyboard getMiddleVacanciesMenu() {
        List<VacancyDto> vacancies = vacancyService.getMiddleVacancies(); //list with only middle vacancies

        return getVacanciesMenu(vacancies); //call method for list of middle vacancies
    }

    private ReplyKeyboard getSeniorVacanciesMenu() {
        List<VacancyDto> vacancies = vacancyService.getSeniorVacancies(); //list with only senior vacancies

        return getVacanciesMenu(vacancies); //call method for list of senior vacancies
    }

    private ReplyKeyboard getVacanciesMenu(List<VacancyDto> vacancies) {
        List<InlineKeyboardButton> row = new ArrayList<>(); //collection for vacancies from level-vacancy button
        for (VacancyDto vacancy: vacancies) { //for each vacancy from vacancies we form new vacancy button
            InlineKeyboardButton vacancyButton = new InlineKeyboardButton();
            vacancyButton.setText(vacancy.getTitle()); //name vacancy
            vacancyButton.setCallbackData("vacancyId=" + vacancy.getId());

            row.add(vacancyButton); //add in list all senior vacancies
        }

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup(); //object for callback
        keyboardMarkup.setKeyboard(List.of(row)); //callback our list of buttons

        return keyboardMarkup; //return our buttons at the bot
    }

    private void handleStartCommand(Update update) {
        String text = update.getMessage().getText(); //get users text-message from bot
        System.out.println("Received text is: " + text);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(update.getMessage().getChatId()); //get id of chat from bot
        sendMessage.setText("Welcome to vacancies bot! Please choose your title:"); //create greet fot users
        sendMessage.setReplyMarkup(getStartMenu()); //call the method for start menu

        try {
            execute(sendMessage); //send message to bot
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private ReplyKeyboard getStartMenu() {
        List<InlineKeyboardButton> row = new ArrayList<>(); //collection for our buttons
        InlineKeyboardButton junior = new InlineKeyboardButton(); //create button for junior vacancies
        junior.setText("Junior"); //title for button junior
        junior.setCallbackData("showJuniorVacancies"); //signal from push on the button (init callback data from bot)
        row.add(junior); //inject button to our list of buttons

        InlineKeyboardButton middle = new InlineKeyboardButton(); //create button for middle vacancies
        middle.setText("Middle"); //title for button middle
        middle.setCallbackData("showMiddleVacancies"); //signal from push on the button (init callback data from bot)
        row.add(middle); //inject button to our list of buttons

        InlineKeyboardButton senior = new InlineKeyboardButton(); //create button for senior vacancies
        senior.setText("Senior"); //title for button senior
        senior.setCallbackData("showSeniorVacancies"); //signal from push on the button (init callback data from bot)
        row.add(senior); //inject button to our list of buttons

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup(); //object for callback
        keyboardMarkup.setKeyboard(List.of(row)); //callback our list of buttons

        return keyboardMarkup; //return our buttons at the bot
    }

}
