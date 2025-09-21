package io.beanvortex.bitkip;

import io.beanvortex.bitkip.api.BatchService;
import io.beanvortex.bitkip.api.SingleService;
import io.beanvortex.bitkip.api.SyncService;
import io.beanvortex.bitkip.exceptions.GlobalExceptionHandler;
import io.beanvortex.bitkip.repo.QueuesRepo;
import io.beanvortex.bitkip.repo.ScheduleRepo;
import io.beanvortex.bitkip.task.ScheduleTask;
import io.beanvortex.bitkip.utils.FxUtils;
import io.beanvortex.bitkip.utils.IOUtils;
import io.beanvortex.bitkip.utils.MoreUtils;
import io.helidon.media.jackson.JacksonSupport;
import io.helidon.webserver.Routing;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.cors.CorsSupport;
import io.helidon.webserver.cors.CrossOriginConfig;
import io.beanvortex.bitkip.config.AppConfigs;
import io.beanvortex.bitkip.config.observers.QueueSubject;
import io.beanvortex.bitkip.exceptions.DeniedException;
import io.beanvortex.bitkip.repo.DownloadsRepo;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;


import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.awt.*;
import java.awt.event.ActionListener;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

import static io.beanvortex.bitkip.config.AppConfigs.*;
import static io.beanvortex.bitkip.controllers.SettingsController.*;

public class BitKip extends Application {

    private static WebServer server;

    @Override
    public void start(Stage stage) {
        IOUtils.readConfig();
        AppConfigs.initPaths();
        IOUtils.createSaveLocations();
        DownloadsRepo.createTable();
        ScheduleRepo.createSchedulesTable();
        QueuesRepo.createTable();
        var queues = QueuesRepo.getAllQueues(false, true);
        if (queues.isEmpty())
            queues = QueuesRepo.createDefaultRecords();
        queues = ScheduleRepo.createDefaultSchedulesForQueues(queues);
        QueueSubject.addAllQueues(queues);
        IOUtils.createFoldersForQueue();
        hostServices = getHostServices();
        FxUtils.startMainStage(stage);
        ScheduleTask.scheduleQueues();
        MoreUtils.checkUpdates(false);
        IOUtils.moveChunkFilesToTemp(downloadPath);
        initStartup();
        initTray(stage);
        startServer();
        trustAllConnections();
    }

    private static void trustAllConnections() {
        if (trustAllServers) {
            try {
                TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            public X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[0];
                            }
                            public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                        }
                };
                SSLContext sc = SSLContext.getInstance("TLS");
                sc.init(null, trustAllCerts, new SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

                HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void initStartup() {
        try {
            if (!startup && existsOnStartup())
                removeFromStartup();
            if (startup && !existsOnStartup())
                addToStartup();
        } catch (DeniedException e) {
            throw new RuntimeException(e);
        }
    }


    private void initTray(Stage stage) {
        if (SystemTray.isSupported()) {
            Platform.setImplicitExit(false);
            var tray = SystemTray.getSystemTray();
            var image = Toolkit.getDefaultToolkit().getImage(getResource("icons/logo.png"));
            var popup = new PopupMenu();
            var openItem = new MenuItem("Open App");
            var exitItem = new MenuItem("Exit App");
            ActionListener openListener = e -> Platform.runLater(() -> {
                if (stage.isShowing())
                    stage.toFront();
                else stage.show();
            });
            openItem.addActionListener(openListener);
            exitItem.addActionListener(e -> stop());
            popup.add(openItem);
            popup.add(exitItem);
            var trayIcon = new TrayIcon(image, "BitKip", popup);
            trayIcon.addActionListener(openListener);
            trayIcon.setImageAutoSize(true);
            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                log.error(e.toString());
            }

        }

    }


    @Override
    public void stop() {
        var notObservedDms = new ArrayList<>(currentDownloadings);
        notObservedDms.forEach(dm -> dm.getDownloadTask().pause(()->{}));
        startedQueues.clear();
        currentSchedules.values().forEach(sm -> {
            var startScheduler = sm.getStartScheduler();
            var stopScheduler = sm.getStopScheduler();
            if (startScheduler != null)
                startScheduler.shutdownNow();
            if (stopScheduler != null)
                stopScheduler.shutdownNow();
        });
        try {
            if (server != null)
                server.shutdown();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.exit(0);
    }


    public static void main(String[] args) {
        initLogger();
        GlobalExceptionHandler.setup();
        launch();
    }

    private static void startServer() {
        if (serverEnabled) {
            var cors = CorsSupport.builder()
                    .addCrossOrigin(CrossOriginConfig.builder()
                            .allowMethods("POST", "GET")
                            .build()
                    )
                    .build();
            var routing = Routing.builder()
                    .register("/single", cors, new SingleService())
                    .register("/batch", cors, new BatchService())
                    .register("/sync", cors, new SyncService())
                    .build();
            var jacksonSupport = JacksonSupport.create();
            server = WebServer.builder()
                    .bindAddress("127.0.0.1")
                    .port(serverPort)
                    .addRouting(routing)
                    .addMediaSupport(jacksonSupport)
                    .build();
            server.start().exceptionally(throwable -> {
                var header = "Failed to start server. Is there another instance running?\nIf not you may need to change application server port and restart";
                log.error(header);
                Platform.runLater(() -> {
                    if (FxUtils.showFailedToStart(header, throwable.toString()))
                        Platform.exit();
                });
                return null;
            });
        }

    }

    public static URL getResource(String path) {
        return BitKip.class.getResource(path);
    }
}