package ru.mirea.project.ui;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;

import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.model.enums.LookupValue;
import ru.mirea.project.model.value.NumberRange;
import ru.mirea.project.model.value.Period;

public class ConsoleInput {
    public static final String DATE_TIME_HINT = "дд.мм.гггг чч:мм";
    public static final String CREATE_HINT = "Введите q в любом поле, чтобы отменить";
    public static final String EDIT_HINT = "Enter: оставить текущее значение, q: отменить изменение";

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("dd.MM.uuuu HH:mm").withResolverStyle(ResolverStyle.STRICT);
    private static final String CANCEL_COMMAND = "q";

    private final Scanner scanner = new Scanner(System.in);

    public static String format(LocalDateTime value) {
        return DATE_TIME_FORMATTER.format(value);
    }

    public int readMenuChoice(String prompt) {
        while (true) {
            try {
                return Integer.parseInt(readLine(prompt).trim());
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: введите целое число");
            }
        }
    }

    public String readText(String prompt) {
        return readInput(prompt);
    }

    public long readLong(String prompt) {
        return readValid(prompt, this::parseLong);
    }

    public LocalDateTime readDateTime(String prompt) {
        return readValid(prompt, this::parseDateTime);
    }

    public LocalDateTime readDateTimeOrKeep(String prompt, LocalDateTime current) {
        return readValidOrKeep(prompt, format(current), current, this::parseDateTime);
    }

    public Period readPeriod(boolean required) {
        String hint = required ? "" : ", Enter = без ограничения";
        while (true) {
            LocalDateTime from = readValid("Начало периода (" + DATE_TIME_HINT + hint + "): ",
                value -> parseBound(value, required));
            LocalDateTime to = readValid("Конец периода (" + DATE_TIME_HINT + hint + "): ",
                value -> parseBound(value, required));
            try {
                return new Period(from, to);
            } catch (BusinessException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
    }

    public NumberRange readRange(String label) {
        while (true) {
            BigDecimal min = readValid(label + " от (Enter = без ограничения): ", this::parseOptionalDecimal);
            BigDecimal max = readValid(label + " до (Enter = без ограничения): ", this::parseOptionalDecimal);
            try {
                return new NumberRange(min, max);
            } catch (BusinessException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
    }

    public boolean readAscending() {
        return readOneOrTwo("Порядок (1 - по возрастанию, 2 - по убыванию): ");
    }

    public boolean readYesNo(String question) {
        return readOneOrTwo(question + " (1 - да, 2 - нет): ");
    }

    public <T extends LookupValue> T readOption(String title, T[] values) {
        return readOption(title, Arrays.asList(values));
    }

    public <T extends LookupValue> T readOption(String title, List<T> values) {
        printOptions(title, values);
        return readValid("Выберите номер: ", value -> parseOption(value, values));
    }

    public <T extends LookupValue> T readOptionOrKeep(String title, T current, T[] values) {
        List<T> options = Arrays.asList(values);
        printOptions(title, options);
        return readValidOrKeep("Выберите номер", current, value -> parseOption(value, options));
    }

    public <T> T readValid(String prompt, Function<String, T> check) {
        while (true) {
            try {
                return check.apply(readInput(prompt));
            } catch (BusinessException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
    }

    public <T> T readValidOrKeep(String prompt, T current, Function<String, T> check) {
        return readValidOrKeep(prompt, String.valueOf(current), current, check);
    }

    public <T> T readValidOrKeep(String prompt, String shown, T current, Function<String, T> check) {
        while (true) {
            try {
                String input = readInput(prompt + " [" + shown + "]: ");
                return input.isBlank() ? current : check.apply(input);
            } catch (BusinessException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
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

    private boolean readOneOrTwo(String prompt) {
        return readValid(prompt, value -> switch (value.trim()) {
            case "1" -> true;
            case "2" -> false;
            default -> throw new BusinessException("введите 1 или 2");
        });
    }

    private void printOptions(String title, List<? extends LookupValue> values) {
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

    private LocalDateTime parseBound(String input, boolean required) {
        if (input.isBlank()) {
            if (required) {
                throw new BusinessException("укажите дату и время");
            }
            return null;
        }
        return parseDateTime(input);
    }

    private BigDecimal parseOptionalDecimal(String input) {
        if (input.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(input.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new BusinessException("введите число");
        }
    }

    private <T extends LookupValue> T parseOption(String input, List<T> values) {
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

    public static class InputCancelledException extends RuntimeException {
    }
}
