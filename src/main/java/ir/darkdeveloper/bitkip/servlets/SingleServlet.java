package ir.darkdeveloper.bitkip.servlets;

import ir.darkdeveloper.bitkip.models.SingleURLModel;
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
            var urlModel = mapper.readValue(req.getReader(), SingleURLModel.class);
            var agent = urlModel.agent();
            if (agent != null && !agent.isBlank() && !agent.equals(userAgent)) {
                userAgent = agent;
                IOUtils.saveConfigs();
            }
            Platform.runLater(() -> FxUtils.newDownloadStage(true, urlModel));
        } catch (IOException e) {
            try {
                log.warn(e.getLocalizedMessage());
                resp.getWriter().write("failed to read payload");
            } catch (IOException ex) {
                log.warn(ex.getLocalizedMessage());
            }
        }
    }

}
