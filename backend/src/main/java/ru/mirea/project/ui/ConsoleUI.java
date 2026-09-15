package ru.mirea.project.ui;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.DataAccessException;
import ru.mirea.project.exception.EntityNotFoundException;
import ru.mirea.project.model.ParkingRequest;
import ru.mirea.project.model.RequestStatus;
import ru.mirea.project.service.ParkingRequestService;
import ru.mirea.project.service.UserService;

public class ConsoleUI {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final String DATE_TIME_HINT = "дд.мм.гггг чч:мм";

    private final Scanner scanner = new Scanner(System.in);
    private final UserService userService;
    private final ParkingRequestService parkingRequestService;

    public ConsoleUI(UserService userService, ParkingRequestService parkingRequestService) {
        this.userService = userService;
        this.parkingRequestService = parkingRequestService;
    }

    public void run() {
        while (true) {
            System.out.println();
            System.out.println("========== ПАРКОВОЧНАЯ СИСТЕМА ==========");
            System.out.println("1. Владельцы автомобилей");
            System.out.println("2. Заявки на парковку");
            System.out.println("3. Поиск");
            System.out.println("4. Фильтрация");
            System.out.println("5. Статистика");
            System.out.println("6. Экспорт данных");
            System.out.println("7. Вывести таблицы базы данных");
            System.out.println("0. Выход");
            int choice = readInt("Выберите действие: ");
            switch (choice) {
                case 1, 3, 4, 5, 6, 7 -> System.out.println("Пока не реализовано");
                case 2 -> parkingRequestsMenu();
                case 0 -> {
                    System.out.println("До свидания!");
                    return;
                }
                default -> System.out.println("Неизвестный пункт меню");
            }
        }
    }

    private void parkingRequestsMenu() {
        while (true) {
            System.out.println();
            System.out.println("---- Заявки на парковку ----");
            System.out.println("1. Показать все");
            System.out.println("2. Создать");
            System.out.println("3. Найти по ID");
            System.out.println("4. Изменить");
            System.out.println("5. Сменить статус");
            System.out.println("6. Удалить");
            System.out.println("0. Назад");
            int choice = readInt("Выберите действие: ");
            switch (choice) {
                case 1 -> showAllParkingRequests();
                case 2 -> createParkingRequest();
                case 3 -> findParkingRequestById();
                case 4 -> updateParkingRequest();
                case 5 -> changeParkingRequestStatus();
                case 6 -> deleteParkingRequest();
                case 0 -> {
                    return;
                }
                default -> System.out.println("Неизвестный пункт меню");
            }
        }
    }

    private void showAllParkingRequests() {
        try {
            List<ParkingRequest> requests = parkingRequestService.getAll();
            if (requests.isEmpty()) {
                System.out.println("Заявок пока нет");
                return;
            }
            requests.forEach(System.out::println);
        } catch (DataAccessException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void createParkingRequest() {
        try {
            long userId = readLong("ID владельца: ");
            String licensePlate = readLine("Гос. номер: ");
            int spotNumber = readInt("Номер места: ");
            LocalDateTime startTime = readDateTime("Начало (" + DATE_TIME_HINT + "): ");
            LocalDateTime endTime = readDateTime("Окончание (" + DATE_TIME_HINT + "): ");

            ParkingRequest created = parkingRequestService.create(userId, licensePlate, spotNumber, startTime, endTime);
            System.out.println("Заявка создана: " + created);
        } catch (BusinessException | EntityNotFoundException | DataAccessException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void findParkingRequestById() {
        try {
            long id = readLong("ID заявки: ");
            System.out.println(parkingRequestService.getById(id));
        } catch (EntityNotFoundException | DataAccessException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void updateParkingRequest() {
        try {
            long id = readLong("ID заявки: ");
            String licensePlate = readLine("Новый гос. номер: ");
            int spotNumber = readInt("Новый номер места: ");
            LocalDateTime startTime = readDateTime("Новое начало (" + DATE_TIME_HINT + "): ");
            LocalDateTime endTime = readDateTime("Новое окончание (" + DATE_TIME_HINT + "): ");

            ParkingRequest updated = parkingRequestService.update(id, licensePlate, spotNumber, startTime, endTime);
            System.out.println("Заявка обновлена: " + updated);
        } catch (BusinessException | EntityNotFoundException | DataAccessException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void changeParkingRequestStatus() {
        try {
            long id = readLong("ID заявки: ");
            RequestStatus newStatus = readStatus("Новый статус " + Arrays.toString(RequestStatus.values()) + ": ");

            ParkingRequest updated = parkingRequestService.changeStatus(id, newStatus);
            System.out.println("Статус обновлён: " + updated);
        } catch (BusinessException | EntityNotFoundException | DataAccessException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void deleteParkingRequest() {
        try {
            long id = readLong("ID заявки: ");
            parkingRequestService.delete(id);
            System.out.println("Заявка удалена");
        } catch (EntityNotFoundException | DataAccessException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }

    private int readInt(String prompt) {
        while (true) {
            try {
                return Integer.parseInt(readLine(prompt).trim());
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: введите целое число");
            }
        }
    }

    private long readLong(String prompt) {
        while (true) {
            try {
                return Long.parseLong(readLine(prompt).trim());
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: введите целое число");
            }
        }
    }

    private LocalDateTime readDateTime(String prompt) {
        while (true) {
            try {
                return LocalDateTime.parse(readLine(prompt).trim(), DATE_TIME_FORMATTER);
            } catch (DateTimeParseException e) {
                System.out.println("Ошибка: некорректный формат даты/времени, ожидается " + DATE_TIME_HINT);
            }
        }
    }

    private RequestStatus readStatus(String prompt) {
        while (true) {
            try {
                return RequestStatus.valueOf(readLine(prompt).trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                System.out.println("Ошибка: неизвестный статус");
            }
        }
    }
}
