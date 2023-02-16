package ir.darkdeveloper.bitkip.controllers.interfaces;

import ir.darkdeveloper.bitkip.utils.TableUtils;

public interface NewDownloadFxmlController extends FXMLController {
    void setTableUtils(TableUtils tableUtils);

    void setParentController(FXMLController parentController);
}
