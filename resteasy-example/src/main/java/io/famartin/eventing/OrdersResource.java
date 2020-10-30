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
 * This example shows the explicit usage of apicurio cloudevents serdes library
 * 
 */
@ApplicationScoped
@Path("/")
public class OrdersResource {

    private static final Logger log = Logger.getLogger(OrdersResource.class);

    private ObjectMapper mapper = new ObjectMapper();

    @POST
    public CloudEvent newOrder(CloudEvent event) throws JsonParseException, JsonMappingException, IOException {
        log.info("Received cloud event of type "+event.getType());
        log.info(new String(event.getData()));

        //

        JsonSchemaMiddleware middleware = new JsonSchemaMiddleware(RegistryRestClientFactory.create("http://localhost:8080/api"));

        NewOrder neworder = middleware.readData(event, NewOrder.class);

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

        //

        CloudEvent outputEvent = middleware.writeData(processedOrderEvent, order);

        //

        return outputEvent;
    }

    @POST
    @Path("novalidate")
    public CloudEvent newOrderNoValidate(CloudEvent event) throws JsonParseException, JsonMappingException, IOException {
        log.info("Received cloud event of type "+event.getType());
        log.info(new String(event.getData()));

        NewOrder neworder = mapper.readValue(event.getData(), NewOrder.class);

        ProcessedOrder order = new ProcessedOrder();
        order.setOrderId(UUID.randomUUID().toString());
        log.info("Processing order "+order.getOrderId());
        order.setItemId(neworder.getItemId());
        order.setQuantity(neworder.getQuantity());
        order.setProcessingTimestamp(Instant.now().toString());
        order.setProcessedBy("orders-service");
        order.setApproved(true);

        CloudEvent orderEvent = CloudEventBuilder.v1()
            .withId(UUID.randomUUID().toString())
            .withSource(URI.create("orders-service"))
            .withType(ProcessedOrder.processedOrderEventType)
            // .withData(dataContentType, dataSchema, data) //option
            .withData("application/json", mapper.writeValueAsBytes(order))
            .build();

        return orderEvent;
    }

    @POST
    @Path("nocloudevent")
    public ProcessedOrder newOrderNoCloudEvent(CloudEvent event) throws JsonParseException, JsonMappingException, IOException {
        log.info("Received cloud event of type "+event.getType());
        log.info(new String(event.getData()));

        NewOrder neworder = mapper.readValue(event.getData(), NewOrder.class);

        ProcessedOrder order = new ProcessedOrder();
        order.setOrderId(UUID.randomUUID().toString());
        log.info("Processing order "+order.getOrderId());
        order.setItemId(neworder.getItemId());
        order.setQuantity(neworder.getQuantity());
        order.setProcessingTimestamp(Instant.now().toString());
        order.setProcessedBy("orders-service");
        order.setApproved(true);
        return order;
    }

    
}