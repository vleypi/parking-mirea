package ru.mirea.parking.csv;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Одна строка CSV: номер строки в файле и значения по заголовкам, с удобным разбором типов. */
public record CsvRow(int line, Map<String, String> values) {

    private static final Set<String> TRUE = Set.of("да", "true", "1", "yes", "+");
    private static final Set<String> FALSE = Set.of("нет", "false", "0", "no", "-", "");

    /** Значение столбца без пробелов по краям; пустая строка, если столбца нет. */
    public String get(String header) {
        String value = values.get(header.toLowerCase(Locale.ROOT));
        return value == null ? "" : value.trim();
    }

    public String required(String header) {
        String value = get(header);
        if (value.isEmpty()) {
            throw new CsvRowException("Не заполнен столбец «" + header + "»");
        }
        return value;
    }

    public String optional(String header) {
        String value = get(header);
        return value.isEmpty() ? null : value;
    }

    public int requiredInt(String header) {
        String value = required(header);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new CsvRowException("Столбец «" + header + "»: «" + value + "» не целое число");
        }
    }

    public Integer optionalInt(String header) {
        return get(header).isEmpty() ? null : requiredInt(header);
    }

    /** Десятичное число; допускается запятая как в русском Excel. */
    public BigDecimal requiredDecimal(String header) {
        String value = required(header).replace(" ", "").replace(',', '.');
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new CsvRowException("Столбец «" + header + "»: «" + value + "» не число");
        }
    }

    /** да/нет, true/false, 1/0; пустое значение — false. */
    public boolean bool(String header) {
        String value = get(header).toLowerCase(Locale.ROOT);
        if (TRUE.contains(value)) {
            return true;
        }
        if (FALSE.contains(value)) {
            return false;
        }
        throw new CsvRowException("Столбец «" + header + "»: ожидается да или нет, получено «" + get(header) + "»");
    }

    /** Значение перечисления по имени константы или по русскому названию. */
    public <E extends Enum<E>> E enumValue(String header, Class<E> type, java.util.function.Function<E, String> title) {
        String value = required(header);
        for (E constant : type.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(value) || title.apply(constant).equalsIgnoreCase(value)) {
                return constant;
            }
        }
        throw new CsvRowException("Столбец «" + header + "»: неизвестное значение «" + value + "»");
    }
}
