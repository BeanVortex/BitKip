package io.beanvortex.bitkip.api;

import io.beanvortex.bitkip.models.BatchURLModel;
import io.beanvortex.bitkip.models.LinkModel;
import io.beanvortex.bitkip.repo.QueuesRepo;
import io.beanvortex.bitkip.utils.FxUtils;
import io.beanvortex.bitkip.utils.Validations;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import javafx.application.Platform;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static io.beanvortex.bitkip.utils.Defaults.ALL_DOWNLOADS_QUEUE;

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
        return urlModel.links().stream().map(s -> {
            var lm = new LinkModel(s, chunks);
            lm.getQueues().add(allDownloadsQueue);
            return lm;
        }).toList();
    }


}
