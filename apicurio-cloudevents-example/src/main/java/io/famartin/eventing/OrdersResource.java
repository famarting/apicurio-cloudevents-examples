package io.famartin.eventing;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.apicurio.registry.utils.cloudevents.CloudEvent;
import io.apicurio.registry.utils.cloudevents.CloudEventsWsProvider;
import io.cloudevents.core.builder.CloudEventBuilder;

/**
 *
 * This example shows the implicit usage of apicurio cloudevents serdes library, here we SERIALIZE, DESERIALIZE AND VALIDATE the data
 *
 */
@ApplicationScoped
@Path("/")
public class OrdersResource {

    private static final Logger log = Logger.getLogger(OrdersResource.class);

    @GET
    @Path("/purchase/{item-id}/{quantity}")
    public Response start(@PathParam("item-id") String itemId, @PathParam("quantity") Integer quantity) {

        NewOrder order = new NewOrder();
        order.setItemId(itemId);
        order.setQuantity(quantity);

        CloudEvent<NewOrder> ce = CloudEvent.from(
                CloudEventBuilder.v1()
                        .withId(UUID.randomUUID().toString())
                        .withSource(URI.create("orders-service"))
                        .withType(NewOrder.newOrderEventType)
                        .build(),

                "/apicurio/"+NewOrder.newOrderEventType+"/1",

                order);

        Response res = ResteasyClientBuilder.newClient().target("http://localhost:8082/order")

//                //magic happening here
                .register(CloudEventsWsProvider.class)

                .request()
                .buildPost(Entity.entity(ce, "application/json"))
                .invoke();
//            if (res.getStatus() == 400) {
        return res;
//            }

//        return Response.ok().build();
    }

    @POST
    @Path("order")
    public CloudEvent<ProcessedOrder> newOrder(CloudEvent<NewOrder> event) throws JsonParseException, JsonMappingException, IOException {
        log.info("Received cloud event of type "+event.type());

        NewOrder neworder = event.data();

        ProcessedOrder order = new ProcessedOrder();
        order.setOrderId(UUID.randomUUID().toString());
        log.info("Processing order "+order.getOrderId());
        order.setItemId(neworder.getItemId());
        order.setQuantity(neworder.getQuantity());
        order.setProcessingTimestamp(Instant.now().toString());
        order.setProcessedBy("orders-service");
        order.setApproved(true);

        return CloudEvent.from(
                    CloudEventBuilder.v1()
                            .withId(UUID.randomUUID().toString())
                            .withSource(URI.create("orders-service"))
                            .withType(ProcessedOrder.processedOrderEventType)
                            .build(),

                    "/apicurio/"+ProcessedOrder.processedOrderEventType+"/1",

                    order);
    }


}