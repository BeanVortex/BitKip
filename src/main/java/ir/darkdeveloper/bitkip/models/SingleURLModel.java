package ir.darkdeveloper.bitkip.models;

public record SingleURLModel(Long fileSize, String filename, String mimeType, Boolean resumable, String url, String agent) {
}
