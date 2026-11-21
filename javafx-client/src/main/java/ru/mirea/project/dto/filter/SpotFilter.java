package ru.mirea.project.dto.filter;

import ru.mirea.project.model.enums.LookupValue;
import ru.mirea.project.model.enums.SpotType;
import ru.mirea.project.model.value.NumberRange;

public class SpotFilter extends EntityFilter<SpotFilter.Column, SpotFilter.SortField> {

    public SpotFilter() {
        super(SortField.ID);
    }

    @Override
    public Column[] columns() {
        return Column.values();
    }

    @Override
    public SortField[] sortFields() {
        return SortField.values();
    }

    public void setNumberRange(NumberRange range) {
        if (range.isUnbounded()) {
            remove(Column.NUMBER);
            return;
        }
        set(Column.NUMBER, range, "Номер: " + range);
    }

    public void setType(SpotType type) {
        set(Column.TYPE, type, "Тип = " + type.getTitle());
    }

    public void setRateRange(NumberRange range) {
        if (range.isUnbounded()) {
            remove(Column.RATE);
            return;
        }
        set(Column.RATE, range, "Тариф: " + range + " руб/ч");
    }

    public NumberRange getNumberRange() {
        return get(Column.NUMBER, NumberRange.class);
    }

    public SpotType getType() {
        return get(Column.TYPE, SpotType.class);
    }

    public NumberRange getRateRange() {
        return get(Column.RATE, NumberRange.class);
    }

    public enum Column implements LookupValue {
        NUMBER(1, "Номер"),
        TYPE(2, "Тип"),
        RATE(3, "Тариф");

        private final int id;
        private final String title;

        Column(int id, String title) {
            this.id = id;
            this.title = title;
        }

        @Override
        public int getId() {
            return id;
        }

        @Override
        public String getTitle() {
            return title;
        }
    }

    public enum SortField implements LookupValue {
        ID(1, "ID"),
        NUMBER(2, "Номер"),
        TYPE(3, "Тип"),
        RATE(4, "Тариф");

        private final int id;
        private final String title;

        SortField(int id, String title) {
            this.id = id;
            this.title = title;
        }

        @Override
        public int getId() {
            return id;
        }

        @Override
        public String getTitle() {
            return title;
        }
    }
}
