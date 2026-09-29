package ru.mirea.project.ui;

import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Function;
import java.util.stream.Collectors;

import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.DataAccessException;
import ru.mirea.project.exception.EntityNotFoundException;
import ru.mirea.project.model.ParkingRequest;
import ru.mirea.project.model.ParkingSpot;
import ru.mirea.project.model.RequestStatus;
import ru.mirea.project.model.SpotType;
import ru.mirea.project.model.User;
import ru.mirea.project.model.Vehicle;
import ru.mirea.project.service.ParkingRequestService;
import ru.mirea.project.service.ParkingSpotService;
import ru.mirea.project.service.Statistics;
import ru.mirea.project.service.UserService;
import ru.mirea.project.service.VehicleService;
import ru.mirea.project.util.ExcelExporter;

public class ConsoleUI {
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("dd.MM.uuuu HH:mm").withResolverStyle(ResolverStyle.STRICT);
    private static final String DATE_TIME_HINT = "дд.мм.гггг чч:мм";
    private static final String EXPORT_FILE_NAME = "parking_export.xlsx";

    private final Scanner scanner = new Scanner(System.in);
    private final UserService userService;
    private final VehicleService vehicleService;
    private final ParkingSpotService parkingSpotService;
    private final ParkingRequestService parkingRequestService;

    public ConsoleUI(UserService userService, VehicleService vehicleService,
                     ParkingSpotService parkingSpotService, ParkingRequestService parkingRequestService) {
        this.userService = userService;
        this.vehicleService = vehicleService;
        this.parkingSpotService = parkingSpotService;
        this.parkingRequestService = parkingRequestService;
    }

