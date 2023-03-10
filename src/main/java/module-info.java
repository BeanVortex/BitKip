module ir.darkdeveloper.bitkip {

    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires java.logging;
    requires lombok;
    requires java.sql;
    requires org.xerial.sqlitejdbc;
    requires org.apache.commons.lang3;
    requires org.junit.jupiter.api;
    requires org.junit.platform.commons;
    requires reactor.core;

    opens ir.darkdeveloper.bitkip to javafx.fxml;
    opens ir.darkdeveloper.bitkip.controllers to javafx.fxml, javafx.base;
    opens ir.darkdeveloper.bitkip.models to javafx.fxml, javafx.base;
    exports ir.darkdeveloper.bitkip;
    opens ir.darkdeveloper.bitkip.controllers.interfaces to javafx.base, javafx.fxml;
}