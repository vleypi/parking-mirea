package ru.mirea.project.dto.filter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ru.mirea.project.model.enums.LookupValue;

public abstract class EntityFilter<C extends LookupValue, S extends LookupValue> {
    private final Map<C, Object> values = new LinkedHashMap<>();
    private final Map<C, String> descriptions = new LinkedHashMap<>();
    private S sortField;
    private boolean ascending = true;

    protected EntityFilter(S defaultSortField) {
        this.sortField = defaultSortField;
    }

    public abstract C[] columns();

    public abstract S[] sortFields();

    public List<C> activeColumns() {
        return new ArrayList<>(values.keySet());
    }

    public List<String> describe() {
        return new ArrayList<>(descriptions.values());
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public void remove(C column) {
        values.remove(column);
        descriptions.remove(column);
    }

    public void clear() {
        values.clear();
        descriptions.clear();
    }

    public S getSortField() {
        return sortField;
    }

    public boolean isAscending() {
        return ascending;
    }

    public void setSort(S sortField, boolean ascending) {
        this.sortField = sortField;
        this.ascending = ascending;
    }

    public String describeSort() {
        return sortField.getTitle() + (ascending ? ", по возрастанию" : ", по убыванию");
    }

    protected void set(C column, Object value, String description) {
        values.put(column, value);
        descriptions.put(column, description);
    }

    protected <T> T get(C column, Class<T> type) {
        return type.cast(values.get(column));
    }
}
