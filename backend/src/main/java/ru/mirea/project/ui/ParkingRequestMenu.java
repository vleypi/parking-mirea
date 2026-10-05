package ru.mirea.project.ui;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import ru.mirea.project.dto.ParkingRequestStatistics;
import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.model.entity.ParkingRequest;
import ru.mirea.project.model.entity.ParkingSpot;
import ru.mirea.project.model.entity.User;
import ru.mirea.project.model.entity.Vehicle;
import ru.mirea.project.model.enums.RequestStatus;
import ru.mirea.project.model.enums.SpotType;
import ru.mirea.project.service.ParkingRequestService;
import ru.mirea.project.service.ParkingSpotService;
import ru.mirea.project.service.UserService;
import ru.mirea.project.service.VehicleService;

public class ParkingRequestMenu extends EntityMenu {
    private final ParkingRequestService parkingRequestService;
    private final UserService userService;
    private final VehicleService vehicleService;
    private final ParkingSpotService parkingSpotService;

    public ParkingRequestMenu(ConsoleInput input, ParkingRequestService parkingRequestService, UserService userService,
                              VehicleService vehicleService, ParkingSpotService parkingSpotService) {
        super(input);
        this.parkingRequestService = parkingRequestService;
        this.userService = userService;
        this.vehicleService = vehicleService;
        this.parkingSpotService = parkingSpotService;
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
        printOrEmpty(parkingRequestService.getAll(), this::printList);
    }

    @Override
    protected void findById() {
        printList(List.of(parkingRequestService.getById(input.readLong("ID заявки: "))));
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
        parkingSpotService.getAll().forEach(System.out::println);
        long spotId = input.readLong("ID места: ");
        LocalDateTime startTime = input.readDateTime("Начало (" + ConsoleInput.DATE_TIME_HINT + "): ");
        LocalDateTime endTime = readEndTime(
            () -> input.readDateTime("Окончание (" + ConsoleInput.DATE_TIME_HINT + "): "), startTime);

        ParkingRequest created = parkingRequestService.create(userId, vehicleId, spotId, startTime, endTime);
        System.out.println("Заявка создана:");
        printList(List.of(created));
    }

    @Override
    protected void update() {
        long id = input.readLong("ID заявки: ");
        ParkingRequest existing = parkingRequestService.getById(id);
        parkingRequestService.checkEditable(existing);
        System.out.println(ConsoleInput.EDIT_HINT);
        System.out.println("Автомобили владельца:");
        vehicleService.getByUserId(existing.getUserId()).forEach(System.out::println);
        long vehicleId = input.readValidOrKeep("ID автомобиля", existing.getVehicleId(), this::parseId);
        System.out.println("Парковочные места:");
        parkingSpotService.getAll().forEach(System.out::println);
        long spotId = input.readValidOrKeep("ID места", existing.getSpotId(), this::parseId);
        LocalDateTime startTime = input.readDateTimeOrKeep("Начало (" + ConsoleInput.DATE_TIME_HINT + ")",
            existing.getStartTime());
        LocalDateTime endTime = readEndTime(
            () -> input.readDateTimeOrKeep("Окончание (" + ConsoleInput.DATE_TIME_HINT + ")", existing.getEndTime()),
            startTime);

        ParkingRequest updated = parkingRequestService.update(id, vehicleId, spotId, startTime, endTime);
        System.out.println("Заявка обновлена:");
        printList(List.of(updated));
    }

    @Override
    protected void delete() {
        parkingRequestService.delete(input.readLong("ID заявки: "));
        System.out.println("Заявка удалена");
    }

    @Override
    protected void search() {
        runSubmenu("Поиск заявок",
            option("По гос. номеру", () -> printOrEmpty(
                parkingRequestService.searchByLicensePlate(input.readText("Фрагмент гос. номера: ")), this::printList)),
            option("По имени владельца", this::searchByOwnerName),
            option("По номеру места", () -> printOrEmpty(parkingRequestService.searchBySpotNumber(
                input.readValid("Номер места: ", parkingSpotService::parseSpotNumber)), this::printList)));
    }

    @Override
    protected void filter() {
        runSubmenu("Фильтры и сортировка заявок",
            option("По статусу", () -> printOrEmpty(
                parkingRequestService.filterByStatus(input.readOption("Статус", RequestStatus.values())), this::printList)),
            option("За период", () -> printOrEmpty(
                parkingRequestService.filterByPeriod(input.readPeriod(false)), this::printList)),
            option("По типу места", () -> printOrEmpty(
                parkingRequestService.filterBySpotType(input.readOption("Тип места", SpotType.values())), this::printList)),
            option("Сортировка по началу", () -> printOrEmpty(
                parkingRequestService.sortByStartTime(input.readAscending()), this::printList)),
            option("Сортировка по дате создания", () -> printOrEmpty(
                parkingRequestService.sortByCreatedAt(input.readAscending()), this::printList)));
    }

    @Override
    protected void statistics() {
        ParkingRequestStatistics stats = parkingRequestService.getStatistics(input.readPeriod(false));

        System.out.println();
        System.out.println("Статистика заявок (" + stats.period() + ")");
        printStatusCounts(stats.requestsByStatus());
        System.out.println("Выручка по завершённым заявкам: " + stats.revenue() + " руб.");
        System.out.println("Средняя длительность (без отменённых), ч: " + stats.averageDurationHours());
    }

    private void changeStatus() {
        long id = input.readLong("ID заявки: ");
        ParkingRequest current = parkingRequestService.getById(id);
        System.out.println("Текущий статус: " + current.getStatus().getTitle());
        RequestStatus newStatus = input.readOption("Новый статус", RequestStatus.values());

        ParkingRequest updated = parkingRequestService.changeStatus(id, newStatus);
        System.out.println("Статус обновлён:");
        printList(List.of(updated));
    }

    private void searchByOwnerName() {
        Map<User, List<ParkingRequest>> found =
            parkingRequestService.searchByOwnerName(input.readText("Фрагмент имени владельца: "));
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
                parkingRequestService.checkPeriod(startTime, endTime);
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

    private void printList(List<ParkingRequest> requests) {
        Map<Long, String> ownerNames = userService.getAll().stream()
            .collect(Collectors.toMap(User::getId, User::getName));
        Map<Long, String> plates = vehicleService.getAll().stream()
            .collect(Collectors.toMap(Vehicle::getId, Vehicle::getLicensePlate));
        Map<Long, Integer> spotNumbers = parkingSpotService.getAll().stream()
            .collect(Collectors.toMap(ParkingSpot::getId, ParkingSpot::getSpotNumber));
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
