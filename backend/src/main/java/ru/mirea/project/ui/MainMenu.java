package ru.mirea.project.ui;

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import ru.mirea.project.dto.GeneralStatistics;
import ru.mirea.project.model.entity.Request;
import ru.mirea.project.model.entity.Spot;
import ru.mirea.project.model.entity.User;
import ru.mirea.project.model.entity.Vehicle;
import ru.mirea.project.model.enums.RequestStatus;
import ru.mirea.project.model.enums.SpotType;
import ru.mirea.project.service.RequestService;
import ru.mirea.project.service.SpotService;
import ru.mirea.project.service.StatisticsService;
import ru.mirea.project.service.UserService;
import ru.mirea.project.service.VehicleService;
import ru.mirea.project.util.ExcelExporter;

public class MainMenu {
    private static final String EXPORT_FILE_NAME = "parking_export.xlsx";

    private final ConsoleInput input;
    private final List<EntityMenu> menus;
    private final StatisticsService statisticsService;
    private final UserService userService;
    private final VehicleService vehicleService;
    private final SpotService spotService;
    private final RequestService requestService;

    public MainMenu(ConsoleInput input, List<EntityMenu> menus, StatisticsService statisticsService,
                    UserService userService, VehicleService vehicleService,
                    SpotService spotService, RequestService requestService) {
        this.input = input;
        this.menus = menus;
        this.statisticsService = statisticsService;
        this.userService = userService;
        this.vehicleService = vehicleService;
        this.spotService = spotService;
        this.requestService = requestService;
    }

    public void run() {
        int statisticsItem = menus.size() + 1;
        int exportItem = menus.size() + 2;
        int tablesItem = menus.size() + 3;
        while (true) {
            System.out.println();
            System.out.println("ПАРКОВОЧНАЯ СИСТЕМА");
            for (int i = 0; i < menus.size(); i++) {
                System.out.println((i + 1) + ". " + menus.get(i).title());
            }
            System.out.println(statisticsItem + ". Общая статистика за период");
            System.out.println(exportItem + ". Экспорт в Excel");
            System.out.println(tablesItem + ". Таблицы базы данных");
            System.out.println("0. Выход");
            int choice = input.readMenuChoice("Выберите действие: ");
            if (choice == 0) {
                System.out.println("До свидания!");
                return;
            }
            if (choice >= 1 && choice <= menus.size()) {
                menus.get(choice - 1).run();
            } else if (choice == statisticsItem) {
                EntityMenu.perform(this::showStatistics);
            } else if (choice == exportItem) {
                EntityMenu.perform(this::exportData);
            } else if (choice == tablesItem) {
                EntityMenu.perform(this::showTables);
            } else {
                System.out.println("Неизвестный пункт меню");
            }
        }
    }

    private void showStatistics() {
        GeneralStatistics stats = statisticsService.getGeneralStatistics(input.readPeriod(false));
        System.out.println();
        System.out.println("Общая статистика (" + stats.period() + ")");
        System.out.println("Всего владельцев: " + stats.totalUsers());
        System.out.println("Всего автомобилей: " + stats.totalVehicles());
        System.out.println("Всего парковочных мест: " + stats.totalSpots());
        System.out.println("Заявок за период: " + stats.requests());
        System.out.println("Активных (Новая + Подтверждена): " + stats.active());
        System.out.println("Завершённых: " + stats.completed());
        System.out.println("Отменённых: " + stats.cancelled());
        System.out.println("Занятых мест прямо сейчас: " + stats.occupiedNow());
        System.out.println("Выручка по завершённым заявкам: " + stats.revenue() + " руб.");
    }

    private void exportData() {
        Path file = Path.of(EXPORT_FILE_NAME).toAbsolutePath();
        try {
            ExcelExporter.export(userService.getAll(), vehicleService.getAll(),
                spotService.getAll(), requestService.getAll(), file);
            System.out.println("Экспорт выполнен: " + file);
        } catch (UncheckedIOException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void showTables() {
        List<User> users = userService.getAll();
        List<Vehicle> vehicles = vehicleService.getAll();
        List<Spot> spots = spotService.getAll();
        List<Request> requests = requestService.getAll();

        System.out.println();
        System.out.println("Таблица request_statuses");
        TablePrinter.print(new String[] {"ID", "Код", "Название"},
            Arrays.stream(RequestStatus.values())
                .map(s -> new String[] {String.valueOf(s.getId()), s.name(), s.getTitle()})
                .toList());

        System.out.println();
        System.out.println("Таблица spot_types");
        TablePrinter.print(new String[] {"ID", "Код", "Название"},
            Arrays.stream(SpotType.values())
                .map(t -> new String[] {String.valueOf(t.getId()), t.name(), t.getTitle()})
                .toList());

        System.out.println();
        System.out.println("Таблица users");
        TablePrinter.print(new String[] {"ID", "Имя", "Телефон", "Создан"},
            users.stream()
                .map(u -> new String[] {
                    String.valueOf(u.getId()), u.getName(), u.getPhone(), ConsoleInput.format(u.getCreatedAt())})
                .toList());

        System.out.println();
        System.out.println("Таблица vehicles");
        TablePrinter.print(new String[] {"ID", "Владелец", "Гос. номер", "Марка", "Модель"},
            vehicles.stream()
                .map(v -> new String[] {
                    String.valueOf(v.getId()), String.valueOf(v.getUserId()), v.getLicensePlate(),
                    v.getBrand(), v.getModel()})
                .toList());

        System.out.println();
        System.out.println("Таблица spots");
        TablePrinter.print(new String[] {"ID", "Номер места", "Тип", "Тариф, руб/ч"},
            spots.stream()
                .map(s -> new String[] {
                    String.valueOf(s.getId()), String.valueOf(s.getSpotNumber()), s.getSpotType().getTitle(),
                    s.getHourlyRate().toPlainString()})
                .toList());

        System.out.println();
        System.out.println("Таблица requests");
        TablePrinter.print(new String[] {"ID", "Владелец", "Автомобиль", "Место", "Начало", "Окончание", "Статус", "Создана"},
            requests.stream()
                .map(r -> new String[] {
                    String.valueOf(r.getId()), String.valueOf(r.getUserId()), String.valueOf(r.getVehicleId()),
                    String.valueOf(r.getSpotId()), ConsoleInput.format(r.getStartTime()),
                    ConsoleInput.format(r.getEndTime()), r.getStatus().getTitle(),
                    ConsoleInput.format(r.getCreatedAt())})
                .toList());
    }
}
