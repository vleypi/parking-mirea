package ru.mirea.project.dto.filter;

import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.model.enums.LookupValue;
import ru.mirea.project.util.InputFormats;

public class VehicleFilter extends EntityFilter<VehicleFilter.Column, VehicleFilter.SortField> {

    public VehicleFilter() {
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

    public void setPlateContains(String fragment) {
        String plate = InputFormats.normalizePlateText(InputFormats.requireText(fragment, "Введите фрагмент гос. номера"));
        set(Column.PLATE, plate, "Гос. номер содержит «" + plate + "»");
    }

    public void setRegion(String region) {
        String value = InputFormats.requireText(region, "Введите регион");
        if (!value.matches("\\d{2,3}")) {
            throw new BusinessException("Регион состоит из 2 или 3 цифр, например 56 или 777");
        }
        set(Column.REGION, value, "Регион = " + value);
    }

    public void setBrandContains(String fragment) {
        String brand = InputFormats.requireText(fragment, "Введите фрагмент марки");
        set(Column.BRAND, brand, "Марка содержит «" + brand + "»");
    }

    public void setModelContains(String fragment) {
        String model = InputFormats.requireText(fragment, "Введите фрагмент модели");
        set(Column.MODEL, model, "Модель содержит «" + model + "»");
    }

    public void setOwnerNameContains(String fragment) {
        String name = InputFormats.requireText(fragment, "Введите фрагмент имени владельца");
        set(Column.OWNER, name, "Владелец содержит «" + name + "»");
    }

    public String getPlateContains() {
        return get(Column.PLATE, String.class);
    }

    public String getRegion() {
        return get(Column.REGION, String.class);
    }

    public String getBrandContains() {
        return get(Column.BRAND, String.class);
    }

    public String getModelContains() {
        return get(Column.MODEL, String.class);
    }

    public String getOwnerNameContains() {
        return get(Column.OWNER, String.class);
    }

    public enum Column implements LookupValue {
        PLATE(1, "Гос. номер"),
        REGION(2, "Регион"),
        BRAND(3, "Марка"),
        MODEL(4, "Модель"),
        OWNER(5, "Владелец");

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
        PLATE(2, "Гос. номер"),
        REGION(3, "Регион"),
        BRAND(4, "Марка"),
        MODEL(5, "Модель"),
        OWNER(6, "Владелец");

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
