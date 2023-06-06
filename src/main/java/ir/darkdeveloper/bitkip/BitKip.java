package ir.darkdeveloper.bitkip;

import ir.darkdeveloper.bitkip.config.AppConfigs;
import ir.darkdeveloper.bitkip.config.observers.QueueSubject;
import ir.darkdeveloper.bitkip.servlets.BatchServlet;
import ir.darkdeveloper.bitkip.servlets.SingleServlet;
import ir.darkdeveloper.bitkip.repo.DownloadsRepo;
import ir.darkdeveloper.bitkip.repo.QueuesRepo;
import ir.darkdeveloper.bitkip.repo.ScheduleRepo;
import ir.darkdeveloper.bitkip.task.ScheduleTask;
import ir.darkdeveloper.bitkip.utils.FxUtils;
import ir.darkdeveloper.bitkip.utils.IOUtils;
import ir.darkdeveloper.bitkip.utils.MoreUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.awt.*;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import static ir.darkdeveloper.bitkip.config.AppConfigs.*;

public class BitKip extends Application {

    private static Server server;


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
        ScheduleTask.startSchedules();
        MoreUtils.checkUpdates(false);
        IOUtils.moveChunkFilesToTemp(downloadPath);
        initTray(stage);
        startServer();
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
                log.error(e.getLocalizedMessage());
            }

        }

    }


    @Override
    public void stop() {
        var notObservedDms = new ArrayList<>(currentDownloadings);
        notObservedDms.forEach(dm -> dm.getDownloadTask().pause());
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
                server.stop();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        System.exit(0);
    }


    public static void main(String[] args) throws IOException {
        initLogger();
        launch();
    }

    private static void startServer() {
        if (serverEnabled) {
            var threadPool = new QueuedThreadPool(5, 1);
            server = new Server(threadPool);
            var connector = new ServerConnector(server);
            connector.setPort(serverPort);
            server.setConnectors(new Connector[]{connector});
            var handler = new ServletHandler();
            server.setHandler(handler);

            handler.addServletWithMapping(SingleServlet.class, "/single");
            handler.addServletWithMapping(BatchServlet.class, "/batch");

            // Start the server
            try {
                server.start();
            } catch (Exception e) {
                log.error(e.getMessage());
                if (FxUtils.askWithDialog(Alert.AlertType.ERROR, "Failed to run",
                        e.getLocalizedMessage(), ButtonType.CLOSE, null))
                    System.exit(1);
            }
        }
    }

    public static URL getResource(String path) {
        return BitKip.class.getResource(path);
    }
}