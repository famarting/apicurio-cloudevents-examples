package io.famartin.eventing;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jboss.logging.Logger;

import io.apicurio.registry.client.RegistryRestClientFactory;
import io.famartin.cloudevents.JsonSchemaMiddleware;
import io.quarkus.funqy.Context;
import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEvent;
import io.quarkus.funqy.knative.events.CloudEventMapping;

//this example is not fully functional because ideally schema validation functionalities should be contributed into quarkus funqy extension
public class OrdersProcessor {
    private static final Logger log = Logger.getLogger(OrdersProcessor.class);

    // workaround for demo purposes
    private ObjectMapper mapper = new ObjectMapper();

    @Funq
    @CloudEventMapping(trigger = NewOrder.newOrderEventType, responseSource = "orders-service", responseType = ProcessedOrder.processedOrderEventType)
    public ProcessedOrder process(@Context CloudEvent eventInfo, Object eventData) throws JsonProcessingException {

        // workaround for demo purposes, this could be embedded into quarkus functionality, or just find some other way to provide this functionality

        byte[] eventDataBytes = mapper.writeValueAsBytes(eventData);

        JsonSchemaMiddleware middleware = new JsonSchemaMiddleware(RegistryRestClientFactory.create("http://localhost:8080/api"));

        // adapt quarkus CloudEvent to CloudEvent SDK
        io.cloudevents.CloudEvent cloudevent = new CloudEventAdapter(NewOrder.newOrderEventType, eventInfo, "application/json", eventDataBytes);

        NewOrder neworder = middleware.readData(cloudevent, NewOrder.class);

        //

        ProcessedOrder order = new ProcessedOrder();
        order.setOrderId(UUID.randomUUID().toString());
        log.info("Processing order "+order.getOrderId());
        order.setItemId(neworder.getItemId());
        order.setQuantity(neworder.getQuantity());

        //dummy operation
        int newquantity = order.getQuantity() + 1;

        order.setProcessingTimestamp(Instant.now().toString());
        order.setProcessedBy("orders-service");
        order.setApproved(true);

        //

        // something like this could be implemented in quarkus, to validate the output adheres to the schema
        io.cloudevents.CloudEvent output = new CloudEventAdapter(ProcessedOrder.processedOrderEventType, null, null, null);
        io.cloudevents.CloudEvent outputEvent = middleware.writeData(output, order);

        //

        return order;
    }

}
