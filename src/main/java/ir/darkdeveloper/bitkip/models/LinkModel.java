package ir.darkdeveloper.bitkip.models;

import ir.darkdeveloper.bitkip.utils.IOUtils;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LinkModel {
    private String link;
    private long size;
    private int chunks;
    private Boolean resumeable;
    private String name;

    private String sizeString;
    private String resumeableString;

    public LinkModel(String link, long size, int chunks, boolean resumeable, String name) {
        this.link = link;
        this.size = size;
        this.chunks = chunks;
        this.resumeable = resumeable;
        this.name = name;
    }

    public LinkModel(String link, int chunks) {
        this.link = link;
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

}
