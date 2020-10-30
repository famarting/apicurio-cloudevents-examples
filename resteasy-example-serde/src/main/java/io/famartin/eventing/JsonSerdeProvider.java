package io.famartin.eventing;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.annotation.Priority;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import io.apicurio.registry.client.RegistryRestClientFactory;
import io.apicurio.registry.utils.IoUtil;
import io.cloudevents.core.message.MessageReader;
import io.cloudevents.http.restful.ws.impl.RestfulWSMessageFactory;
import io.cloudevents.http.restful.ws.impl.RestfulWSMessageWriter;
import io.famartin.cloudevents.JsonSchemaMiddleware;
import io.famartin.cloudevents.ParsedData;

@Provider
@Consumes(MediaType.WILDCARD)
@Produces(MediaType.WILDCARD)
@Priority(10)
public class JsonSerdeProvider
        implements MessageBodyReader<CloudEvent<?>>, MessageBodyWriter<CloudEvent<?>>, ClientRequestFilter {

    JsonSchemaMiddleware middleware = new JsonSchemaMiddleware(
            RegistryRestClientFactory.create("http://localhost:8080/api"));

    @Override
    public void filter(ClientRequestContext ctx) throws IOException {
        Object entity = ctx.getEntity();
        if (entity != null && CloudEventImpl.class.isAssignableFrom(entity.getClass())) {
            System.out.println("Filtering!");
        }
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return CloudEvent.class.isAssignableFrom(type);
    }

    @Override
    public CloudEvent<?> readFrom(Class<CloudEvent<?>> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        MessageReader reader = RestfulWSMessageFactory.create(mediaType, httpHeaders, IoUtil.toBytes(entityStream));
        io.cloudevents.CloudEvent cloudevent = reader.toEvent();

        ParameterizedType pt = (ParameterizedType) genericType;
        Class<?> typeClass = (Class<?>) pt.getActualTypeArguments()[0];

        ParsedData<?> outputParsedData = middleware.readParsedData(cloudevent, typeClass);

        return new CloudEventImpl(cloudevent, outputParsedData.datacontenttype, outputParsedData.data);
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return CloudEvent.class.isAssignableFrom(type);
    }

    @Override
    public void writeTo(CloudEvent<?> event, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {

        CloudEventAdapter adapter = new CloudEventAdapter(event);

        io.cloudevents.CloudEvent outputevent = middleware.writeData(adapter, event.data());

        new RestfulWSMessageWriter(httpHeaders, entityStream).writeBinary(outputevent);

    }

}