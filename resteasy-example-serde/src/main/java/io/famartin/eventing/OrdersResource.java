package io.famartin.eventing;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jboss.logging.Logger;

import io.apicurio.registry.client.RegistryRestClient;
import io.apicurio.registry.client.RegistryRestClientFactory;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.famartin.cloudevents.JsonSchemaMiddleware;

/**
 * 
 * This example shows the implicit usage of apicurio cloudevents serdes library, here we SERIALIZE, DESERIALIZE AND VALIDATE the data
 * 
 */
@ApplicationScoped
@Path("/")
public class OrdersResource {

    private static final Logger log = Logger.getLogger(OrdersResource.class);

    private ObjectMapper mapper = new ObjectMapper();

    @POST
    public io.famartin.eventing.CloudEvent<ProcessedOrder> newOrder(io.famartin.eventing.CloudEvent<NewOrder> event) throws JsonParseException, JsonMappingException, IOException {
        log.info("Received cloud event of type "+event.type());

        NewOrder neworder = event.data();
        //

        ProcessedOrder order = new ProcessedOrder();
        order.setOrderId(UUID.randomUUID().toString());
        log.info("Processing order "+order.getOrderId());
        order.setItemId(neworder.getItemId());
        order.setQuantity(neworder.getQuantity());
        order.setProcessingTimestamp(Instant.now().toString());
        order.setProcessedBy("orders-service");
        order.setApproved(true);

        CloudEvent processedOrderEvent = CloudEventBuilder.v1()
            .withId(UUID.randomUUID().toString())
            .withSource(URI.create("orders-service"))
            .withType(ProcessedOrder.processedOrderEventType)
            .build();

        return new CloudEventImpl<ProcessedOrder>(processedOrderEvent, "/apicurio/"+ProcessedOrder.processedOrderEventType+"/1", order);
    }


}