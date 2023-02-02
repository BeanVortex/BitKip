module ir.darkdeveloper.bitkip {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires java.logging;

    opens ir.darkdeveloper.bitkip to javafx.fxml;
    opens ir.darkdeveloper.bitkip.controllers to javafx.fxml;
    exports ir.darkdeveloper.bitkip;
}