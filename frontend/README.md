# Frontend парковочной системы

Web-клиент для сотрудников на Next.js 16. Правила кода и архитектуры: `CLAUDE.md` и `AGENTS.md` в этой папке.

## Запуск

Нужны Node.js 24 и pnpm 12. Backend должен работать на `http://localhost:8080` (адрес меняется переменной `API_URL`).

```bash
cd frontend
pnpm install
pnpm dev
```

Приложение откроется на http://localhost:3000. Запросы к `/api` проксируются на backend, поэтому cookie входа остаются на адресе приложения.

Проверка перед PR:

```bash
pnpm check
```

## Структура

```text
src/
├── app/                      # только маршруты Next: page.tsx и layout.tsx
│   ├── (workspace)/          # разделы после входа: requests, owners, vehicles, spots, analytics, references, users, audit
│   └── login/
├── proxy.ts                  # проверка сессии и редирект на /login
├── views/                    # страницы целиком
├── widgets/                  # шапка и боковое меню
├── features/                 # вход, выход, смена пароля
├── entities/                 # viewer (текущий пользователь и его роли)
└── shared/
    ├── api/                  # apiClient, ApiError, IPage, QUERY_KEYS
    ├── config/               # маршруты, меню, роли, тексты списков
    ├── lib/                  # useListParams, applyServerErrors, схемы логина и пароля
    ├── ui/                   # DataTable, ListToolbar, Pagination, Modal, поля форм, кнопки
    ├── query/                # QueryProvider
    └── proxy/                # createAuthProxy
```

## Как добавить страницу своей сущности

Пример для раздела «Владельцы». Образец готовой страницы: раздел «Заявки».

1. **Сущность.** Создай слайс `src/entities/owner/`:
   - `interfaces/owner.interface.ts`: тип `IOwner` в точности как ответ backend;
   - `services/owner.service.ts`: класс `OwnerService` с методами поверх `apiClient`, экспорт `ownerService`;
   - `hooks/useOwners.ts`: `useQuery` с ключом из `QUERY_KEYS`;
   - `index.ts`: наружу только то, что нужно другим слоям.
2. **Ключи запросов.** Добавь ключи в `src/shared/api/query-keys.ts`, например `owners: ["owners"]` и `ownerPage: (request) => ["owners", request]`.
3. **Действия.** Каждое действие пользователя это отдельный слайс в `src/features/`: `save-owner`, `archive-owner`, `import-owners`. Форма валидируется схемой zod из `schemas/*.schema.ts`, ошибки сервера раскладываются через `applyServerErrors`.
4. **Страница.** Создай `src/views/owners/` и собери страницу из `PageHeader`, `ListToolbar`, `DataTable`, `Pagination` и хука `useListParams`. Тексты интерфейса положи в `constants/owners-page.constant.ts`.
5. **Маршрут.** В `src/app/(workspace)/owners/page.tsx` замени `SectionPlaceholderPage` на свою view. Пункт меню уже есть в `NAV_ITEMS`.
6. **Проверка.** `pnpm check` без ошибок.

Свои таблицы, фильтры, модальные окна и поля форм с нуля не пишутся: если в `shared` чего-то не хватает, это добавляется туда и становится доступно всем.
