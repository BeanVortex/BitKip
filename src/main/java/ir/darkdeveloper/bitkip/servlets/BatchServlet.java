package ir.darkdeveloper.bitkip.servlets;

import ir.darkdeveloper.bitkip.controllers.BatchDownload;
import ir.darkdeveloper.bitkip.models.BatchURLModel;
import ir.darkdeveloper.bitkip.models.LinkModel;
import ir.darkdeveloper.bitkip.repo.QueuesRepo;
import ir.darkdeveloper.bitkip.utils.FxUtils;
import ir.darkdeveloper.bitkip.utils.Validations;
import ir.darkdeveloper.bitkip.utils.DownloadUtils;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javafx.application.Platform;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static ir.darkdeveloper.bitkip.config.AppConfigs.log;
import static ir.darkdeveloper.bitkip.config.AppConfigs.mapper;
import static ir.darkdeveloper.bitkip.utils.Defaults.ALL_DOWNLOADS_QUEUE;

public class BatchServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        try {
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");

            var urlModel = mapper.readValue(req.getReader(), BatchURLModel.class);
            var links = convertToLinks(urlModel);
            if (links.isEmpty())
                throw new IOException("Empty data sent by extension");
            Platform.runLater(() -> FxUtils.newBatchListStage(links));
            resp.setStatus(200);
        } catch (IOException e) {
            log.warn(e.getLocalizedMessage());
            resp.setStatus(400);
        }
    }

    private List<LinkModel> convertToLinks(BatchURLModel urlModel) throws IOException {
        var links = urlModel.links();
        if (links == null || links.isEmpty())
            return Collections.emptyList();
        var chunks = Validations.maxChunks();
        var allDownloadsQueue = QueuesRepo.findByName(ALL_DOWNLOADS_QUEUE, false);
        var firstUrl = links.get(0);
        var connection = DownloadUtils.connect(firstUrl, true);
        var firstFileName = DownloadUtils.extractFileName(firstUrl, connection);
        var secondaryQueue = BatchDownload.getSecondaryQueueByFileName(firstFileName);
        var path = DownloadUtils.determineLocation(firstFileName);
        return urlModel.links().stream().map(s -> {
            var lm = new LinkModel(s, chunks);
            lm.getQueues().add(allDownloadsQueue);
            lm.getQueues().add(secondaryQueue);
            lm.setPath(path);
            return lm;
        }).toList();
    }
}
