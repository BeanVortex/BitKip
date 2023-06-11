package ir.darkdeveloper.bitkip.models;

public record SingleURLModel(Long fileSize, String filename,
                             String mimeType, String url, String agent) {
}
