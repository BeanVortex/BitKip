package ir.darkdeveloper.bitkip.config.observers;

import ir.darkdeveloper.bitkip.controllers.interfaces.FXMLController;
import ir.darkdeveloper.bitkip.models.QueueModel;

public interface QueueObserver extends FXMLController {
    void updateQueue();
}
