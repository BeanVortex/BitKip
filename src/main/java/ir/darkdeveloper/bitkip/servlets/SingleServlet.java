package ir.darkdeveloper.bitkip.servlets;

import ir.darkdeveloper.bitkip.models.SingleURLModel;
import ir.darkdeveloper.bitkip.utils.DownloadOpUtils;
import ir.darkdeveloper.bitkip.utils.FxUtils;
import ir.darkdeveloper.bitkip.utils.IOUtils;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javafx.application.Platform;

import java.io.IOException;

import static ir.darkdeveloper.bitkip.config.AppConfigs.*;

public class SingleServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        try {
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");

            var urlModel = mapper.readValue(req.getReader(), SingleURLModel.class);
            var agent = urlModel.agent();
            if (agent != null && !agent.isBlank() && !agent.equals(userAgent)) {
                userAgent = agent;
                IOUtils.saveConfigs();
            }
            Platform.runLater(() -> {
                if (downloadImmediately) DownloadOpUtils.downloadImmediately(urlModel);
                else FxUtils.newDownloadStage(true, urlModel);
            });
            resp.setStatus(200);
        } catch (IOException e) {
            resp.setStatus(400);
            log.warn(e.getLocalizedMessage());
        }
    }


}
