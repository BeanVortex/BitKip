package io.beanvortex.bitkip;

import io.beanvortex.bitkip.config.AppConfigs;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class InstanceManager {
    public static boolean notifyCurrentInstance() {
        try (var client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .build()) {

            var request = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:" + AppConfigs.serverPort + "/new_instance"))
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();

            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {

            return false;
        }
    }
}
