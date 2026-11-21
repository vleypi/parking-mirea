package ru.mirea.parking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Точка входа REST-сервера парковочной системы.
 *
 * <p>{@code @SpringBootApplication} включает автоконфигурацию и поиск компонентов
 * (контроллеров, сервисов, репозиториев) во всех подпакетах {@code ru.mirea.parking}.</p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ParkingApplication {

    public static void main(String[] args) {
        SpringApplication.run(ParkingApplication.class, args);
    }
}
