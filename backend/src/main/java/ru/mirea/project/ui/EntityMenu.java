package ru.mirea.project.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import ru.mirea.project.dto.filter.EntityFilter;
import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.DataAccessException;
import ru.mirea.project.exception.EntityNotFoundException;
import ru.mirea.project.model.enums.LookupValue;
import ru.mirea.project.model.enums.RequestStatus;

public abstract class EntityMenu {
    protected final ConsoleInput input;

    protected EntityMenu(ConsoleInput input) {
        this.input = input;
    }

    public abstract String title();

    protected abstract void showAll();

    protected abstract void findById();

    protected abstract void create();

    protected abstract void update();

    protected abstract void delete();

    protected abstract List<MenuOption> searchOptions();

    protected abstract void filter();

    protected abstract void statistics();

    protected List<MenuOption> extraOptions() {
        return List.of();
    }

    public final void run() {
        List<MenuOption> options = new ArrayList<>(List.of(
            option("Показать все", this::showAll),
            option("Создать", this::create),
            option("Изменить", this::update),
            option("Удалить", this::delete),
            option("Поиск", this::search),
            option("Фильтры и сортировка", this::filter),
            option("Статистика", this::statistics)));
        options.addAll(extraOptions());
        runSubmenu(title(), options);
    }

    private void search() {
        List<MenuOption> options = new ArrayList<>();
        options.add(option("По ID", this::findById));
        options.addAll(searchOptions());
        runSubmenu(title() + ": поиск", options);
    }

    protected <T, C extends LookupValue, S extends LookupValue> void runFilterScreen(
            EntityFilter<C, S> filter, Supplier<List<T>> query, Consumer<List<T>> printer, Consumer<C> askValue) {
        while (true) {
            System.out.println();
            System.out.println(title() + ": фильтры и сортировка");
            System.out.println("Фильтры: " + (filter.isEmpty() ? "нет" : String.join("; ", filter.describe())));
            System.out.println("Сортировка: " + filter.describeSort());
            List<T> items = query.get();
            System.out.println("Найдено: " + items.size());
            printOrEmpty(items, printer);
            System.out.println("1. Добавить фильтр");
            System.out.println("2. Убрать фильтр");
            System.out.println("3. Сбросить все фильтры");
            System.out.println("4. Сортировка");
            System.out.println("0. Назад");
            switch (input.readMenuChoice("Выберите действие: ")) {
                case 0 -> {
                    return;
                }
                case 1 -> perform(() -> askValue.accept(input.readOption("Фильтр по столбцу", filter.columns())));
                case 2 -> perform(() -> removeFilter(filter));
                case 3 -> filter.clear();
                case 4 -> perform(() -> filter.setSort(
                    input.readOption("Сортировать по столбцу", filter.sortFields()), input.readAscending()));
                default -> System.out.println("Неизвестный пункт меню");
            }
        }
    }

    protected void ask(String prompt, Consumer<String> setter) {
        input.readValid(prompt, value -> {
            setter.accept(value);
            return value;
        });
    }

    private <C extends LookupValue> void removeFilter(EntityFilter<C, ?> filter) {
        if (filter.isEmpty()) {
            System.out.println("Активных фильтров нет");
            return;
        }
        filter.remove(input.readOption("Убрать фильтр", filter.activeColumns()));
    }

    protected void runSubmenu(String title, MenuOption... options) {
        runSubmenu(title, List.of(options));
    }

    protected void runSubmenu(String title, List<MenuOption> options) {
        while (true) {
            System.out.println();
            System.out.println(title);
            for (int i = 0; i < options.size(); i++) {
                System.out.println((i + 1) + ". " + options.get(i).label());
            }
            System.out.println("0. Назад");
            int choice = input.readMenuChoice("Выберите действие: ");
            if (choice == 0) {
                return;
            }
            if (choice < 0 || choice > options.size()) {
                System.out.println("Неизвестный пункт меню");
                continue;
            }
            perform(options.get(choice - 1).action());
        }
    }

    static void perform(Runnable action) {
        try {
            action.run();
        } catch (BusinessException | EntityNotFoundException | DataAccessException e) {
            System.out.println("Ошибка: " + e.getMessage());
        } catch (ConsoleInput.InputCancelledException e) {
            System.out.println("Действие отменено");
        }
    }

    protected <T> void printOrEmpty(List<T> items, Consumer<List<T>> printer) {
        if (items.isEmpty()) {
            System.out.println("Ничего не найдено");
            return;
        }
        printer.accept(items);
    }

    protected static void printStatusCounts(Map<RequestStatus, Long> counts) {
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        System.out.println("Заявок: " + total);
        counts.forEach((status, count) -> System.out.println("  " + status.getTitle() + ": " + count));
    }

    protected static MenuOption option(String label, Runnable action) {
        return new MenuOption(label, action);
    }

    protected record MenuOption(String label, Runnable action) {
    }
}
