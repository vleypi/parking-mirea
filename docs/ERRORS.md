# Ошибки REST API

Любая ошибка backend возвращается в одном формате — `ErrorResponse` из пакета `ru.mirea.parking.error`.

```json
{
  "timestamp": "2026-11-20T10:15:30Z",
  "status": 400,
  "error": "VALIDATION_FAILED",
  "message": "Проверьте поля: hourlyRate",
  "path": "/api/spots",
  "correlationId": "6f1c2a7e-...",
  "fieldErrors": [{ "field": "hourlyRate", "message": "Тариф от 0 до 99999.99" }]
}
```

| Поле | Что значит |
|---|---|
| `status` | HTTP-код |
| `error` | машинный код: `VALIDATION_FAILED`, `BAD_REQUEST`, `UNAUTHORIZED`, `FORBIDDEN`, `NOT_FOUND`, `CONFLICT`, `STALE_VERSION`, `DATA_CONFLICT`, `INTERNAL_ERROR` |
| `message` | текст для человека, его показывает web-клиент |
| `correlationId` | id запроса из заголовка `X-Correlation-Id`; по нему ищем строку в логе сервера и событие в аудите |
| `fieldErrors` | ошибки по полям формы, web-клиент подсвечивает эти поля |

Stack trace, текст SQL, имена ограничений базы и классов в ответ не попадают — только в лог сервера.

## Какое исключение бросать

| Ситуация | Что делать | Код |
|---|---|---|
| Неверные поля тела запроса (пусто, длина, диапазон, формат) | аннотации Bean Validation в DTO + `@Valid` в контроллере, бросать ничего не нужно | 400 |
| Неверный параметр, который не проверить аннотацией (from позже to, отключённый тип) | `throw new BadRequestException("поле", "текст")` | 400 |
| Не выполнен вход | ничего — Spring Security + `JsonSecurityErrorHandler` | 401 |
| Роль ниже нужной | ничего — `@PreAuthorize("hasRole('OPERATOR')")` | 403 |
| Запрет по правилу (чужая заметка, удаление выключено настройкой) | `throw new ForbiddenException("текст")` | 403 |
| Нет записи с таким UUID | `throw new NotFoundException("Место ... не найдено")` | 404 |
| Бизнес-правило: номер занят, запись в архиве, есть связанные заявки | `throw new ConflictException("текст")` | 409 |
| Устаревшая `version` | `ConflictException` при сравнении version или ничего — `ObjectOptimisticLockingFailureException` обработается сам | 409 |
| Гонка при вставке дубликата | ничего — `DataIntegrityViolationException` превратится в 409 | 409 |
| Всё непредвиденное | ничего — 500 с общим текстом, подробности в логе | 500 |

Ответ вручную (`ResponseEntity.status(...).body(...)`) собирать не нужно. Старый код с
`throw new ResponseStatusException(HttpStatus.CONFLICT, "...")` тоже работает и отдаёт тот же формат:
все наши исключения наследуются от `ResponseStatusException`, поэтому `catch (ResponseStatusException e)`
в массовых операциях ловит и их.

## Где это в коде

| Класс | Роль |
|---|---|
| `GlobalExceptionHandler` | `@RestControllerAdvice`: перехватывает исключения контроллеров и сервисов |
| `JsonErrorController` | `/error`: ошибки, которые случились вне контроллеров (фильтры, Tomcat) |
| `JsonSecurityErrorHandler` (пакет security) | 401 и 403 от Spring Security, до контроллера запрос не дошёл |
| `ErrorCodes` | машинные коды и сообщения по умолчанию |

Проверка: `ErrorHandlingTest` воспроизводит каждый код и сверяет, что JSON одинаковый и без stack trace.
