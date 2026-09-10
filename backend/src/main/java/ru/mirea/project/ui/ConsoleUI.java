package ru.mirea.project.ui;

import java.util.Scanner;

import ru.mirea.project.service.ParkingRequestService;
import ru.mirea.project.service.UserService;

public class ConsoleUI {
    private final Scanner scanner = new Scanner(System.in);
    private final UserService userService;
    private final ParkingRequestService parkingRequestService;

    public ConsoleUI(UserService userService, ParkingRequestService parkingRequestService) {
        this.userService = userService;
        this.parkingRequestService = parkingRequestService;
    }

    public void run() {
        while (true) {
            System.out.println();
            System.out.println("========== ПАРКОВОЧНАЯ СИСТЕМА ==========");
            System.out.println("1. Владельцы автомобилей");
            System.out.println("2. Заявки на парковку");
            System.out.println("3. Поиск");
            System.out.println("4. Фильтрация");
            System.out.println("5. Статистика");
            System.out.println("6. Экспорт данных");
            System.out.println("7. Вывести таблицы базы данных");
            System.out.println("0. Выход");
            int choice = readInt("Выберите действие: ");
            switch (choice) {
                case 1, 2, 3, 4, 5, 6, 7 -> System.out.println("Пока не реализовано");
                case 0 -> {
                    System.out.println("До свидания!");
                    return;
                }
                default -> System.out.println("Неизвестный пункт меню");
            }
        }
    }

    private String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }

    private int readInt(String prompt) {
        while (true) {
            try {
                return Integer.parseInt(readLine(prompt).trim());
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: введите целое число");
            }
        }
    }
}
