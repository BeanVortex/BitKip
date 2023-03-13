package ir.darkdeveloper.bitkip.controllers.interfaces;

import ir.darkdeveloper.bitkip.config.QueueObserver;
import ir.darkdeveloper.bitkip.utils.MainTableUtils;

public interface NewDownloadFxmlController extends QueueObserver {
    void setMainTableUtils(MainTableUtils mainTableUtils);

}
