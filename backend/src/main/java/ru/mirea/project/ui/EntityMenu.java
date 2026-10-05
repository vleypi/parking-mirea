package ru.mirea.project.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.DataAccessException;
import ru.mirea.project.exception.EntityNotFoundException;
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

    protected abstract void search();

    protected abstract void filter();

    protected abstract void statistics();

    protected List<MenuOption> extraOptions() {
        return List.of();
    }

    public final void run() {
        List<MenuOption> options = new ArrayList<>(List.of(
            option("Показать все", this::showAll),
            option("Найти по ID", this::findById),
            option("Создать", this::create),
            option("Изменить", this::update),
            option("Удалить", this::delete),
            option("Поиск", this::search),
            option("Фильтры и сортировка", this::filter),
            option("Статистика", this::statistics)));
        options.addAll(extraOptions());
        runSubmenu(title(), options);
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