    public void run() {
        while (true) {
            System.out.println();
            System.out.println("ПАРКОВОЧНАЯ СИСТЕМА");
            System.out.println("1. Владельцы автомобилей");
            System.out.println("2. Автомобили");
            System.out.println("3. Парковочные места");
            System.out.println("4. Заявки на парковку");
            System.out.println("5. Поиск");
            System.out.println("6. Фильтрация");
            System.out.println("7. Статистика");
            System.out.println("8. Экспорт данных");
            System.out.println("9. Вывести таблицы базы данных");
            System.out.println("0. Выход");
            int choice = readInt("Выберите действие: ");
            switch (choice) {
                case 7 -> showStatistics();
                case 8 -> exportData();
                case 9 -> showTables();
                case 1 -> usersMenu();
                case 2 -> vehiclesMenu();
                case 3 -> parkingSpotsMenu();
                case 5 -> searchMenu();
                case 6 -> filterMenu();
                case 4 -> parkingRequestsMenu();
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
            System.out.println("Заявки на парковку");
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
            describeRequests(requests).forEach(System.out::println);
        } catch (DataAccessException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void createParkingRequest() {
        try {
            long userId = readLong("ID владельца: ");
            userService.getById(userId);
            List<Vehicle> vehicles = vehicleService.getByUserId(userId);
            if (vehicles.isEmpty()) {
                System.out.println("У владельца нет автомобилей. Сначала добавьте автомобиль в разделе «Автомобили»");
                return;
            }
            System.out.println("Автомобили владельца:");
            vehicles.forEach(System.out::println);
            long vehicleId = readLong("ID автомобиля: ");
            System.out.println("Парковочные места:");
            parkingSpotService.getAll().forEach(System.out::println);
            long spotId = readLong("ID места: ");
            LocalDateTime startTime = readDateTime("Начало (" + DATE_TIME_HINT + "): ");
            LocalDateTime endTime = readEndTime("Окончание (" + DATE_TIME_HINT + "): ", startTime);

            ParkingRequest created = parkingRequestService.create(userId, vehicleId, spotId, startTime, endTime);
            System.out.println("Заявка создана: " + describeRequest(created));
        } catch (BusinessException | EntityNotFoundException | DataAccessException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void findParkingRequestById() {
        try {
            long id = readLong("ID заявки: ");
            System.out.println(describeRequest(parkingRequestService.getById(id)));
        } catch (EntityNotFoundException | DataAccessException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void updateParkingRequest() {
        try {
            long id = readLong("ID заявки: ");
            ParkingRequest existing = parkingRequestService.getById(id);
            System.out.println("Автомобили владельца:");
            vehicleService.getByUserId(existing.getUserId()).forEach(System.out::println);
            long vehicleId = readLong("ID нового автомобиля: ");
            System.out.println("Парковочные места:");
            parkingSpotService.getAll().forEach(System.out::println);
            long spotId = readLong("ID нового места: ");
            LocalDateTime startTime = readDateTime("Новое начало (" + DATE_TIME_HINT + "): ");
            LocalDateTime endTime = readEndTime("Новое окончание (" + DATE_TIME_HINT + "): ", startTime);

            ParkingRequest updated = parkingRequestService.update(id, vehicleId, spotId, startTime, endTime);
            System.out.println("Заявка обновлена: " + describeRequest(updated));
        } catch (BusinessException | EntityNotFoundException | DataAccessException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void changeParkingRequestStatus() {
        try {
            long id = readLong("ID заявки: ");
            RequestStatus newStatus = readStatus("Новый статус " + Arrays.toString(RequestStatus.values()) + ": ");

            ParkingRequest updated = parkingRequestService.changeStatus(id, newStatus);
            System.out.println("Статус обновлён: " + describeRequest(updated));
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
            System.out.println("Владельцы автомобилей");
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
            String name = readValid("Имя: ", userService::checkName);
            String phone = readValid("Телефон (например 89991234567): ", input -> userService.checkPhone(0, input));

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
            userService.getById(id);
            String name = readValid("Новое имя: ", userService::checkName);
            String phone = readValid("Новый телефон (например 89991234567): ", input -> userService.checkPhone(id, input));

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

    private void vehiclesMenu() {
        while (true) {
            System.out.println();
            System.out.println("Автомобили");
            System.out.println("1. Показать все");
            System.out.println("2. Создать");
            System.out.println("3. Найти по ID");
            System.out.println("4. Изменить");
            System.out.println("5. Удалить");
            System.out.println("6. Автомобили владельца");
            System.out.println("0. Назад");
            int choice = readInt("Выберите действие: ");
            switch (choice) {
                case 1 -> showAllVehicles();
                case 2 -> createVehicle();
                case 3 -> findVehicleById();
                case 4 -> updateVehicle();
                case 5 -> deleteVehicle();
                case 6 -> showVehiclesByOwner();
                case 0 -> {
                    return;
                }
                default -> System.out.println("Неизвестный пункт меню");
            }
        }
    }

    private void showAllVehicles() {
        try {
            List<Vehicle> vehicles = vehicleService.getAll();
            if (vehicles.isEmpty()) {
                System.out.println("Автомобилей пока нет");
                return;
            }
            vehicles.forEach(System.out::println);
        } catch (DataAccessException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void createVehicle() {
        try {
            long userId = readLong("ID владельца: ");
            userService.getById(userId);
            String plate = readValid("Гос. номер (например А123ВС777): ", input -> vehicleService.checkLicensePlate(0, input));
            String brand = readValid("Марка: ", vehicleService::checkBrand);
            String model = readValid("Модель: ", vehicleService::checkModel);

            Vehicle created = vehicleService.create(userId, plate, brand, model);
            System.out.println("Автомобиль создан: " + created);
        } catch (BusinessException | EntityNotFoundException | DataAccessException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void findVehicleById() {
        try {
            long id = readLong("ID автомобиля: ");
            System.out.println(vehicleService.getById(id));
        } catch (EntityNotFoundException | DataAccessException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void updateVehicle() {
        try {
            long id = readLong("ID автомобиля: ");
            vehicleService.getById(id);
            String plate = readValid("Новый гос. номер (например А123ВС777): ", input -> vehicleService.checkLicensePlate(id, input));
            String brand = readValid("Новая марка: ", vehicleService::checkBrand);
            String model = readValid("Новая модель: ", vehicleService::checkModel);

            Vehicle updated = vehicleService.update(id, plate, brand, model);
            System.out.println("Автомобиль обновлён: " + updated);
        } catch (BusinessException | EntityNotFoundException | DataAccessException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void deleteVehicle() {
        try {
            long id = readLong("ID автомобиля: ");
            vehicleService.delete(id);
            System.out.println("Автомобиль удалён");
        } catch (EntityNotFoundException | DataAccessException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void showVehiclesByOwner() {
        try {
            long userId = readLong("ID владельца: ");
            User owner = userService.getById(userId);
            List<Vehicle> vehicles = vehicleService.getByUserId(userId);
            if (vehicles.isEmpty()) {
                System.out.println("У владельца " + owner.getName() + " нет автомобилей");
                return;
            }
            vehicles.forEach(System.out::println);
        } catch (EntityNotFoundException | DataAccessException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void parkingSpotsMenu() {
        while (true) {
            System.out.println();
            System.out.println("Парковочные места");
            System.out.println("1. Показать все");
            System.out.println("2. Создать");
            System.out.println("3. Найти по ID");
            System.out.println("4. Изменить");
            System.out.println("5. Удалить");
            System.out.println("6. Места по типу");
            System.out.println("0. Назад");
            int choice = readInt("Выберите действие: ");
            switch (choice) {
                case 1 -> showAllParkingSpots();
                case 2 -> createParkingSpot();
                case 3 -> findParkingSpotById();
                case 4 -> updateParkingSpot();
                case 5 -> deleteParkingSpot();
                case 6 -> showParkingSpotsByType();
                case 0 -> {
                    return;
                }
                default -> System.out.println("Неизвестный пункт меню");
            }
        }
    }

    private void showAllParkingSpots() {
        try {
            List<ParkingSpot> spots = parkingSpotService.getAll();
            if (spots.isEmpty()) {
                System.out.println("Парковочных мест пока нет");
                return;
            }
            spots.forEach(System.out::println);
        } catch (DataAccessException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void createParkingSpot() {
        try {
            int spotNumber = readValid("Номер места: ",
                input -> parkingSpotService.checkSpotNumberUnique(0, parkingSpotService.parseSpotNumber(input)));
            SpotType spotType = readValid("Тип места (STANDARD, DISABLED, ELECTRIC): ", parkingSpotService::parseSpotType);
            BigDecimal rate = readValid("Тариф, руб/час: ", parkingSpotService::parseHourlyRate);

            ParkingSpot created = parkingSpotService.create(spotNumber, spotType, rate);
            System.out.println("Место создано: " + created);
        } catch (BusinessException | DataAccessException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void findParkingSpotById() {
        try {
            long id = readLong("ID места: ");
            System.out.println(parkingSpotService.getById(id));
        } catch (EntityNotFoundException | DataAccessException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void updateParkingSpot() {
        try {
            long id = readLong("ID места: ");
            parkingSpotService.getById(id);
            int spotNumber = readValid("Новый номер места: ",
                input -> parkingSpotService.checkSpotNumberUnique(id, parkingSpotService.parseSpotNumber(input)));
            SpotType spotType = readValid("Новый тип места (STANDARD, DISABLED, ELECTRIC): ", parkingSpotService::parseSpotType);
            BigDecimal rate = readValid("Новый тариф, руб/час: ", parkingSpotService::parseHourlyRate);

            ParkingSpot updated = parkingSpotService.update(id, spotNumber, spotType, rate);
            System.out.println("Место обновлено: " + updated);
        } catch (BusinessException | EntityNotFoundException | DataAccessException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void deleteParkingSpot() {
        try {
            long id = readLong("ID места: ");
            parkingSpotService.delete(id);
            System.out.println("Место удалено");
        } catch (EntityNotFoundException | DataAccessException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void showParkingSpotsByType() {
        try {
            SpotType spotType = readValid("Тип места (STANDARD, DISABLED, ELECTRIC): ", parkingSpotService::parseSpotType);
            List<ParkingSpot> spots = parkingSpotService.filterByType(spotType);
            if (spots.isEmpty()) {
                System.out.println("Ничего не найдено");
                return;
            }
            spots.forEach(System.out::println);
        } catch (DataAccessException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void searchMenu() {
    while (true) {
        System.out.println();
        System.out.println("Поиск");
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
                    describeRequests(requests).forEach(line -> System.out.println("  " + line));
                }
            });
        } catch (DataAccessException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void filterMenu() {
        while (true) {
            System.out.println();
            System.out.println("Фильтрация и сортировка");
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
        describeRequests(requests).forEach(System.out::println);
    }

    private List<String> describeRequests(List<ParkingRequest> requests) {
        Map<Long, String> plates = vehicleService.getAll().stream()
            .collect(Collectors.toMap(Vehicle::getId, Vehicle::getLicensePlate));
        Map<Long, Integer> spotNumbers = parkingSpotService.getAll().stream()
            .collect(Collectors.toMap(ParkingSpot::getId, ParkingSpot::getSpotNumber));

        return requests.stream()
            .map(r -> "[%d] место %s | %s | %s - %s | статус: %s | владелец: %d".formatted(
                r.getId(), spotNumbers.get(r.getSpotId()), plates.get(r.getVehicleId()),
                DATE_TIME_FORMATTER.format(r.getStartTime()), DATE_TIME_FORMATTER.format(r.getEndTime()),
                r.getStatus(), r.getUserId()))
            .toList();
    }

    private String describeRequest(ParkingRequest request) {
        return describeRequests(List.of(request)).get(0);
    }

    private void showStatistics() {
        try {
            Statistics statistics = parkingRequestService.getStatistics();
            System.out.println();
            System.out.println("Статистика");
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
            System.out.println("Таблица users");
            printTable(new String[] {"ID", "Имя", "Телефон", "Создан"},
                users.stream()
                    .map(u -> new String[] {
                        String.valueOf(u.getId()), u.getName(), u.getPhone(),
                        DATE_TIME_FORMATTER.format(u.getCreatedAt())})
                    .toList());

            System.out.println();
            System.out.println("Таблица parking_requests");
            printTable(new String[] {"ID", "Владелец", "Автомобиль", "Место", "Начало", "Окончание", "Статус", "Создана"},
                requests.stream()
                    .map(r -> new String[] {
                        String.valueOf(r.getId()), String.valueOf(r.getUserId()), String.valueOf(r.getVehicleId()),
                        String.valueOf(r.getSpotId()), DATE_TIME_FORMATTER.format(r.getStartTime()),
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

    private <T> T readValid(String prompt, Function<String, T> check) {
        while (true) {
            try {
                return check.apply(readLine(prompt));
            } catch (BusinessException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
    }

    private LocalDateTime readEndTime(String prompt, LocalDateTime startTime) {
        while (true) {
            LocalDateTime endTime = readDateTime(prompt);
            try {
                parkingRequestService.checkPeriod(startTime, endTime);
                return endTime;
            } catch (BusinessException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
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
