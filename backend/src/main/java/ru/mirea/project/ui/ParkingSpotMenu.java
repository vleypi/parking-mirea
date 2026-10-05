package ru.mirea.project.ui;

import java.math.BigDecimal;
import java.util.List;

import ru.mirea.project.dto.ParkingSpotStatistics;
import ru.mirea.project.model.entity.ParkingSpot;
import ru.mirea.project.model.enums.SpotType;
import ru.mirea.project.model.value.Period;
import ru.mirea.project.service.ParkingSpotService;

public class ParkingSpotMenu extends EntityMenu {
    private final ParkingSpotService parkingSpotService;

    public ParkingSpotMenu(ConsoleInput input, ParkingSpotService parkingSpotService) {
        super(input);
        this.parkingSpotService = parkingSpotService;
    }

    @Override
    public String title() {
        return "Парковочные места";
    }

    @Override
    protected void showAll() {
        printOrEmpty(parkingSpotService.getAll(), this::printList);
    }

    @Override
    protected void findById() {
        printList(List.of(parkingSpotService.getById(input.readLong("ID места: "))));
    }

    @Override
    protected void create() {
        System.out.println(ConsoleInput.CREATE_HINT);
        int spotNumber = input.readValid("Номер места: ",
            value -> parkingSpotService.checkSpotNumberUnique(0, parkingSpotService.parseSpotNumber(value)));
        SpotType spotType = input.readOption("Тип места", SpotType.values());
        BigDecimal rate = input.readValid("Тариф, руб/час: ", parkingSpotService::parseHourlyRate);

        ParkingSpot created = parkingSpotService.create(spotNumber, spotType, rate);
        System.out.println("Место создано:");
        printList(List.of(created));
    }

    @Override
    protected void update() {
        long id = input.readLong("ID места: ");
        ParkingSpot existing = parkingSpotService.getById(id);
        System.out.println(ConsoleInput.EDIT_HINT);
        int spotNumber = input.readValidOrKeep("Номер места", existing.getSpotNumber(),
            value -> parkingSpotService.checkSpotNumberUnique(id, parkingSpotService.parseSpotNumber(value)));
        SpotType spotType = input.readOptionOrKeep("Тип места", existing.getSpotType(), SpotType.values());
        BigDecimal rate = input.readValidOrKeep("Тариф, руб/час", existing.getHourlyRate(), parkingSpotService::parseHourlyRate);

        ParkingSpot updated = parkingSpotService.update(id, spotNumber, spotType, rate);
        System.out.println("Место обновлено:");
        printList(List.of(updated));
    }

    @Override
    protected void delete() {
        parkingSpotService.delete(input.readLong("ID места: "));
        System.out.println("Место удалено");
    }

    @Override
    protected void search() {
        runSubmenu("Поиск мест",
            option("По номеру", () -> printList(List.of(parkingSpotService.findByNumber(
                input.readValid("Номер места: ", parkingSpotService::parseSpotNumber))))),
            option("Свободные на период", this::searchFree));
    }

    @Override
    protected void filter() {
        runSubmenu("Фильтры и сортировка мест",
            option("По типу", () -> printOrEmpty(
                parkingSpotService.filterByType(input.readOption("Тип места", SpotType.values())), this::printList)),
            option("По диапазону тарифа", this::filterByRate),
            option("Сортировка по номеру", () -> printOrEmpty(
                parkingSpotService.sortByNumber(input.readAscending()), this::printList)),
            option("Сортировка по тарифу", () -> printOrEmpty(
                parkingSpotService.sortByRate(input.readAscending()), this::printList)));
    }

    @Override
    protected void statistics() {
        long id = input.readLong("ID места: ");
        parkingSpotService.getById(id);
        ParkingSpotStatistics stats = parkingSpotService.getStatistics(id, input.readPeriod(false));

        System.out.println();
        System.out.println("Статистика места " + stats.spot().getSpotNumber() + " (" + stats.spot().getSpotType().getTitle()
            + ", " + stats.period() + ")");
        System.out.println("Заявок (без отменённых): " + stats.requestCount());
        System.out.println("Занято часов: " + stats.occupiedHours());
        System.out.println("Выручка по завершённым заявкам: " + stats.revenue() + " руб.");
    }

    private void searchFree() {
        Period period = input.readPeriod(true);
        System.out.println("Свободные места " + period + ":");
        printOrEmpty(parkingSpotService.findFreeDuring(period), this::printList);
    }

    private void filterByRate() {
        BigDecimal min = input.readValid("Тариф от, руб/час: ", parkingSpotService::parseHourlyRate);
        BigDecimal max = input.readValid("Тариф до, руб/час: ", parkingSpotService::parseHourlyRate);
        printOrEmpty(parkingSpotService.filterByRateRange(min, max), this::printList);
    }

    private void printList(List<ParkingSpot> spots) {
        TablePrinter.print(new String[] {"ID", "Номер", "Тип", "Тариф, руб/ч"},
            spots.stream()
                .map(s -> new String[] {
                    String.valueOf(s.getId()), String.valueOf(s.getSpotNumber()),
                    s.getSpotType().getTitle(), s.getHourlyRate().toPlainString()})
                .toList());
    }
}
