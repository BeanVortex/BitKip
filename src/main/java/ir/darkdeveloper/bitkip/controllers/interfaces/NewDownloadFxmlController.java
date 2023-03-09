package ir.darkdeveloper.bitkip.controllers.interfaces;

import ir.darkdeveloper.bitkip.config.QueueObserver;
import ir.darkdeveloper.bitkip.utils.TableUtils;

public interface NewDownloadFxmlController extends QueueObserver {
    void setTableUtils(TableUtils tableUtils);
}
