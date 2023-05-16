package ir.darkdeveloper.bitkip.models;

import ir.darkdeveloper.bitkip.utils.IOUtils;
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
    private String url;
    private long size;
    private int chunks;
    private Boolean resumeable;
    private String name;
    private String path;
    private transient String agent;

    private final List<QueueModel> queues = new ArrayList<>();

    private String sizeString;
    private String resumeableString;
    private String queuesString;


    public LinkModel(String url, int chunks) {
        this.url = url;
        this.chunks = chunks;
    }

    public String getSizeString() {
        return IOUtils.formatBytes(size);
    }

    public String getResumeableString() {
        if (resumeable == null)
            return "";
        return resumeable ? "yes" : "no";
    }

    public String getQueuesString() {
        var q = queues.toString();
        return q.substring(q.lastIndexOf(',') + 2, q.lastIndexOf(']'));
    }
}
