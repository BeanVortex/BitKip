package ir.darkdeveloper.bitkip.config.observers;

import javafx.scene.Scene;

import static ir.darkdeveloper.bitkip.BitKip.getResource;
import static ir.darkdeveloper.bitkip.config.AppConfigs.theme;

public interface ThemeObserver {
    default void updateTheme(Scene scene) {
        updateThemeNotObserved(scene);
    }

    static void updateThemeNotObserved(Scene scene){
        var stylesheets = scene.getStylesheets();
        if (!stylesheets.contains(getResource("css/light_mode.css").toExternalForm()))
            stylesheets.add(getResource("css/light_mode.css").toExternalForm());

        if (theme.equals("light"))
            stylesheets.remove(getResource("css/dark_mode.css").toExternalForm());
        else
            stylesheets.add(getResource("css/dark_mode.css").toExternalForm());
    }
}
