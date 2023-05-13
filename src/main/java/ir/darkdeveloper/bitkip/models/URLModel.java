package ir.darkdeveloper.bitkip.models;

public record URLModel(Long fileSize, String filename, String mimeType, Boolean resumable, String url) {
}
