module ir.darkdeveloper.bitkip {

    requires javafx.controls;
    requires javafx.fxml;
    requires java.base;
    requires jdk.crypto.cryptoki;
    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    //dynamic load in jlink image
    requires org.kordamp.ikonli.fontawesome5;
    requires org.kordamp.bootstrapfx.core;
    requires java.logging;
    requires lombok;
    requires java.sql;
    requires org.xerial.sqlitejdbc;
    requires org.apache.commons.lang3;
    requires org.junit.jupiter.api;
    requires org.junit.platform.commons;
    requires reactor.core;
    requires org.jsoup;
    requires java.desktop;
    requires com.dustinredmond.fxtrayicon;
    requires javafx.swing;

    opens ir.darkdeveloper.bitkip to javafx.fxml, javafx.controls;
    opens ir.darkdeveloper.bitkip.controllers to javafx.fxml, javafx.base;
    opens ir.darkdeveloper.bitkip.models to javafx.fxml, javafx.base;
    opens ir.darkdeveloper.bitkip.controllers.interfaces to javafx.base, javafx.fxml;
    exports ir.darkdeveloper.bitkip;
}