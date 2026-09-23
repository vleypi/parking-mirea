package ru.mirea.project.ui;

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.DataAccessException;
import ru.mirea.project.exception.EntityNotFoundException;
import ru.mirea.project.model.ParkingRequest;
import ru.mirea.project.model.RequestStatus;
import ru.mirea.project.model.User;
import ru.mirea.project.service.ParkingRequestService;
import ru.mirea.project.service.Statistics;
import ru.mirea.project.service.UserService;
import ru.mirea.project.util.ExcelExporter;

public class ConsoleUI {
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("dd.MM.uuuu HH:mm").withResolverStyle(ResolverStyle.STRICT);
    private static final String DATE_TIME_HINT = "дд.мм.гггг чч:мм";
    private static final String EXPORT_FILE_NAME = "parking_export.xlsx";

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
                case 5 -> showStatistics();
                case 6 -> exportData();
                case 7 -> showTables();
                case 1 -> usersMenu();
                case 3 -> searchMenu();
                case 4 -> filterMenu();
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

    private void usersMenu() {
        while (true) {
            System.out.println();
            System.out.println("---- Владельцы автомобилей ----");
            System.out.println("1. Показать всех");
            System.out.println("2. Создать");
            System.out.println("3. Найти по ID");
            System.out.println("4. Изменить");
            System.out.println("5. Удалить");
            System.out.println("0. Назад");
            int choice = readInt("Выберите действие: ");
            switch (choice) {
                case 1 -> showAllUsers();
                case 2 -> createUser();
                case 3 -> findUserById();
                case 4 -> updateUser();
                case 5 -> deleteUser();
                case 0 -> {
                    return;
                }
                default -> System.out.println("Неизвестный пункт меню");
            }
        }
    }

