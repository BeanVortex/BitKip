package ir.darkdeveloper.bitkip.controllers.interfaces;

import ir.darkdeveloper.bitkip.config.QueueObserver;
import ir.darkdeveloper.bitkip.models.URLModel;

public interface NewDownload extends QueueObserver {
    void setUrlModel(URLModel urlModel);
}
