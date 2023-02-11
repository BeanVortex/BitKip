package ir.darkdeveloper.bitkip.controllers;

import ir.darkdeveloper.bitkip.task.DownloadTask;
import ir.darkdeveloper.bitkip.utils.TableUtils;

import java.util.List;

public interface NewDownloadFxmlController extends FXMLController{
    void setTableUtils(TableUtils tableUtils);

    void setDownloadTaskList(List<DownloadTask> downloadTaskList);
}
