package ir.darkdeveloper.bitkip;

import ir.darkdeveloper.bitkip.config.AppConfigs;
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
import javafx.stage.Stage;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.awt.*;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.ArrayList;

import static ir.darkdeveloper.bitkip.config.AppConfigs.*;

public class BitKip extends Application {

    private static Server server;


    @Override
    public void start(Stage stage) {
        IOUtils.createSaveLocations();
        DownloadsRepo.createTable();
        ScheduleRepo.createSchedulesTable();
        QueuesRepo.createTable();
        var queues = QueuesRepo.getAllQueues(false, true);
        if (queues.isEmpty())
            queues = QueuesRepo.createDefaultRecords();
        queues = ScheduleRepo.createDefaultSchedulesForQueues(queues);
        AppConfigs.addAllQueues(queues);
        IOUtils.createFoldersForQueue();
        AppConfigs.setHostServices(getHostServices());
        FxUtils.startMainStage(stage);
        ScheduleTask.startSchedules();
        MoreUtils.checkUpdates(false);
        IOUtils.moveChunkFilesToTemp(downloadPath);
        initTray(stage);
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
            exitItem.addActionListener(e -> {
                stop();
                System.exit(0);
            });
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
            server.stop();
        }catch (Exception e){
            log.error(e.getMessage());
        }
    }


    public static void main(String[] args) {
        initLogger();
        startServer();
        launch();
    }

    private static void startServer() {
        var threadPool = new QueuedThreadPool(5, 1);
        server = new Server(threadPool);
        var connector = new ServerConnector(server);
        connector.setPort(56423);
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
            System.exit(1);
        }
    }

    public static URL getResource(String path) {
        return BitKip.class.getResource(path);
    }
}