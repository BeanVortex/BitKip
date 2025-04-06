package io.beanvortex.bitkip.task;

import io.beanvortex.bitkip.controllers.BatchDownload;
import io.beanvortex.bitkip.models.Credentials;
import io.beanvortex.bitkip.utils.DownloadUtils;
import io.beanvortex.bitkip.models.LinkModel;
import javafx.concurrent.Task;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;

import static io.beanvortex.bitkip.config.AppConfigs.log;

public class LinkDataTask extends Task<Flux<LinkModel>> {

    private final List<LinkModel> links;
    private final Credentials credentials;
    private boolean cancel;

    public LinkDataTask(List<LinkModel> links, Credentials credentials) {
        this.links = links;
        this.credentials = credentials;
    }

    @Override
    protected Flux<LinkModel> call() {
        return Flux.create(fluxSink -> {
            for (var lm : links) {
                if (cancel)
                    break;
                HttpURLConnection connection;
                try {
                    connection = DownloadUtils.connect(lm.getUri(), credentials);
                } catch (IOException e) {
                    log.error(e.toString());
                    break;
                }
                var uri = lm.getUri();
                var fileSize = DownloadUtils.getFileSize(connection);
                var fileName = DownloadUtils.extractFileName(uri, connection);
                var secondaryQueue = BatchDownload.getSecondaryQueueByFileName(fileName);
                var path = DownloadUtils.determineLocation(fileName);
                lm.setName(DownloadUtils.getNewFileNameIfExists(fileName, lm.getPath()));
                lm.setSize(fileSize);
                lm.setPath(path);
                if (!lm.getQueues().contains(secondaryQueue))
                    lm.getQueues().add(secondaryQueue);
                lm.setResumable(DownloadUtils.canResume(connection));
                fluxSink.next(lm);
            }
            fluxSink.complete();
        });
    }

    public void setCancel(boolean cancel) {
        this.cancel = cancel;
    }
}
