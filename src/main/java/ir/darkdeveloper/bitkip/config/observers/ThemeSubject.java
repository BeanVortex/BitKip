package ir.darkdeveloper.bitkip.config.observers;


import ir.darkdeveloper.bitkip.config.AppConfigs;

import java.util.ArrayList;
import java.util.List;

public class ThemeSubject {

    private final List<ThemeObserver> observers = new ArrayList<>();
    private static final ThemeSubject themeSubject = new ThemeSubject();

    private ThemeSubject() {
    }

    private void addObserver(ThemeObserver o) {
        observers.add(o);
    }

    private void removeObserver(ThemeObserver o) {
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
