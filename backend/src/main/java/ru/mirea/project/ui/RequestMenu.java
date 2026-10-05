package ru.mirea.project.ui;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import ru.mirea.project.dto.RequestStatistics;
import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.model.entity.Request;
import ru.mirea.project.model.entity.Spot;
import ru.mirea.project.model.entity.User;
import ru.mirea.project.model.entity.Vehicle;
import ru.mirea.project.model.enums.RequestStatus;
import ru.mirea.project.model.enums.SpotType;
import ru.mirea.project.service.RequestService;
import ru.mirea.project.service.SpotService;
import ru.mirea.project.service.UserService;
import ru.mirea.project.service.VehicleService;

public class RequestMenu extends EntityMenu {
    private final RequestService requestService;
    private final UserService userService;
    private final VehicleService vehicleService;
    private final SpotService spotService;

    public RequestMenu(ConsoleInput input, RequestService requestService, UserService userService,
                              VehicleService vehicleService, SpotService spotService) {
        super(input);
        this.requestService = requestService;
        this.userService = userService;
        this.vehicleService = vehicleService;
        this.spotService = spotService;
    }

    @Override
    public String title() {
        return "Заявки на парковку";
    }

    @Override
    protected List<MenuOption> extraOptions() {
        return List.of(option("Сменить статус", this::changeStatus));
    }

    @Override
    protected void showAll() {
        printOrEmpty(requestService.getAll(), this::printList);
    }

    @Override
    protected void findById() {
        printList(List.of(requestService.getById(input.readLong("ID заявки: "))));
    }

    @Override
    protected void create() {
        System.out.println(ConsoleInput.CREATE_HINT);
        long userId = input.readLong("ID владельца: ");
        List<Vehicle> vehicles = vehicleService.getByUserId(userId);
        if (vehicles.isEmpty()) {
            System.out.println("У владельца нет автомобилей. Сначала добавьте автомобиль в разделе «Автомобили»");
            return;
        }
        System.out.println("Автомобили владельца:");
        vehicles.forEach(System.out::println);
        long vehicleId = input.readLong("ID автомобиля: ");
        System.out.println("Парковочные места:");
        spotService.getAll().forEach(System.out::println);
        long spotId = input.readLong("ID места: ");
        LocalDateTime startTime = input.readDateTime("Начало (" + ConsoleInput.DATE_TIME_HINT + "): ");
        LocalDateTime endTime = readEndTime(
            () -> input.readDateTime("Окончание (" + ConsoleInput.DATE_TIME_HINT + "): "), startTime);

        Request created = requestService.create(userId, vehicleId, spotId, startTime, endTime);
        System.out.println("Заявка создана:");
        printList(List.of(created));
    }

    @Override
    protected void update() {
        long id = input.readLong("ID заявки: ");
        Request existing = requestService.getById(id);
        requestService.checkEditable(existing);
        System.out.println(ConsoleInput.EDIT_HINT);
        System.out.println("Автомобили владельца:");
        vehicleService.getByUserId(existing.getUserId()).forEach(System.out::println);
        long vehicleId = input.readValidOrKeep("ID автомобиля", existing.getVehicleId(), this::parseId);
        System.out.println("Парковочные места:");
        spotService.getAll().forEach(System.out::println);
        long spotId = input.readValidOrKeep("ID места", existing.getSpotId(), this::parseId);
        LocalDateTime startTime = input.readDateTimeOrKeep("Начало (" + ConsoleInput.DATE_TIME_HINT + ")",
            existing.getStartTime());
        LocalDateTime endTime = readEndTime(
            () -> input.readDateTimeOrKeep("Окончание (" + ConsoleInput.DATE_TIME_HINT + ")", existing.getEndTime()),
            startTime);

        Request updated = requestService.update(id, vehicleId, spotId, startTime, endTime);
        System.out.println("Заявка обновлена:");
        printList(List.of(updated));
    }

    @Override
    protected void delete() {
        requestService.delete(input.readLong("ID заявки: "));
        System.out.println("Заявка удалена");
    }

