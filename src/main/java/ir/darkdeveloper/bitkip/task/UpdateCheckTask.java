package ir.darkdeveloper.bitkip.task;


import javafx.concurrent.Task;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

public class UpdateCheckTask extends Task<String> {
    private ExecutorService executor;

    @Override
    protected String call() throws Exception {

        Document doc = null;
        for (int i = 0; i < 3; i++) {
            try {
                doc = Jsoup.connect("https://github.com/DarkDeveloper-arch/BitKip/releases")
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

        var updateBox = doc.select(".Box-body").get(0);
        var updateVersion = updateBox.select("div").get(0).select("span").get(0).text().substring(1);
        var updateDescription = updateBox.select("div.markdown-body").text();

        executor.shutdown();
        return updateVersion + "," + updateDescription;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }
}
