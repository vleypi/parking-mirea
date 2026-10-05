package ru.mirea.project;

import java.util.List;

import ru.mirea.project.ui.ConsoleInput;
import ru.mirea.project.ui.EntityMenu;
import ru.mirea.project.ui.MainMenu;
import ru.mirea.project.ui.RequestMenu;
import ru.mirea.project.ui.SpotMenu;
import ru.mirea.project.ui.UserMenu;
import ru.mirea.project.ui.VehicleMenu;

public class Main {
    public static void main(String[] args) {
        AppContext context = new AppContext();

        ConsoleInput input = new ConsoleInput();
        List<EntityMenu> menus = List.of(
            new UserMenu(input, context.getUserService(), context.getVehicleService()),
            new VehicleMenu(input, context.getVehicleService(), context.getUserService()),
            new SpotMenu(input, context.getSpotService()),
            new RequestMenu(input, context.getRequestService(), context.getUserService(),
                context.getVehicleService(), context.getSpotService()));
        new MainMenu(input, menus, context.getStatisticsService(), context.getUserService(),
            context.getVehicleService(), context.getSpotService(), context.getRequestService()).run();
    }
}
