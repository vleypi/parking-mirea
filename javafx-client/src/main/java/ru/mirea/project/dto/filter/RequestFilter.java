package ru.mirea.project.dto.filter;

import ru.mirea.project.model.enums.LookupValue;
import ru.mirea.project.model.enums.RequestStatus;
import ru.mirea.project.model.enums.SpotType;
import ru.mirea.project.model.value.Period;
import ru.mirea.project.util.InputFormats;

public class RequestFilter extends EntityFilter<RequestFilter.Column, RequestFilter.SortField> {

    public RequestFilter() {
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

    public void setOwnerNameContains(String fragment) {
        String name = InputFormats.requireText(fragment, "Введите фрагмент имени владельца");
        set(Column.OWNER, name, "Владелец содержит «" + name + "»");
    }

    public void setPlateContains(String fragment) {
        String plate = InputFormats.normalizePlateText(InputFormats.requireText(fragment, "Введите фрагмент гос. номера"));
        set(Column.PLATE, plate, "Автомобиль содержит «" + plate + "»");
    }

    public void setSpotNumber(int spotNumber) {
        set(Column.SPOT, spotNumber, "Место = " + spotNumber);
    }

    public void setSpotType(SpotType spotType) {
        set(Column.SPOT_TYPE, spotType, "Тип места = " + spotType.getTitle());
    }

    public void setPeriod(Period period) {
        if (period.isAllTime()) {
            remove(Column.PERIOD);
            return;
        }
        set(Column.PERIOD, period, "Период " + period);
    }

    public void setStatus(RequestStatus status) {
        set(Column.STATUS, status, "Статус = " + status.getTitle());
    }

    public String getOwnerNameContains() {
        return get(Column.OWNER, String.class);
    }

    public String getPlateContains() {
        return get(Column.PLATE, String.class);
    }

    public Integer getSpotNumber() {
        return get(Column.SPOT, Integer.class);
    }

    public SpotType getSpotType() {
        return get(Column.SPOT_TYPE, SpotType.class);
    }

    public Period getPeriod() {
        return get(Column.PERIOD, Period.class);
    }

    public RequestStatus getStatus() {
        return get(Column.STATUS, RequestStatus.class);
    }

    public enum Column implements LookupValue {
        OWNER(1, "Владелец"),
        PLATE(2, "Автомобиль"),
        SPOT(3, "Место"),
        SPOT_TYPE(4, "Тип места"),
        PERIOD(5, "Период"),
        STATUS(6, "Статус");

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
        OWNER(2, "Владелец"),
        PLATE(3, "Автомобиль"),
        SPOT(4, "Место"),
        START(5, "Начало"),
        END(6, "Окончание"),
        STATUS(7, "Статус"),
        CREATED(8, "Дата создания");

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
