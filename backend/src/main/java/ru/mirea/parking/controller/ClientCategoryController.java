package ru.mirea.parking.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.mirea.parking.dto.reference.ClientCategoryResponse;
import ru.mirea.parking.repository.ClientCategoryRepository;

import java.util.List;

/**
 * Справочник категорий клиентов для выпадающих списков формы и фильтров.
 * Создание, изменение и отключение категорий администратором добавятся по образцу общего /api/references.
 */
@Tag(name = "references")
@RestController
@RequestMapping("/api/references/client-categories")
public class ClientCategoryController {

    private final ClientCategoryRepository categories;

    public ClientCategoryController(ClientCategoryRepository categories) {
        this.categories = categories;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<ClientCategoryResponse> list() {
        return categories.findAllByOrderByIdAsc().stream().map(ClientCategoryResponse::from).toList();
    }
}
