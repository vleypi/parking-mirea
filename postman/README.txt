Postman-коллекция API парковки (КР3): 122 запроса с автотестами.

1. Запустить backend: .\backend\start-compose.ps1
   Для папки 9 (окончательное удаление) в .env должно быть PERMANENT_DELETE_ENABLED=true.
2. В Postman перетащить два файла:
   - parking-mirea.postman_collection.json
   - parking-local.postman_environment.json
3. Environments -> «Parking local» -> adminPassword = значение BOOTSTRAP_ADMIN_PASSWORD из своего .env.
   Справа вверху выбрать окружение «Parking local».
4. Settings -> General -> Working directory = эта папка (postman), чтобы запросы импорта нашли файлы data\*.csv.
5. Коллекция -> Run -> Run Parking MIREA. Папки идти по порядку, все тесты должны быть зелёными.

Без Postman то же самое из консоли:
npx newman run parking-mirea.postman_collection.json -e parking-local.postman_environment.json --env-var adminPassword=<пароль> --working-dir .
