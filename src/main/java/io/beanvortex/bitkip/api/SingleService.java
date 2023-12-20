package io.beanvortex.bitkip.api;

import io.beanvortex.bitkip.utils.DownloadOpUtils;
import io.beanvortex.bitkip.utils.FxUtils;
import io.beanvortex.bitkip.utils.IOUtils;
import io.helidon.common.reactive.Single;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import io.beanvortex.bitkip.models.SingleURLModel;
import javafx.application.Platform;

import static io.beanvortex.bitkip.config.AppConfigs.downloadImmediately;
import static io.beanvortex.bitkip.config.AppConfigs.userAgent;

public class SingleService implements Service {
    @Override
    public void update(Routing.Rules rules) {
        rules.post("/", this::doPost);
    }


    private void doPost(ServerRequest req, ServerResponse res) {
        Single<SingleURLModel> singleURLModelSingle = req.content().as(SingleURLModel.class);
        singleURLModelSingle.thenAccept(urlModel -> {
            var agent = urlModel.agent();
            if (agent != null && !agent.isBlank() && !agent.equals(userAgent)) {
                userAgent = agent;
                IOUtils.saveConfigs();
            }
            Platform.runLater(() -> {
                if (downloadImmediately) DownloadOpUtils.downloadImmediately(urlModel);
                else FxUtils.newDownloadStage(true, urlModel);
            });
            res.status(200).send();
        });
    }

}
