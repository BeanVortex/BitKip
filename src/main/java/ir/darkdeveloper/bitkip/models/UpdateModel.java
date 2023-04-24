package ir.darkdeveloper.bitkip.models;

import java.util.List;

public record UpdateModel(String version, String description, List<Asset> assets) {
    public record Asset(String title, String link, String size) {
    }
}

