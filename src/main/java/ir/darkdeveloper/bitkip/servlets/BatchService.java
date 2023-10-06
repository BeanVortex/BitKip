package ir.darkdeveloper.bitkip.servlets;

import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import ir.darkdeveloper.bitkip.controllers.BatchDownload;
import ir.darkdeveloper.bitkip.models.BatchURLModel;
import ir.darkdeveloper.bitkip.models.LinkModel;
import ir.darkdeveloper.bitkip.repo.QueuesRepo;
import ir.darkdeveloper.bitkip.utils.DownloadUtils;
import ir.darkdeveloper.bitkip.utils.FxUtils;
import ir.darkdeveloper.bitkip.utils.Validations;
import javafx.application.Platform;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static ir.darkdeveloper.bitkip.utils.Defaults.ALL_DOWNLOADS_QUEUE;

public class BatchService implements Service {
    @Override
    public void update(Routing.Rules rules) {
        rules.post("/", this::doPost);
    }

    private void doPost(ServerRequest req, ServerResponse res) {
            var urlModel = req.content().as(BatchURLModel.class);
            urlModel.thenAccept(batchURLModel -> {
                List<LinkModel> links;
                try {
                    links = convertToLinks(batchURLModel);
                } catch (IOException e) {
                    res.status(400).send("Connection error");
                    return;
                }
                if (links.isEmpty()){
                    res.status(400).send("Empty data sent by extension");
                    return;
                }
                Platform.runLater(() -> FxUtils.newBatchListStage(links));
                res.status(200).send();
            });
    }

    private List<LinkModel> convertToLinks(BatchURLModel urlModel) throws IOException {
        var links = urlModel.links();
        if (links == null || links.isEmpty())
            return Collections.emptyList();
        var chunks = Validations.maxChunks(Long.MAX_VALUE);
        var allDownloadsQueue = QueuesRepo.findByName(ALL_DOWNLOADS_QUEUE, false);
        var firstUrl = links.get(0);
        var connection = DownloadUtils.connect(firstUrl);
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
