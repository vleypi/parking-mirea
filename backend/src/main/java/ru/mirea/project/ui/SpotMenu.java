package ru.mirea.project.ui;

import java.math.BigDecimal;
import java.util.List;

import ru.mirea.project.dto.filter.SpotFilter;
import ru.mirea.project.dto.statistics.SpotStatistics;
import ru.mirea.project.model.entity.Spot;
import ru.mirea.project.model.enums.SpotType;
import ru.mirea.project.model.value.Period;
import ru.mirea.project.service.SpotService;

public class SpotMenu extends EntityMenu {
    private final SpotService spotService;

    public SpotMenu(ConsoleInput input, SpotService spotService) {
        super(input);
        this.spotService = spotService;
    }

    @Override
    public String title() {
        return "Парковочные места";
    }

    @Override
    protected void showAll() {
        printOrEmpty(spotService.getAll(), this::printList);
    }

    @Override
    protected void findById() {
        printList(List.of(spotService.getById(input.readLong("ID места: "))));
    }

    @Override
    protected void create() {
        System.out.println(ConsoleInput.CREATE_HINT);
        int spotNumber = input.readValid("Номер места: ",
            value -> spotService.checkSpotNumberUnique(0, spotService.parseSpotNumber(value)));
        SpotType spotType = input.readOption("Тип места", SpotType.values());
        BigDecimal rate = input.readValid("Тариф, руб/час: ", spotService::parseHourlyRate);

        Spot created = spotService.create(spotNumber, spotType, rate);
        System.out.println("Место создано:");
        printList(List.of(created));
    }

    @Override
    protected void update() {
        long id = input.readLong("ID места: ");
        Spot existing = spotService.getById(id);
        System.out.println(ConsoleInput.EDIT_HINT);
        int spotNumber = input.readValidOrKeep("Номер места", existing.getSpotNumber(),
            value -> spotService.checkSpotNumberUnique(id, spotService.parseSpotNumber(value)));
        SpotType spotType = input.readOptionOrKeep("Тип места", existing.getSpotType(), SpotType.values());
        BigDecimal rate = input.readValidOrKeep("Тариф, руб/час", existing.getHourlyRate(), spotService::parseHourlyRate);

        Spot updated = spotService.update(id, spotNumber, spotType, rate);
        System.out.println("Место обновлено:");
        printList(List.of(updated));
    }

    @Override
    protected void delete() {
        spotService.delete(input.readLong("ID места: "));
        System.out.println("Место удалено");
    }

    @Override
    protected List<MenuOption> searchOptions() {
        return List.of(
            option("По номеру", () -> printList(List.of(spotService.findByNumber(
                input.readValid("Номер места: ", spotService::parseSpotNumber))))),
            option("Свободные на период", this::searchFree));
    }

    @Override
    protected void filter() {
        SpotFilter filter = new SpotFilter();
        runFilterScreen(filter, () -> spotService.find(filter), this::printList, column -> askFilter(filter, column));
    }

    private void askFilter(SpotFilter filter, SpotFilter.Column column) {
        switch (column) {
            case NUMBER -> filter.setNumberRange(input.readRange("Номер места"));
            case TYPE -> filter.setType(input.readOption("Тип места", SpotType.values()));
            case RATE -> filter.setRateRange(input.readRange("Тариф, руб/час"));
        }
    }

    @Override
    protected void statistics() {
        long id = input.readLong("ID места: ");
        spotService.getById(id);
        SpotStatistics stats = spotService.getStatistics(id, input.readPeriod(false));

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
        printOrEmpty(spotService.findFreeDuring(period), this::printList);
    }

    private void printList(List<Spot> spots) {
        TablePrinter.print(new String[] {"ID", "Номер", "Тип", "Тариф, руб/ч"},
            spots.stream()
                .map(s -> new String[] {
                    String.valueOf(s.getId()), String.valueOf(s.getSpotNumber()),
                    s.getSpotType().getTitle(), s.getHourlyRate().toPlainString()})
                .toList());
    }
}
