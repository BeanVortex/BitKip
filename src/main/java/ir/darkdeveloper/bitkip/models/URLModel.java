
package ir.darkdeveloper.bitkip.models;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class URLModel {
    private Long fileSize;
    private String filename;
    private String mimeType;
    private Boolean resumable;
    private String url;
}
