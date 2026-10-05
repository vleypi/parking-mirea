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
import java.util.function.Supplier;
import java.util.stream.Collectors;

import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.DataAccessException;
import ru.mirea.project.exception.EntityNotFoundException;
import ru.mirea.project.model.LookupValue;
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
    private static final String CANCEL_COMMAND = "q";
    private static final String CREATE_HINT = "Введите q в любом поле, чтобы отменить";
    private static final String EDIT_HINT = "Enter: оставить текущее значение, q: отменить изменение";

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
                case 1 -> perform(this::showAllParkingRequests);
                case 2 -> perform(this::createParkingRequest);
                case 3 -> perform(this::findParkingRequestById);
                case 4 -> perform(this::updateParkingRequest);
                case 5 -> perform(this::changeParkingRequestStatus);
                case 6 -> perform(this::deleteParkingRequest);
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
            System.out.println(CREATE_HINT);
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
            LocalDateTime endTime = readEndTime(() -> readDateTime("Окончание (" + DATE_TIME_HINT + "): "), startTime);

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
            parkingRequestService.checkEditable(existing);
            System.out.println(EDIT_HINT);
            System.out.println("Автомобили владельца:");
            vehicleService.getByUserId(existing.getUserId()).forEach(System.out::println);
            long vehicleId = readValidOrKeep("ID автомобиля", existing.getVehicleId(), this::parseLong);
            System.out.println("Парковочные места:");
            parkingSpotService.getAll().forEach(System.out::println);
            long spotId = readValidOrKeep("ID места", existing.getSpotId(), this::parseLong);
            LocalDateTime startTime = readDateTimeOrKeep("Начало (" + DATE_TIME_HINT + ")", existing.getStartTime());
            LocalDateTime endTime = readEndTime(
                () -> readDateTimeOrKeep("Окончание (" + DATE_TIME_HINT + ")", existing.getEndTime()), startTime);

            ParkingRequest updated = parkingRequestService.update(id, vehicleId, spotId, startTime, endTime);
            System.out.println("Заявка обновлена: " + describeRequest(updated));
        } catch (BusinessException | EntityNotFoundException | DataAccessException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void changeParkingRequestStatus() {
        try {
            long id = readLong("ID заявки: ");
            RequestStatus newStatus = readOption("Новый статус", RequestStatus.values());

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
                case 1 -> perform(this::showAllUsers);
                case 2 -> perform(this::createUser);
                case 3 -> perform(this::findUserById);
                case 4 -> perform(this::updateUser);
                case 5 -> perform(this::deleteUser);
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
            System.out.println(CREATE_HINT);
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
            User existing = userService.getById(id);
            System.out.println(EDIT_HINT);
            String name = readValidOrKeep("Имя", existing.getName(), userService::checkName);
            String phone = readValidOrKeep("Телефон", existing.getPhone(), input -> userService.checkPhone(id, input));

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
                case 1 -> perform(this::showAllVehicles);
                case 2 -> perform(this::createVehicle);
                case 3 -> perform(this::findVehicleById);
                case 4 -> perform(this::updateVehicle);
                case 5 -> perform(this::deleteVehicle);
                case 6 -> perform(this::showVehiclesByOwner);
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
            System.out.println(CREATE_HINT);
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
            Vehicle existing = vehicleService.getById(id);
            System.out.println(EDIT_HINT);
            String plate = readValidOrKeep("Гос. номер", existing.getLicensePlate(),
                input -> vehicleService.checkLicensePlate(id, input));
            String brand = readValidOrKeep("Марка", existing.getBrand(), vehicleService::checkBrand);
            String model = readValidOrKeep("Модель", existing.getModel(), vehicleService::checkModel);

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
                case 1 -> perform(this::showAllParkingSpots);
                case 2 -> perform(this::createParkingSpot);
                case 3 -> perform(this::findParkingSpotById);
                case 4 -> perform(this::updateParkingSpot);
                case 5 -> perform(this::deleteParkingSpot);
                case 6 -> perform(this::showParkingSpotsByType);
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
            System.out.println(CREATE_HINT);
            int spotNumber = readValid("Номер места: ",
                input -> parkingSpotService.checkSpotNumberUnique(0, parkingSpotService.parseSpotNumber(input)));
            SpotType spotType = readOption("Тип места", SpotType.values());
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
            ParkingSpot existing = parkingSpotService.getById(id);
            System.out.println(EDIT_HINT);
            int spotNumber = readValidOrKeep("Номер места", existing.getSpotNumber(),
                input -> parkingSpotService.checkSpotNumberUnique(id, parkingSpotService.parseSpotNumber(input)));
            SpotType spotType = readOptionOrKeep("Тип места", existing.getSpotType(), SpotType.values());
            BigDecimal rate = readValidOrKeep("Тариф, руб/час", existing.getHourlyRate(), parkingSpotService::parseHourlyRate);

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
            SpotType spotType = readOption("Тип места", SpotType.values());
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
                case 1 -> perform(this::searchByLicensePlate);
                case 2 -> perform(this::searchByOwnerName);
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
        } catch (BusinessException | DataAccessException e) {
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
        } catch (BusinessException | DataAccessException e) {
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
                case 1 -> perform(this::filterByStatus);
                case 2 -> perform(this::filterByDateRange);
                case 3 -> perform(this::sortByStartTime);
                case 4 -> perform(this::sortByCreatedAt);
                case 0 -> {
                    return;
                }
                default -> System.out.println("Неизвестный пункт меню");
            }
        }
    }

    private void filterByStatus() {
        try {
            RequestStatus status = readOption("Статус", RequestStatus.values());
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
            System.out.println("Всего автомобилей: " + statistics.totalVehicles());
            System.out.println("Всего парковочных мест: " + statistics.totalSpots());
            System.out.println("Всего заявок: " + statistics.totalRequests());
            System.out.println("Активных (NEW + CONFIRMED): " + statistics.active());
            System.out.println("Завершённых: " + statistics.completed());
            System.out.println("Отменённых: " + statistics.cancelled());
            System.out.println("Занятых мест сейчас: " + statistics.occupiedNow());
            System.out.println("Выручка по завершённым заявкам: " + statistics.completedRevenue() + " руб.");
        } catch (DataAccessException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void exportData() {
        try {
            Path file = Path.of(EXPORT_FILE_NAME).toAbsolutePath();
            ExcelExporter.export(userService.getAll(), vehicleService.getAll(),
                parkingSpotService.getAll(), parkingRequestService.getAll(), file);
            System.out.println("Экспорт выполнен: " + file);
        } catch (DataAccessException | UncheckedIOException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void showTables() {
        try {
            List<User> users = userService.getAll();
            List<Vehicle> vehicles = vehicleService.getAll();
            List<ParkingSpot> spots = parkingSpotService.getAll();
            List<ParkingRequest> requests = parkingRequestService.getAll();

            System.out.println();
            System.out.println("Таблица request_statuses");
            printTable(new String[] {"ID", "Код", "Название"},
                Arrays.stream(RequestStatus.values())
                    .map(s -> new String[] {String.valueOf(s.getId()), s.name(), s.getTitle()})
                    .toList());

            System.out.println();
            System.out.println("Таблица spot_types");
            printTable(new String[] {"ID", "Код", "Название"},
                Arrays.stream(SpotType.values())
                    .map(t -> new String[] {String.valueOf(t.getId()), t.name(), t.getTitle()})
                    .toList());

            System.out.println();
            System.out.println("Таблица users");
            printTable(new String[] {"ID", "Имя", "Телефон", "Создан"},
                users.stream()
                    .map(u -> new String[] {
                        String.valueOf(u.getId()), u.getName(), u.getPhone(),
                        DATE_TIME_FORMATTER.format(u.getCreatedAt())})
                    .toList());

            System.out.println();
            System.out.println("Таблица vehicles");
            printTable(new String[] {"ID", "Владелец", "Гос. номер", "Марка", "Модель"},
                vehicles.stream()
                    .map(v -> new String[] {
                        String.valueOf(v.getId()), String.valueOf(v.getUserId()), v.getLicensePlate(),
                        v.getBrand(), v.getModel()})
                    .toList());

            System.out.println();
            System.out.println("Таблица parking_spots");
            printTable(new String[] {"ID", "Номер места", "Тип", "Тариф, руб/ч"},
                spots.stream()
                    .map(s -> new String[] {
                        String.valueOf(s.getId()), String.valueOf(s.getSpotNumber()), s.getSpotType().getTitle(),
                        s.getHourlyRate().toPlainString()})
                    .toList());

            System.out.println();
            System.out.println("Таблица parking_requests");
            printTable(new String[] {"ID", "Владелец", "Автомобиль", "Место", "Начало", "Окончание", "Статус", "Создана"},
                requests.stream()
                    .map(r -> new String[] {
                        String.valueOf(r.getId()), String.valueOf(r.getUserId()), String.valueOf(r.getVehicleId()),
                        String.valueOf(r.getSpotId()), DATE_TIME_FORMATTER.format(r.getStartTime()),
                        DATE_TIME_FORMATTER.format(r.getEndTime()), r.getStatus().getTitle(),
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

    private String readInput(String prompt) {
        String input = readLine(prompt);
        if (input.trim().equalsIgnoreCase(CANCEL_COMMAND)) {
            throw new InputCancelledException();
        }
        return input;
    }

    private void perform(Runnable action) {
        try {
            action.run();
        } catch (InputCancelledException e) {
            System.out.println("Действие отменено");
        }
    }

    private <T> T readValid(String prompt, Function<String, T> check) {
        while (true) {
            try {
                return check.apply(readInput(prompt));
            } catch (BusinessException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
    }

    private <T> T readValidOrKeep(String prompt, T current, Function<String, T> check) {
        return readValidOrKeep(prompt, String.valueOf(current), current, check);
    }

    private <T> T readValidOrKeep(String prompt, String shown, T current, Function<String, T> check) {
        while (true) {
            try {
                String input = readInput(prompt + " [" + shown + "]: ");
                return input.isBlank() ? current : check.apply(input);
            } catch (BusinessException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
    }

    private LocalDateTime readDateTimeOrKeep(String prompt, LocalDateTime current) {
        return readValidOrKeep(prompt, DATE_TIME_FORMATTER.format(current), current, this::parseDateTime);
    }

    private LocalDateTime readEndTime(Supplier<LocalDateTime> reader, LocalDateTime startTime) {
        while (true) {
            LocalDateTime endTime = reader.get();
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
        return readValid(prompt, this::parseLong);
    }

    private LocalDateTime readDateTime(String prompt) {
        return readValid(prompt, this::parseDateTime);
    }

    private <T extends LookupValue> T readOption(String title, T[] values) {
        printOptions(title, values);
        return readValid("Выберите номер: ", input -> parseOption(input, values));
    }

    private <T extends LookupValue> T readOptionOrKeep(String title, T current, T[] values) {
        printOptions(title, values);
        return readValidOrKeep("Выберите номер", current, input -> parseOption(input, values));
    }

    private void printOptions(String title, LookupValue[] values) {
        System.out.println(title + ":");
        for (LookupValue value : values) {
            System.out.println(value.getId() + ". " + value.getTitle());
        }
    }

    private long parseLong(String input) {
        try {
            return Long.parseLong(input.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException("введите целое число");
        }
    }

    private LocalDateTime parseDateTime(String input) {
        try {
            return LocalDateTime.parse(input.trim(), DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new BusinessException("некорректный формат даты/времени, ожидается " + DATE_TIME_HINT);
        }
    }

    private <T extends LookupValue> T parseOption(String input, T[] values) {
        try {
            int id = Integer.parseInt(input.trim());
            for (T value : values) {
                if (value.getId() == id) {
                    return value;
                }
            }
        } catch (NumberFormatException e) {
            // нечисловой ввод обрабатывается так же, как номер вне списка
        }
        throw new BusinessException("выберите номер из списка");
    }

    private static class InputCancelledException extends RuntimeException {
    }
}
