package ir.darkdeveloper.bitkip.servlets;

import ir.darkdeveloper.bitkip.models.URLModel;
import ir.darkdeveloper.bitkip.utils.FxUtils;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javafx.application.Platform;

import java.io.IOException;

import static ir.darkdeveloper.bitkip.config.AppConfigs.log;
import static ir.darkdeveloper.bitkip.config.AppConfigs.mapper;

public class SingleServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        try {
            var urlModel = mapper.readValue(req.getReader(), URLModel.class);
            Platform.runLater(() -> FxUtils.newDownloadStage(true, urlModel));
        } catch (IOException e) {
            try {
                log.error(e.getLocalizedMessage());
                resp.getWriter().write("failed to read payload");
            } catch (IOException ex) {
                log.error(ex.getLocalizedMessage());
            }
        }
    }

}
