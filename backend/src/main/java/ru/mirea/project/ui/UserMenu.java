package ru.mirea.project.ui;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.mirea.project.dto.filter.UserFilter;
import ru.mirea.project.dto.statistics.UserStatistics;
import ru.mirea.project.model.entity.User;
import ru.mirea.project.model.entity.Vehicle;
import ru.mirea.project.service.UserService;
import ru.mirea.project.service.VehicleService;

public class UserMenu extends EntityMenu {
    private final UserService userService;
    private final VehicleService vehicleService;

    public UserMenu(ConsoleInput input, UserService userService, VehicleService vehicleService) {
        super(input);
        this.userService = userService;
        this.vehicleService = vehicleService;
    }

    @Override
    public String title() {
        return "Владельцы";
    }

    @Override
    protected void showAll() {
        printOrEmpty(userService.getAll(), this::printList);
    }

    @Override
    protected void findById() {
        printList(List.of(userService.getById(input.readLong("ID владельца: "))));
    }

    @Override
    protected void create() {
        System.out.println(ConsoleInput.CREATE_HINT);
        String name = input.readValid("Имя: ", userService::checkName);
        String phone = input.readValid("Телефон (например 89991234567): ", value -> userService.checkPhone(0, value));

        User created = userService.create(name, phone);
        System.out.println("Владелец создан:");
        printList(List.of(created));
    }

    @Override
    protected void update() {
        long id = input.readLong("ID владельца: ");
        User existing = userService.getById(id);
        System.out.println(ConsoleInput.EDIT_HINT);
        String name = input.readValidOrKeep("Имя", existing.getName(), userService::checkName);
        String phone = input.readValidOrKeep("Телефон", existing.getPhone(), value -> userService.checkPhone(id, value));

        User updated = userService.update(id, name, phone);
        System.out.println("Владелец обновлён:");
        printList(List.of(updated));
    }

    @Override
    protected void delete() {
        userService.delete(input.readLong("ID владельца: "));
        System.out.println("Владелец удалён");
    }

    @Override
    protected List<MenuOption> searchOptions() {
        return List.of(
            option("По имени", () -> printOrEmpty(
                userService.searchByName(input.readText("Фрагмент имени: ")), this::printList)),
            option("По телефону", () -> printOrEmpty(
                userService.searchByPhone(input.readText("Цифры телефона: ")), this::printList)));
    }

    @Override
    protected void filter() {
        UserFilter filter = new UserFilter();
        runFilterScreen(filter, () -> userService.find(filter), this::printList, column -> askFilter(filter, column));
    }

    private void askFilter(UserFilter filter, UserFilter.Column column) {
        switch (column) {
            case NAME -> ask("Имя содержит: ", filter::setNameContains);
            case PHONE -> ask("Цифры телефона: ", filter::setPhoneDigits);
            case VEHICLES -> filter.setVehicleCount(input.readRange("Количество машин"));
            case REGISTERED -> filter.setRegistered(input.readPeriod(false));
            case ACTIVE_REQUESTS -> filter.setHasActiveRequests(input.readYesNo("Есть активные заявки?"));
        }
    }

    @Override
    protected void statistics() {
        long id = input.readLong("ID владельца: ");
        userService.getById(id);
        UserStatistics stats = userService.getStatistics(id, input.readPeriod(false));

        System.out.println();
        System.out.println("Статистика владельца " + stats.user().getName() + " (" + stats.period() + ")");
        System.out.println("Автомобилей: " + stats.vehicleCount());
        printStatusCounts(stats.requestsByStatus());
        System.out.println("Оплачено по завершённым заявкам: " + stats.totalPaid() + " руб.");
    }

    private void printList(List<User> users) {
        Map<Long, Long> vehicleCounts = vehicleService.getAll().stream()
            .collect(Collectors.groupingBy(Vehicle::getUserId, Collectors.counting()));
        TablePrinter.print(new String[] {"ID", "Имя", "Телефон", "Машин", "Зарегистрирован"},
            users.stream()
                .map(u -> new String[] {
                    String.valueOf(u.getId()), u.getName(), u.getPhone(),
                    String.valueOf(vehicleCounts.getOrDefault(u.getId(), 0L)),
                    ConsoleInput.format(u.getCreatedAt())})
                .toList());
    }
}
