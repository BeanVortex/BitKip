package io.beanvortex.bitkip.api;

import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

import static io.beanvortex.bitkip.config.AppConfigs.serverPort;

public class SyncService implements Service {
    @Override
    public void update(Routing.Rules rules) {
        rules.get("/", this::doGet);
    }

    private void doGet(ServerRequest req, ServerResponse res) {
        res.status(200).send(serverPort);
    }
}
