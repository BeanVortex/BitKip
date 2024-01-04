package io.beanvortex.bitkip.task;

import io.beanvortex.bitkip.utils.FxUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import static io.beanvortex.bitkip.task.UpdateCheckTask.getDocument;

public class ExtensionInstallTask extends Task<Integer> {
    private ExecutorService executor;

    @Override
    protected Integer call() throws InterruptedException, IOException {
        var url = "https://github.com/BeanVortex/BitKip/releases";
        var doc = getDocument(url);
        var updateVersion = doc.select(".Box-body").get(0)
                .select("div").get(0)
                .select("span").get(0)
                .text().substring(1);


        var newUrlToCrawl = url + "/expanded_assets/v" + updateVersion;
        var doc2 = getDocument(newUrlToCrawl);
        var rows = doc2.select("li");
        var extensionPath = "";
        for (var row : rows) {
            var aTag = row.select("a").get(0);
            var href = aTag.attr("href");
            if (href.contains("extension")){
                extensionPath = aTag.attr("href");
                break;
            }
        }
        var fileLink = "https://github.com" + extensionPath;
        Platform.runLater(() -> {
            FxUtils.setClipboard(fileLink);
            FxUtils.newDownloadStage(true, null);
        });
        executor.shutdownNow();
        return 0;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

}
