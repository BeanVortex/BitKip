package io.beanvortex.bitkip.config.observers;

import io.beanvortex.bitkip.BitKip;
import javafx.scene.Scene;

import static io.beanvortex.bitkip.config.AppConfigs.theme;

public interface ThemeObserver {
    default void updateTheme(Scene scene) {
        updateThemeNotObserved(scene);
    }

    static void updateThemeNotObserved(Scene scene){
        var stylesheets = scene.getStylesheets();
        if (!stylesheets.contains(BitKip.getResource("css/light_mode.css").toExternalForm()))
            stylesheets.add(BitKip.getResource("css/light_mode.css").toExternalForm());

        if (theme.equals("light"))
            stylesheets.remove(BitKip.getResource("css/dark_mode.css").toExternalForm());
        else
            stylesheets.add(BitKip.getResource("css/dark_mode.css").toExternalForm());
    }
}
