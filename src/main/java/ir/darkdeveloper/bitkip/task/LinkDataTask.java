package ir.darkdeveloper.bitkip.task;

import ir.darkdeveloper.bitkip.models.LinkModel;
import ir.darkdeveloper.bitkip.utils.NewDownloadUtils;
import javafx.concurrent.Task;
import reactor.core.publisher.Flux;

import java.util.List;

public class LinkDataTask extends Task<Flux<LinkModel>> {

    private final List<LinkModel> links;

    public LinkDataTask(List<LinkModel> links) {
        this.links = links;
    }

    @Override
    protected Flux<LinkModel> call() {
        return Flux.create(fluxSink -> {
            links.forEach(lm -> {
                var connection = NewDownloadUtils.connect(lm.getUrl(), 3000, 3000);
                var fileSize = NewDownloadUtils.getFileSize(connection);
                var fileName = NewDownloadUtils.extractFileName(lm.getUrl(), connection);
                lm.setName(fileName);
                lm.setSize(fileSize);
                lm.setResumeable(NewDownloadUtils.canResume(connection));
                fluxSink.next(lm);
            });
            fluxSink.complete();
        });
    }
}
