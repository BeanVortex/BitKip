package ir.darkdeveloper.bitkip.servlets;

import ir.darkdeveloper.bitkip.models.URLModel;
import ir.darkdeveloper.bitkip.utils.FxUtils;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.io.IOException;

import static ir.darkdeveloper.bitkip.config.AppConfigs.log;
import static ir.darkdeveloper.bitkip.config.AppConfigs.mapper;

public class ServletController extends HttpServlet {
    private final Stage stage;


    public ServletController(Stage stage) {
        this.stage = stage;
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        var urlModel = mapper.readValue(req.getReader(), URLModel.class);
        Platform.runLater(() -> FxUtils.newDownloadStage(true, urlModel));
        log.info(urlModel.toString());
    }

}
