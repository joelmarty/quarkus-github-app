package io.quarkiverse.githubapp.runtime;

import static io.quarkiverse.githubapp.runtime.Headers.X_GITHUB_DELIVERY;
import static io.quarkiverse.githubapp.runtime.Headers.X_GITHUB_EVENT;
import static io.quarkiverse.githubapp.runtime.Headers.X_HUB_SIGNATURE;
import static io.quarkiverse.githubapp.runtime.Headers.X_REQUEST_ID;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.quarkus.vertx.web.Header;
import io.quarkus.vertx.web.Route;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Singleton
public class Routes {

    private static final String EMPTY_RESPONSE = "{}";

    @Inject
    Event<GitHubEvent> gitHubEventEmitter;

    @Route(path = "/", methods = HttpMethod.POST, consumes = "application/json", produces = "application/json")
    public String handleRequest(RoutingContext routingContext,
            @Header(X_REQUEST_ID) String requestId,
            @Header(X_HUB_SIGNATURE) String hubSignature,
            @Header(X_GITHUB_DELIVERY) String gitHubDelivery,
            @Header(X_GITHUB_EVENT) String gitHubEvent) {

        JsonObject body = routingContext.getBodyAsJson();
        if (body == null) {
            return EMPTY_RESPONSE;
        }

        Long installationId = extractInstallationId(body);

        gitHubEventEmitter.fireAsync(new GitHubEvent(installationId, gitHubEvent, body.getString("action"), routingContext.getBodyAsString()));

        return EMPTY_RESPONSE;
    }

    private Long extractInstallationId(JsonObject body) {
        Long installationId;

        JsonObject installation = body.getJsonObject("installation");
        if (installation != null) {
            installationId = installation.getLong("id");
            if (installationId != null) {
                return installationId;
            }
        }

        throw new IllegalStateException("Unable to extract installation id from payload");
    }
}
