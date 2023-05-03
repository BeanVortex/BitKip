package ir.darkdeveloper.bitkip.task;


import ir.darkdeveloper.bitkip.models.UpdateModel;
import javafx.concurrent.Task;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

import static com.sun.jna.Platform.*;
import static ir.darkdeveloper.bitkip.config.AppConfigs.VERSION;

public class UpdateCheckTask extends Task<UpdateModel> {
    private ExecutorService executor;

    @Override
    protected UpdateModel call() throws Exception {
        var url = "https://github.com/DarkDeveloper-arch/BitKip/releases";
        var doc = getDocument(url);
        var updateVersion = doc.select(".Box-body").get(0)
                .select("div").get(0)
                .select("span").get(0)
                .text().substring(1);
        var descriptionTitle = doc.select(".Box-body").get(0).select(".markdown-body h3").text();
        var descriptionList = doc.select(".Box-body").get(0).select(".markdown-body ul li");
        var features = new ArrayList<String>();
        for (var li : descriptionList)
            features.add(li.text());

        var description = new UpdateModel.Description(descriptionTitle, features);


        if (!updateVersion.equals(VERSION)) {
            var newUrlToCrawl = url + "/expanded_assets/v" + updateVersion;
            var doc2 = getDocument(newUrlToCrawl);
            var rows = doc2.select("li");
            var assets = new ArrayList<UpdateModel.Asset>();
            for (var row : rows) {
                var fileATag = row.select("a").get(0);
                var fileTitle = fileATag.select("span").text();
                if (isWindows() && !fileTitle.contains("win") && !fileTitle.contains("exe") && !fileTitle.contains("msi")
                        && !(fileTitle.contains("Source code") && fileTitle.contains("zip")))
                    continue;
                if (isLinux() && !fileTitle.contains("linux") && !fileTitle.contains("tar.gz"))
                    continue;
                if (isMac() && !fileTitle.contains("mac") && !fileTitle.contains("dmg") && !fileTitle.contains("tar.gz"))
                    continue;
                var fileLink = "https://github.com" + fileATag.attr("href");
                var fileSize = row.select("div > span").get(0).text();
                if (!fileSize.contains("MB"))
                    fileSize = "";
                assets.add(new UpdateModel.Asset(fileTitle, fileLink, fileSize));
            }
            executor.shutdown();
            return new UpdateModel(updateVersion, description, assets);
        }
        executor.shutdown();
        return null;
    }

    private static Document getDocument(String url) throws InterruptedException {
        Document doc = null;
        for (int i = 0; i < 3; i++) {
            try {
                doc = Jsoup.connect(url)
                        .userAgent("Mozilla")
                        .get();
                break;
            } catch (IOException e) {
                e.printStackTrace();
                Thread.sleep(2000);
            }
        }
        if (doc == null)
            throw new RuntimeException("Not Found");
        return doc;
    }


    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }
}