    private void showAllUsers() {
        try {
            List<User> users = userService.getAll();
            if (users.isEmpty()) {
                System.out.println("Владельцев пока нет");
                return;
            }
            users.forEach(System.out::println);
        } catch (DataAccessException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void createUser() {
        try {
            String name = readLine("Имя: ");
            String phone = readLine("Телефон: ");

            User created = userService.create(name, phone);
            System.out.println("Владелец создан: " + created);
        } catch (BusinessException | DataAccessException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void findUserById() {
        try {
            long id = readLong("ID владельца: ");
            System.out.println(userService.getById(id));
        } catch (EntityNotFoundException | DataAccessException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void updateUser() {
        try {
            long id = readLong("ID владельца: ");
            String name = readLine("Новое имя: ");
            String phone = readLine("Новый телефон: ");

            User updated = userService.update(id, name, phone);
            System.out.println("Владелец обновлён: " + updated);
        } catch (BusinessException | EntityNotFoundException | DataAccessException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void deleteUser() {
        try {
            long id = readLong("ID владельца: ");
            userService.delete(id);
            System.out.println("Владелец удалён");
        } catch (EntityNotFoundException | DataAccessException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    
    private void searchMenu() {
    while (true) {
        System.out.println();
        System.out.println("---- Поиск ----");
        System.out.println("1. По гос. номеру");
        System.out.println("2. По имени владельца");
        System.out.println("0. Назад");
        int choice = readInt("Выберите действие: ");
        switch (choice) {
            case 1 -> searchByLicensePlate();
            case 2 -> searchByOwnerName();
            case 0 -> {
                return;
            }
            default -> System.out.println("Неизвестный пункт меню");
        }
    }
}

    private void searchByLicensePlate() {
        try {
            String fragment = readLine("Фрагмент гос. номера: ");
            printResults(parkingRequestService.searchByLicensePlate(fragment));
        } catch (DataAccessException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void searchByOwnerName() {
        try {
            String fragment = readLine("Фрагмент имени владельца: ");
            Map<User, List<ParkingRequest>> found = parkingRequestService.searchByOwnerName(fragment);
            if (found.isEmpty()) {
                System.out.println("Ничего не найдено");
                return;
            }
            found.forEach((user, requests) -> {
                System.out.println(user);
                if (requests.isEmpty()) {
                    System.out.println("  Заявок нет");
                } else {
                    requests.forEach(request -> System.out.println("  " + request));
                }
            });
        } catch (DataAccessException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void filterMenu() {
        while (true) {
            System.out.println();
            System.out.println("---- Фильтрация и сортировка ----");
            System.out.println("1. По статусу");
            System.out.println("2. По диапазону дат");
            System.out.println("3. Сортировка по времени начала");
            System.out.println("4. Сортировка по дате создания");
            System.out.println("0. Назад");
            int choice = readInt("Выберите действие: ");
            switch (choice) {
                case 1 -> filterByStatus();
                case 2 -> filterByDateRange();
                case 3 -> sortByStartTime();
                case 4 -> sortByCreatedAt();
                case 0 -> {
                    return;
                }
                default -> System.out.println("Неизвестный пункт меню");
            }
        }
    }

    private void filterByStatus() {
        try {
            RequestStatus status = readStatus("Статус " + Arrays.toString(RequestStatus.values()) + ": ");
            printResults(parkingRequestService.filterByStatus(status));
        } catch (DataAccessException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void filterByDateRange() {
        try {
            LocalDateTime from = readDateTime("Начало диапазона (" + DATE_TIME_HINT + "): ");
            LocalDateTime to = readDateTime("Конец диапазона (" + DATE_TIME_HINT + "): ");
            printResults(parkingRequestService.filterByDateRange(from, to));
        } catch (BusinessException | DataAccessException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void sortByStartTime() {
        try {
            printResults(parkingRequestService.sortByStartTime(readAscending()));
        } catch (DataAccessException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void sortByCreatedAt() {
        try {
            printResults(parkingRequestService.sortByCreatedAt(readAscending()));
        } catch (DataAccessException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private boolean readAscending() {
        while (true) {
            int choice = readInt("Порядок (1 - по возрастанию, 2 - по убыванию): ");
            if (choice == 1) {
                return true;
            }
            if (choice == 2) {
                return false;
            }
            System.out.println("Ошибка: введите 1 или 2");
        }
    }

    private void printResults(List<ParkingRequest> requests) {
        if (requests.isEmpty()) {
            System.out.println("Ничего не найдено");
            return;
        }
        requests.forEach(System.out::println);
    }

    private void showStatistics() {
        try {
            Statistics statistics = parkingRequestService.getStatistics();
            System.out.println();
            System.out.println("---- Статистика ----");
            System.out.println("Всего владельцев: " + statistics.totalUsers());
            System.out.println("Всего заявок: " + statistics.totalRequests());
            System.out.println("Активных (NEW + CONFIRMED): " + statistics.active());
            System.out.println("Завершённых: " + statistics.completed());
            System.out.println("Отменённых: " + statistics.cancelled());
            System.out.println("Занятых мест сейчас: " + statistics.occupiedNow());
        } catch (DataAccessException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void exportData() {
        try {
            Path file = Path.of(EXPORT_FILE_NAME).toAbsolutePath();
            ExcelExporter.export(userService.getAll(), parkingRequestService.getAll(), file);
            System.out.println("Экспорт выполнен: " + file);
        } catch (DataAccessException | UncheckedIOException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void showTables() {
        try {
            List<User> users = userService.getAll();
            List<ParkingRequest> requests = parkingRequestService.getAll();

            System.out.println();
            System.out.println("---- Таблица users ----");
            printTable(new String[] {"ID", "Имя", "Телефон", "Создан"},
                users.stream()
                    .map(u -> new String[] {
                        String.valueOf(u.getId()), u.getName(), u.getPhone(),
                        DATE_TIME_FORMATTER.format(u.getCreatedAt())})
                    .toList());

            System.out.println();
            System.out.println("---- Таблица parking_requests ----");
            printTable(new String[] {"ID", "Владелец", "Гос. номер", "Место", "Начало", "Окончание", "Статус", "Создана"},
                requests.stream()
                    .map(r -> new String[] {
                        String.valueOf(r.getId()), String.valueOf(r.getUserId()), r.getLicensePlate(),
                        String.valueOf(r.getSpotNumber()), DATE_TIME_FORMATTER.format(r.getStartTime()),
                        DATE_TIME_FORMATTER.format(r.getEndTime()), r.getStatus().name(),
                        DATE_TIME_FORMATTER.format(r.getCreatedAt())})
                    .toList());
        } catch (DataAccessException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void printTable(String[] headers, List<String[]> rows) {
        int[] widths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            widths[i] = headers[i].length();
        }
        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                widths[i] = Math.max(widths[i], row[i].length());
            }
        }

        printTableRow(headers, widths);
        int totalWidth = Arrays.stream(widths).sum() + 3 * (headers.length - 1);
        System.out.println("-".repeat(totalWidth));
        if (rows.isEmpty()) {
            System.out.println("(записей нет)");
            return;
        }
        rows.forEach(row -> printTableRow(row, widths));
    }

    private void printTableRow(String[] cells, int[] widths) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            line.append(String.format("%-" + widths[i] + "s", cells[i]));
            if (i < cells.length - 1) {
                line.append(" | ");
            }
        }
        System.out.println(line);
    }

    private String readLine(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            System.out.println();
            System.out.println("Ввод завершён. До свидания!");
            System.exit(0);
        }
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
