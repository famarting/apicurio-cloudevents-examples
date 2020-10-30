package io.famartin.eventing;

import java.io.IOException;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.Response;

import io.apicurio.registry.client.RegistryRestClientFactory;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.message.MessageReader;
import io.cloudevents.http.restful.ws.impl.RestfulWSMessageFactory;
import io.famartin.cloudevents.JsonSchemaMiddleware;

public class OutboundCloudEventDataValidator implements  ClientRequestFilter {

    JsonSchemaMiddleware middleware = new JsonSchemaMiddleware(RegistryRestClientFactory.create("http://localhost:8080/api"));

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {

        if (requestContext.getEntity() != null) {

            MessageReader reader = RestfulWSMessageFactory.create(requestContext.getMediaType(), requestContext.getStringHeaders(), (byte[])requestContext.getEntity());

            CloudEvent event = reader.toEvent();
            try {
                //magic happening here
                middleware.validateData(event);

            } catch (Exception e) {
                System.out.println("Error validating client request");
                e.printStackTrace();
                requestContext.abortWith(
                    Response.status(400)
                        .header("Apicurio-Filter", "Validated")
                        .header("content-type", "text/plain")
                        .entity(e.getMessage())
                        .build());
            }

        }

    }

}