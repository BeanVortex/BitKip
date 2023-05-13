package ir.darkdeveloper.bitkip.servlets;

import ir.darkdeveloper.bitkip.models.BatchURLModel;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import static ir.darkdeveloper.bitkip.config.AppConfigs.log;
import static ir.darkdeveloper.bitkip.config.AppConfigs.mapper;

public class BatchServlet  extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        try {
            var urlModel = mapper.readValue(req.getReader(), BatchURLModel.class);
            log.info(urlModel.toString());
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
