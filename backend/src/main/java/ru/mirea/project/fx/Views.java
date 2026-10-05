package ru.mirea.project.fx;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.net.URL;

import javafx.fxml.FXMLLoader;

import ru.mirea.project.AppContext;

public final class Views {
    private static final String STYLESHEET = "app.css";

    private Views() {
    }

    public static FXMLLoader load(String fxmlName, AppContext context) {
        URL resource = Views.class.getResource(fxmlName);
        if (resource == null) {
            throw new IllegalStateException("Не найден файл интерфейса " + fxmlName);
        }
        FXMLLoader loader = new FXMLLoader(resource);
        loader.setControllerFactory(type -> createController(type, context));
        try {
            loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось загрузить " + fxmlName, e);
        }
        return loader;
    }

    public static String stylesheet() {
        return Views.class.getResource(STYLESHEET).toExternalForm();
    }

    private static Object createController(Class<?> type, AppContext context) {
        try {
            for (Constructor<?> constructor : type.getConstructors()) {
                Class<?>[] parameters = constructor.getParameterTypes();
                if (parameters.length == 1 && parameters[0] == AppContext.class) {
                    return constructor.newInstance(context);
                }
            }
            return type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Не удалось создать контроллер " + type.getName(), e);
        }
    }
}
