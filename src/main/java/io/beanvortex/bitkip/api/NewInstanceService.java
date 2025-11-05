package io.beanvortex.bitkip.api;

import io.beanvortex.bitkip.BitKip;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import javafx.application.Platform;

public class NewInstanceService implements Service {
    @Override
    public void update(Routing.Rules rules) {
        rules.get("/", this::doGet);
    }

    private void doGet(ServerRequest req, ServerResponse res) {
        Platform.runLater(BitKip::show);
        res.status(200);
        res.send("OK");
    }
}
