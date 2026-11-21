package ru.mirea.project.ui;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.mirea.project.dto.filter.VehicleFilter;
import ru.mirea.project.dto.statistics.VehicleStatistics;
import ru.mirea.project.model.entity.User;
import ru.mirea.project.model.entity.Vehicle;
import ru.mirea.project.model.value.Period;
import ru.mirea.project.service.UserService;
import ru.mirea.project.service.VehicleService;

public class VehicleMenu extends EntityMenu {
    private final VehicleService vehicleService;
    private final UserService userService;

    public VehicleMenu(ConsoleInput input, VehicleService vehicleService, UserService userService) {
        super(input);
        this.vehicleService = vehicleService;
        this.userService = userService;
    }

    @Override
    public String title() {
        return "Автомобили";
    }

    @Override
    protected void showAll() {
        printOrEmpty(vehicleService.getAll(), this::printList);
    }

    @Override
    protected void findById() {
        printList(List.of(vehicleService.getById(input.readLong("ID автомобиля: "))));
    }

    @Override
    protected void create() {
        System.out.println(ConsoleInput.CREATE_HINT);
        long userId = input.readLong("ID владельца: ");
        userService.getById(userId);
        String plate = input.readValid("Гос. номер (например А123ВС777): ", value -> vehicleService.checkLicensePlate(0, value));
        String brand = input.readValid("Марка: ", vehicleService::checkBrand);
        String model = input.readValid("Модель: ", vehicleService::checkModel);

        Vehicle created = vehicleService.create(userId, plate, brand, model);
        System.out.println("Автомобиль создан:");
        printList(List.of(created));
    }

    @Override
    protected void update() {
        long id = input.readLong("ID автомобиля: ");
        Vehicle existing = vehicleService.getById(id);
        System.out.println(ConsoleInput.EDIT_HINT);
        String plate = input.readValidOrKeep("Гос. номер", existing.getLicensePlate(),
            value -> vehicleService.checkLicensePlate(id, value));
        String brand = input.readValidOrKeep("Марка", existing.getBrand(), vehicleService::checkBrand);
        String model = input.readValidOrKeep("Модель", existing.getModel(), vehicleService::checkModel);

        Vehicle updated = vehicleService.update(id, plate, brand, model);
        System.out.println("Автомобиль обновлён:");
        printList(List.of(updated));
    }

    @Override
    protected void delete() {
        vehicleService.delete(input.readLong("ID автомобиля: "));
        System.out.println("Автомобиль удалён");
    }

    @Override
    protected List<MenuOption> searchOptions() {
        return List.of(
            option("По гос. номеру", () -> printOrEmpty(
                vehicleService.searchByLicensePlate(input.readText("Фрагмент гос. номера: ")), this::printList)),
            option("Стояли на парковке в период", this::searchParkedDuring));
    }

    @Override
    protected void filter() {
        VehicleFilter filter = new VehicleFilter();
        runFilterScreen(filter, () -> vehicleService.find(filter), this::printList, column -> askFilter(filter, column));
    }

    private void askFilter(VehicleFilter filter, VehicleFilter.Column column) {
        switch (column) {
            case PLATE -> ask("Гос. номер содержит: ", filter::setPlateContains);
            case REGION -> ask("Регион: ", filter::setRegion);
            case BRAND -> ask("Марка содержит: ", filter::setBrandContains);
            case MODEL -> ask("Модель содержит: ", filter::setModelContains);
            case OWNER -> ask("Имя владельца содержит: ", filter::setOwnerNameContains);
        }
    }

    @Override
    protected void statistics() {
        long id = input.readLong("ID автомобиля: ");
        vehicleService.getById(id);
        VehicleStatistics stats = vehicleService.getStatistics(id, input.readPeriod(false));
        Vehicle vehicle = stats.vehicle();

        System.out.println();
        System.out.println("Статистика автомобиля " + vehicle.getLicensePlate() + " " + vehicle.getBrand() + " "
            + vehicle.getModel() + " (" + stats.period() + ")");
        printStatusCounts(stats.requestsByStatus());
        System.out.println("Часов на парковке (без отменённых заявок): " + stats.parkedHours());
        System.out.println("Оплачено по завершённым заявкам: " + stats.totalPaid() + " руб.");
    }

    private void searchParkedDuring() {
        Period period = input.readPeriod(true);
        System.out.println("Автомобили на парковке " + period + ":");
        printOrEmpty(vehicleService.findParkedDuring(period), this::printList);
    }

    private void printList(List<Vehicle> vehicles) {
        Map<Long, String> ownerNames = userService.getAll().stream()
            .collect(Collectors.toMap(User::getId, User::getName));
        TablePrinter.print(new String[] {"ID", "Гос. номер", "Регион", "Марка", "Модель", "Владелец", "ID владельца"},
            vehicles.stream()
                .map(v -> new String[] {
                    String.valueOf(v.getId()), v.getLicensePlate(), v.getRegion(), v.getBrand(), v.getModel(),
                    ownerNames.getOrDefault(v.getUserId(), "?"), String.valueOf(v.getUserId())})
                .toList());
    }
}
