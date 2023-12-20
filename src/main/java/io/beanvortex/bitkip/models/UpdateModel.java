package io.beanvortex.bitkip.models;

import java.util.List;

public record UpdateModel(String version, Description description, List<Asset> assets) {
    public record Asset(String title, String link, String size) {
    }
    public record Description(String header, List<String> features){}
}

