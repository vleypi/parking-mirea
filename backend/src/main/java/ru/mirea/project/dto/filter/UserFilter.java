package ru.mirea.project.dto.filter;

import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.model.enums.LookupValue;
import ru.mirea.project.model.value.NumberRange;
import ru.mirea.project.model.value.Period;
import ru.mirea.project.util.InputFormats;

public class UserFilter extends EntityFilter<UserFilter.Column, UserFilter.SortField> {

    public UserFilter() {
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

    public void setNameContains(String fragment) {
        String name = InputFormats.requireText(fragment, "Введите фрагмент имени");
        set(Column.NAME, name, "Имя содержит «" + name + "»");
    }

    public void setPhoneDigits(String fragment) {
        String digits = fragment == null ? "" : fragment.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            throw new BusinessException("Введите цифры телефона");
        }
        set(Column.PHONE, digits, "Телефон содержит " + digits);
    }

    public void setVehicleCount(NumberRange range) {
        if (range.isUnbounded()) {
            remove(Column.VEHICLES);
            return;
        }
        set(Column.VEHICLES, range, "Машин: " + range);
    }

    public void setRegistered(Period period) {
        if (period.isAllTime()) {
            remove(Column.REGISTERED);
            return;
        }
        set(Column.REGISTERED, period, "Зарегистрирован " + period);
    }

    public void setHasActiveRequests(boolean hasActiveRequests) {
        set(Column.ACTIVE_REQUESTS, hasActiveRequests,
            hasActiveRequests ? "Есть активные заявки" : "Нет активных заявок");
    }

    public String getNameContains() {
        return get(Column.NAME, String.class);
    }

    public String getPhoneDigits() {
        return get(Column.PHONE, String.class);
    }

    public NumberRange getVehicleCount() {
        return get(Column.VEHICLES, NumberRange.class);
    }

    public Period getRegistered() {
        return get(Column.REGISTERED, Period.class);
    }

    public Boolean getHasActiveRequests() {
        return get(Column.ACTIVE_REQUESTS, Boolean.class);
    }

    public enum Column implements LookupValue {
        NAME(1, "Имя"),
        PHONE(2, "Телефон"),
        VEHICLES(3, "Количество машин"),
        REGISTERED(4, "Дата регистрации"),
        ACTIVE_REQUESTS(5, "Активные заявки");

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
        NAME(2, "Имя"),
        PHONE(3, "Телефон"),
        VEHICLES(4, "Количество машин"),
        REGISTERED(5, "Дата регистрации");

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
