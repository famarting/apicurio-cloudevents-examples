package io.famartin.eventing;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Set;

import io.cloudevents.CloudEvent;
import io.cloudevents.SpecVersion;

public class CloudEventAdapter implements CloudEvent {

    private io.famartin.eventing.CloudEvent<?> delegate;

    public CloudEventAdapter(io.famartin.eventing.CloudEvent<?> delegate) {
        this.delegate = delegate;
    }

    @Override
    public SpecVersion getSpecVersion() {
        return SpecVersion.parse(delegate.specVersion());
    }

    @Override
    public String getId() {
        return delegate.id();
    }

    @Override
    public String getType() {
        return delegate.type();
    }

    @Override
    public URI getSource() {
        return URI.create(delegate.source());
    }

    @Override
    public String getDataContentType() {
        return "application/json"; //TODO fix
    }

    @Override
    public URI getDataSchema() {
        return URI.create(delegate.dataschema());
    }

    @Override
    public String getSubject() {
        return delegate.subject();
    }

    @Override
    public OffsetDateTime getTime() {
        return delegate.time();
    }

    @Override
    public Object getAttribute(String attributeName) throws IllegalArgumentException {
        return null;
    }

    @Override
    public Object getExtension(String extensionName) {
        return null;
    }

    @Override
    public Set<String> getExtensionNames() {
        return Collections.emptySet();
    }

    @Override
    public byte[] getData() {
        return null;
    }

}