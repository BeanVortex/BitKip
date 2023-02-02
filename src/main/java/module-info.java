module ir.darkdeveloper.bitkip {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;

    opens ir.darkdeveloper.bitkip to javafx.fxml;
    exports ir.darkdeveloper.bitkip;
}