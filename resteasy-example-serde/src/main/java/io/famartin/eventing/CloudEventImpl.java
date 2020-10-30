package io.famartin.eventing;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Set;

import io.cloudevents.SpecVersion;

public class CloudEventImpl<T> implements CloudEvent<T> {

    String dataschema;
    T data;

    final io.cloudevents.CloudEvent delegate;

    public CloudEventImpl(io.cloudevents.CloudEvent cloudevent, String dataschema, T data) {
        this.delegate = cloudevent;
        this.dataschema = dataschema;
        this.data = data;
    }

    @Override
    public String id() {
        return delegate.getId();
    }

    @Override
    public String specVersion() {
        return delegate.getSpecVersion().toString();
    }

    @Override
    public String source() {
        return delegate.getSource().toString();
    }

    @Override
    public String subject() {
        return delegate.getSubject();
    }

    @Override
    public OffsetDateTime time() {
        return delegate.getTime();
    }

    @Override
    public String type() {
        return delegate.getType();
    }

    @Override
    public T data() {
        return data;
    }

    @Override
    public String dataschema() {
        return dataschema;
    }
   
}