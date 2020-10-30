package io.famartin.eventing;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Set;

import io.cloudevents.CloudEvent;
import io.cloudevents.SpecVersion;

public class CloudEventAdapter implements CloudEvent {

    private String eventType;
    private io.quarkus.funqy.knative.events.CloudEvent delegate;
    private String datacontentType;
    private byte[] data;

    public CloudEventAdapter(String eventType, io.quarkus.funqy.knative.events.CloudEvent delegate, String datacontentType, byte[] data) {
        this.eventType = eventType;
        this.delegate = delegate;
        this.datacontentType = datacontentType;
        this.data = data;
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
        return eventType;
    }

    @Override
    public URI getSource() {
        return URI.create(delegate.source());
    }

    @Override
    public String getDataContentType() {
        return datacontentType;
    }

    @Override
    public URI getDataSchema() {
        return null;
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
        return data;
    }

}