package io.famartin.eventing;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import io.apicurio.registry.client.RegistryRestClientFactory;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.message.MessageReader;
import io.cloudevents.http.vertx.VertxMessageFactory;
import io.famartin.cloudevents.JsonSchemaMiddleware;
import io.quarkus.vertx.http.runtime.filters.Filters;

// cannot use ContainerRequestFilter because you would read request body twice

@ApplicationScoped
public class InboundCloudEventDataValidator {

    JsonSchemaMiddleware middleware = new JsonSchemaMiddleware(RegistryRestClientFactory.create("http://localhost:8080/api"));

    public void filters(@Observes Filters filters) {
        filters.register(
            rc -> {
                if (rc.request().getHeader("ce-type") != null && rc.request().getHeader("ce-specversion") != null) {
                    //it's a cloud event

                    rc.response()
                        .putHeader("Apicurio-Filter", "Validated");

                    rc.request().bodyHandler(body -> {

                        rc.setBody(body);

                        MessageReader reader = VertxMessageFactory.createReader(rc.request().headers(), body);

                        CloudEvent event = reader.toEvent();
                        try {
                            //magic happening here
                            middleware.validateData(event);

                            rc.next();
                        } catch (Exception e) {
                            rc.response()
                                .setStatusCode(400)
                                .putHeader("content-type", "text/plain")
                                .end(e.getMessage());
                        }

                    });

                } else {
                    rc.next();
                }
            },
            10);
    }

}