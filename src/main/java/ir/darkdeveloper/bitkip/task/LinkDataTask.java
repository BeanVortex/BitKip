package ir.darkdeveloper.bitkip.task;

import ir.darkdeveloper.bitkip.models.LinkModel;
import ir.darkdeveloper.bitkip.utils.NewDownloadUtils;
import javafx.concurrent.Task;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;

import static ir.darkdeveloper.bitkip.config.AppConfigs.log;

public class LinkDataTask extends Task<Flux<LinkModel>> {

    private final List<LinkModel> links;
    private boolean cancel;

    public LinkDataTask(List<LinkModel> links) {
        this.links = links;
    }

    @Override
    protected Flux<LinkModel> call() {
        return Flux.create(fluxSink -> {
            for (var lm : links) {
                if (cancel)
                    break;
                HttpURLConnection connection;
                try {
                    connection = NewDownloadUtils.connect(lm.getUrl(), true);
                } catch (IOException e) {
                    log.error(e.getMessage());
                    break;
                }
                var fileSize = NewDownloadUtils.getFileSize(connection);
                var fileName = NewDownloadUtils.extractFileName(lm.getUrl(), connection);
                lm.setName(fileName);
                lm.setSize(fileSize);
                lm.setResumable(NewDownloadUtils.canResume(connection));
                fluxSink.next(lm);
            }
            fluxSink.complete();
        });
    }

    public void setCancel(boolean cancel) {
        this.cancel = cancel;
    }
}