    @Override
    protected void search() {
        runSubmenu("Поиск заявок",
            option("По гос. номеру", () -> printOrEmpty(
                requestService.searchByLicensePlate(input.readText("Фрагмент гос. номера: ")), this::printList)),
            option("По имени владельца", this::searchByOwnerName),
            option("По номеру места", () -> printOrEmpty(requestService.searchBySpotNumber(
                input.readValid("Номер места: ", spotService::parseSpotNumber)), this::printList)));
    }

    @Override
    protected void filter() {
        runSubmenu("Фильтры и сортировка заявок",
            option("По статусу", () -> printOrEmpty(
                requestService.filterByStatus(input.readOption("Статус", RequestStatus.values())), this::printList)),
            option("За период", () -> printOrEmpty(
                requestService.filterByPeriod(input.readPeriod(false)), this::printList)),
            option("По типу места", () -> printOrEmpty(
                requestService.filterBySpotType(input.readOption("Тип места", SpotType.values())), this::printList)),
            option("Сортировка по началу", () -> printOrEmpty(
                requestService.sortByStartTime(input.readAscending()), this::printList)),
            option("Сортировка по дате создания", () -> printOrEmpty(
                requestService.sortByCreatedAt(input.readAscending()), this::printList)));
    }

    @Override
    protected void statistics() {
        RequestStatistics stats = requestService.getStatistics(input.readPeriod(false));

        System.out.println();
        System.out.println("Статистика заявок (" + stats.period() + ")");
        printStatusCounts(stats.requestsByStatus());
        System.out.println("Выручка по завершённым заявкам: " + stats.revenue() + " руб.");
        System.out.println("Средняя длительность (без отменённых), ч: " + stats.averageDurationHours());
    }

    private void changeStatus() {
        long id = input.readLong("ID заявки: ");
        Request current = requestService.getById(id);
        System.out.println("Текущий статус: " + current.getStatus().getTitle());
        RequestStatus newStatus = input.readOption("Новый статус", RequestStatus.values());

        Request updated = requestService.changeStatus(id, newStatus);
        System.out.println("Статус обновлён:");
        printList(List.of(updated));
    }

    private void searchByOwnerName() {
        Map<User, List<Request>> found =
            requestService.searchByOwnerName(input.readText("Фрагмент имени владельца: "));
        if (found.isEmpty()) {
            System.out.println("Ничего не найдено");
            return;
        }
        found.forEach((user, requests) -> {
            System.out.println();
            System.out.println("Владелец: " + user.getName() + " (" + user.getPhone() + ")");
            if (requests.isEmpty()) {
                System.out.println("Заявок нет");
            } else {
                printList(requests);
            }
        });
    }

    private LocalDateTime readEndTime(Supplier<LocalDateTime> reader, LocalDateTime startTime) {
        while (true) {
            LocalDateTime endTime = reader.get();
            try {
                requestService.checkPeriod(startTime, endTime);
                return endTime;
            } catch (BusinessException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
    }

    private long parseId(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException("введите целое число");
        }
    }

    private void printList(List<Request> requests) {
        Map<Long, String> ownerNames = userService.getAll().stream()
            .collect(Collectors.toMap(User::getId, User::getName));
        Map<Long, String> plates = vehicleService.getAll().stream()
            .collect(Collectors.toMap(Vehicle::getId, Vehicle::getLicensePlate));
        Map<Long, Integer> spotNumbers = spotService.getAll().stream()
            .collect(Collectors.toMap(Spot::getId, Spot::getSpotNumber));
        TablePrinter.print(new String[] {"ID", "Владелец", "Автомобиль", "Место", "Начало", "Окончание", "Статус"},
            requests.stream()
                .map(r -> new String[] {
                    String.valueOf(r.getId()), ownerNames.getOrDefault(r.getUserId(), "?"),
                    plates.getOrDefault(r.getVehicleId(), "?"), String.valueOf(spotNumbers.get(r.getSpotId())),
                    ConsoleInput.format(r.getStartTime()), ConsoleInput.format(r.getEndTime()),
                    r.getStatus().getTitle()})
                .toList());
    }
}
