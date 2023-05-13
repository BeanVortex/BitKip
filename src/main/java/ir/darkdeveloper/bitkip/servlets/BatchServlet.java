package ir.darkdeveloper.bitkip.servlets;

import ir.darkdeveloper.bitkip.controllers.BatchDownload;
import ir.darkdeveloper.bitkip.models.BatchURLModel;
import ir.darkdeveloper.bitkip.models.LinkModel;
import ir.darkdeveloper.bitkip.repo.QueuesRepo;
import ir.darkdeveloper.bitkip.utils.FxUtils;
import ir.darkdeveloper.bitkip.utils.InputValidations;
import ir.darkdeveloper.bitkip.utils.NewDownloadUtils;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javafx.application.Platform;

import java.io.IOException;
import java.util.List;

import static ir.darkdeveloper.bitkip.config.AppConfigs.log;
import static ir.darkdeveloper.bitkip.config.AppConfigs.mapper;
import static ir.darkdeveloper.bitkip.utils.FileExtensions.ALL_DOWNLOADS_QUEUE;

public class BatchServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        try {
            var urlModel = mapper.readValue(req.getReader(), BatchURLModel.class);
            var links = convertToLinks(urlModel);
            Platform.runLater(() -> FxUtils.newBatchListStage(links));
        } catch (IOException e) {
            try {
                log.error(e.getLocalizedMessage());
                resp.getWriter().write("failed to read payload");
            } catch (IOException ex) {
                log.error(ex.getLocalizedMessage());
            }
        }
    }

    private List<LinkModel> convertToLinks(BatchURLModel urlModel) {
        var threads = InputValidations.maxChunks();
        var links = urlModel.links().stream().map(s -> new LinkModel(s, threads)).toList();
        var allDownloadsQueue = QueuesRepo.findByName(ALL_DOWNLOADS_QUEUE, false);
        var firstUrl = links.get(0).getUrl();
        var connection = NewDownloadUtils.connect(firstUrl, 3000, 3000);
        var firstFileName = NewDownloadUtils.extractFileName(firstUrl, connection);
        var secondaryQueue = BatchDownload.getSecondaryQueueByFileName(firstFileName);
        var path = NewDownloadUtils.determineLocation(firstFileName);
        links.forEach(lm -> {
            lm.getQueues().add(allDownloadsQueue);
            lm.getQueues().add(secondaryQueue);
            lm.setPath(path);
        });
        return links;
    }
}
