package ru.mirea.parking.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.mirea.parking.dto.PageResponse;
import ru.mirea.parking.dto.user.TemporaryPasswordResponse;
import ru.mirea.parking.dto.user.UserAccountResponse;
import ru.mirea.parking.dto.user.UserCreateRequest;
import ru.mirea.parking.dto.user.UserUpdateRequest;
import ru.mirea.parking.security.UserPrincipal;
import ru.mirea.parking.service.UserAdminService;

import java.util.UUID;

@Tag(name = "admin users", description = "Учётные записи сотрудников")
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserAdminService userAdminService;

    public AdminUserController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @Operation(summary = "Учётные записи сотрудников с поиском (Администратор)")
    @GetMapping
    public PageResponse<UserAccountResponse> search(@RequestParam(required = false) String q,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size,
                                                    @RequestParam(defaultValue = "createdAt") String sort,
                                                    @RequestParam(defaultValue = "desc") String direction) {
        return userAdminService.search(q, page, size, sort, direction);
    }

    @Operation(summary = "Создать учётную запись с временным паролем")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TemporaryPasswordResponse create(@Valid @RequestBody UserCreateRequest body) {
        return userAdminService.create(body);
    }

    @Operation(summary = "Изменить логин, имя, роли или блокировку учётной записи")
    @PutMapping("/{id}")
    public UserAccountResponse update(@PathVariable UUID id, @Valid @RequestBody UserUpdateRequest body,
                                      @AuthenticationPrincipal UserPrincipal admin) {
        return userAdminService.update(id, body, admin);
    }

    @Operation(summary = "Сбросить пароль: выдаётся новый временный")
    @PostMapping("/{id}/reset-password")
    public TemporaryPasswordResponse resetPassword(@PathVariable UUID id) {
        return userAdminService.resetPassword(id);
    }
}
