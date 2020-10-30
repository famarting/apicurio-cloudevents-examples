package io.famartin.eventing;

import java.time.OffsetDateTime;

public interface CloudEvent<T> {
    
    String id();

    String specVersion();

    String source();

    String subject();

    OffsetDateTime time();

    String type();

    String dataschema();

    T data();

}