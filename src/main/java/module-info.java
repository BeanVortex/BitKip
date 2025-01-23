module io.beanvortex.bitkip {

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
    requires static lombok;
    requires java.sql;
    requires org.xerial.sqlitejdbc;
    requires org.apache.commons.lang3;
    requires reactor.core;
    requires org.jsoup;
    requires java.desktop;
    requires javafx.swing;
    requires com.sun.jna;
    requires com.sun.jna.platform;
    requires org.slf4j;
    requires ch.qos.logback.classic;
    requires com.fasterxml.jackson.databind;
    requires io.helidon.webserver;
    requires io.helidon.webserver.cors;
    requires io.helidon.media.jackson;
    requires java.prefs;
    requires com.zaxxer.hikari;

    opens io.beanvortex.bitkip to javafx.fxml, javafx.controls;
    opens io.beanvortex.bitkip.controllers to javafx.fxml, javafx.base;
    opens io.beanvortex.bitkip.models to javafx.fxml, javafx.base, com.fasterxml.jackson.databind;
    opens io.beanvortex.bitkip.controllers.interfaces to javafx.base, javafx.fxml;
    opens io.beanvortex.bitkip.api to javafx.base, javafx.fxml, com.fasterxml.jackson.databind;
    exports io.beanvortex.bitkip;
}