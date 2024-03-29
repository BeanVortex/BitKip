package io.beanvortex.bitkip.models;

import io.beanvortex.bitkip.utils.IOUtils;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LinkModel {
    private String uri;
    private long size;
    private int chunks;
    private Boolean resumable;
    private String name;
    private String path;
    private String selectedPath;

    private final List<QueueModel> queues = new ArrayList<>();

    private String sizeString;
    private String resumableString;
    private String queuesString;


    public LinkModel(String uri, int chunks) {
        this.uri = uri;
        this.chunks = chunks;
    }

    public String getSizeString() {
        return IOUtils.formatBytes(size);
    }

    public String getResumableString() {
        if (resumable == null)
            return "";
        return resumable ? "yes" : "no";
    }

    public String getQueuesString() {
        if (queues.size() == 3)
            return queues.get(2).getName();
        return queues.get(0).getName();
    }

    public String getSelectedPath() {
        return selectedPath == null ? path : selectedPath;
    }
}
