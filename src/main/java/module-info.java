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
    requires static lombok;
    requires java.sql;
    requires org.xerial.sqlitejdbc;
    requires org.apache.commons.lang3;
    requires reactor.core;
    requires org.jsoup;
    requires java.desktop;
    requires javafx.swing;
    requires com.sun.jna;
    requires org.eclipse.jetty.servlet;
    requires org.eclipse.jetty.server;
    requires org.eclipse.jetty.util;
    requires org.slf4j;
    requires com.fasterxml.jackson.databind;


    opens ir.darkdeveloper.bitkip to javafx.fxml, javafx.controls;
    opens ir.darkdeveloper.bitkip.controllers to javafx.fxml, javafx.base, org.eclipse.jetty.servlet, org.eclipse.jetty.server;
    opens ir.darkdeveloper.bitkip.models to javafx.fxml, javafx.base, com.fasterxml.jackson.databind;
    opens ir.darkdeveloper.bitkip.controllers.interfaces to javafx.base, javafx.fxml;
    exports ir.darkdeveloper.bitkip;
    opens ir.darkdeveloper.bitkip.servlets to javafx.base, javafx.fxml, org.eclipse.jetty.server, org.eclipse.jetty.servlet;
}