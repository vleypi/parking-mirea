package ru.mirea.parking.filter;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Set;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

public final class PageRequestFactory {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private PageRequestFactory() {
    }

    public static Pageable create(int page, int size, String sortField, String direction, Set<String> allowedSortFields) {
        if (!allowedSortFields.contains(sortField)) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "Сортировка по полю «" + sortField + "» не поддерживается. Доступно: " + allowedSortFields);
        }
        Sort.Direction dir = switch (direction.toLowerCase(Locale.ROOT)) {
            case "asc" -> Sort.Direction.ASC;
            case "desc" -> Sort.Direction.DESC;
            default -> throw new ResponseStatusException(BAD_REQUEST, "Направление сортировки должно быть asc или desc");
        };
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_SIZE);
        return PageRequest.of(safePage, safeSize, Sort.by(dir, sortField).and(Sort.by(Sort.Direction.ASC, "id")));
    }
}
