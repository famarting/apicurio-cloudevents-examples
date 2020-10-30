package io.famartin.eventing;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jboss.logging.Logger;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;

/**
 * 
 * This example shows the implicit usage of apicurio cloudevents library, but here we only VALIDATE data
 * all endpoints in this demo will be cloud event validated because of a configured http filter 
 * 
 */
@ApplicationScoped
@Path("/")
public class OrdersResource {

    private static final Logger log = Logger.getLogger(OrdersResource.class);

    private ObjectMapper mapper = new ObjectMapper();

    @POST
    @Path("print")
    public CloudEvent print(CloudEvent event) {
        log.info("Received cloud event of type "+event.getType());
        log.info(new String(event.getData()));
        return event;
    }

    @POST
    @Path("print/object")
    public NewOrder print(NewOrder event) {
        log.info("Received object " + event.toString());
        return event;
    }

    @GET
    @Path("send/{itemId}/{quantity}")
    public Response sendevent(@PathParam("itemId") String itemId, @PathParam("quantity") Integer quant) throws JsonParseException, JsonMappingException, IOException {

        NewOrder neworder = new NewOrder();
        neworder.setItemId(itemId);
        neworder.setQuantity(quant);

        CloudEvent ce = CloudEventBuilder.v1()
            .withId(UUID.randomUUID().toString())
            .withType(NewOrder.newOrderEventType)
            .withSource(URI.create("resteasy.example.client"))
            .withData("application/json", mapper.writeValueAsBytes(neworder))
            .build();

        Response res = ResteasyClientBuilder.newClient().target("http://localhost:8082/print")

            //magic happening here
            .register(OutboundCloudEventDataValidator.class)
        
        
            .request()
            .buildPost(Entity.entity(ce, "application/json"))
            .invoke();
        if (res.getStatus() == 400) {
            return res;
        }
        return Response.status(res.getStatus()).build();

    }
   
}