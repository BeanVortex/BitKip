package io.beanvortex.bitkip.config.observers;


import io.beanvortex.bitkip.config.AppConfigs;
import javafx.scene.Scene;

import java.util.HashMap;
import java.util.Map;

public class ThemeSubject {

    private final Map<ThemeObserver, Scene> observers = new HashMap<>();
    private static final ThemeSubject themeSubject = new ThemeSubject();

    private ThemeSubject() {
    }

    public void addObserver(ThemeObserver o, Scene scene) {
        observers.put(o, scene);
    }

    public void removeObserver(ThemeObserver o) {
        observers.remove(o);
    }

    private void notifyAllObservers() {
        observers.forEach(ThemeObserver::updateTheme);
    }


    public static void setTheme(String theme) {
        AppConfigs.theme = theme;
        themeSubject.notifyAllObservers();
    }

    public static ThemeSubject getThemeSubject() {
        return themeSubject;
    }
}
