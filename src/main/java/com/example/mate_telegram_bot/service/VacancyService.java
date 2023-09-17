package com.example.mate_telegram_bot.service;

import com.example.mate_telegram_bot.dto.VacancyDto;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VacancyService {
    @Autowired
    private VacanciesReaderService vacanciesReaderService;
    private final Map<String, VacancyDto> vacancies = new HashMap<>(); //our base with vacancies by String id

    @PostConstruct //configuring components(beans) after they are created
    public void init() { //for initialization new objects(beans)
        List<VacancyDto> list = vacanciesReaderService.getVacanciesFromFile("vacancies.csv");
        for (VacancyDto vacancy: list) {
            vacancies.put(vacancy.getId(), vacancy);
        }
    }

    public List<VacancyDto> getJuniorVacancies() { //method for sort vacancies by word "junior"
        return vacancies.values().stream() //we use stream API to filter by role "junior"
                .filter(v -> v.getTitle().toLowerCase().contains("junior"))
                .toList();
    }

    public List<VacancyDto> getMiddleVacancies() { //method for sort vacancies by word "middle"
        return vacancies.values().stream() //we use stream API to filter by role "middle"
                .filter(v -> v.getTitle().toLowerCase().contains("middle"))
                .toList();
    }

    public List<VacancyDto> getSeniorVacancies() { //method for sort vacancies by word "senior"
        return vacancies.values().stream() //we use stream API to filter by role "senior"
                .filter(v -> v.getTitle().toLowerCase().contains("senior"))
                .toList();

        //or another variant
//        return vacancies.values().stream()
//                .filter(v -> !v.getTitle().toLowerCase().contains("junior"))
//                .filter(v -> !v.getTitle().toLowerCase().contains("middle"))
//                .toList();
    }

    public VacancyDto get(String id) { //method for return id vacancy
        return vacancies.get(id);
    }
}
