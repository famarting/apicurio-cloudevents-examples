package io.famartin.cloudevents;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.worldturner.medeia.api.StringSchemaSource;
import com.worldturner.medeia.api.jackson.MedeiaJacksonApi;
import com.worldturner.medeia.schema.validation.SchemaValidator;

import org.everit.json.schema.FormatValidator;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

import io.apicurio.registry.client.RegistryRestClient;
import io.apicurio.registry.rest.beans.ArtifactMetaData;
import io.apicurio.registry.utils.IoUtil;
import io.cloudevents.CloudEvent;
import io.cloudevents.SpecVersion;
import io.cloudevents.rw.CloudEventAttributesWriter;
import io.cloudevents.rw.CloudEventExtensionsWriter;
import io.cloudevents.rw.CloudEventRWException;
import io.cloudevents.rw.CloudEventReader;
import io.cloudevents.rw.CloudEventWriter;
import io.cloudevents.rw.CloudEventWriterFactory;

public class JsonSchemaMiddleware {

    protected static MedeiaJacksonApi api = new MedeiaJacksonApi();
    protected static ObjectMapper mapper = new ObjectMapper();

    private RegistryRestClient registryClient;
    private CloudEventSchemaCache<Schema> schemaCache;
    private CloudEventSchemaCache<SchemaValidator> schemaValidatorCache;

    public JsonSchemaMiddleware(RegistryRestClient registryClient) {
        this.registryClient = registryClient;
    }

    public <T> T readData(CloudEvent cloudevent, Class<T> clazz) {
        if (cloudevent.getData() == null) {
            return null;
        }

        try {
            DataSchemaEntry<SchemaValidator> dataschema = getSchemaValidatorCache().getSchema(cloudevent);
            SchemaValidator schemaValidator = dataschema.getSchema();
            

            JsonParser parser = mapper.getFactory().createParser(cloudevent.getData());
            parser = api.decorateJsonParser(schemaValidator, parser);

            return mapper.readValue(parser, clazz);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public <T> CloudEvent writeData(CloudEvent cloudevent, T data) {
        if (data == null) {
            return cloudevent;
        }

        try {
            DataSchemaEntry<SchemaValidator> dataschema = getSchemaValidatorCache().getSchema(cloudevent);
            SchemaValidator schemaValidator = dataschema.getSchema();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            JsonGenerator generator = mapper.getFactory().createGenerator(baos);
            generator = api.decorateJsonGenerator(schemaValidator, generator);

            mapper.writeValue(generator, data);

            return new CloudEventDelegate(cloudevent, "application/json", baos.toByteArray(), dataschema.getDataSchema());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void validateData(CloudEvent cloudevent) {
        if (cloudevent.getData() == null) {
            return;
        }

        DataSchemaEntry<Schema> dataschema = getSchemaCache().getSchema(cloudevent);
        Schema schema = dataschema.getSchema();

        schema.validate(new JSONObject(new JSONTokener(new ByteArrayInputStream(cloudevent.getData()))));
    }

    private synchronized CloudEventSchemaCache<Schema> getSchemaCache() {
        if (schemaCache == null) {
            schemaCache = new CloudEventSchemaCache<Schema>(registryClient) {
                @Override
                protected Schema toSchema(InputStream rawSchema) {
                    JSONObject parsedSchema = new JSONObject(new JSONTokener(rawSchema));

                    return SchemaLoader.builder()
                            .schemaJson(parsedSchema)
                            .build()
                            .load()
                            .build();
                }
            };
        }
        return schemaCache;
    }

    private synchronized CloudEventSchemaCache<SchemaValidator> getSchemaValidatorCache() {
        if (schemaValidatorCache == null) {
            schemaValidatorCache = new CloudEventSchemaCache<SchemaValidator>(registryClient) {
                @Override
                protected SchemaValidator toSchema(InputStream rawSchema) {
                    return api.loadSchema(new StringSchemaSource(IoUtil.toString(rawSchema)));
                }
            };
        }
        return schemaValidatorCache;
    }

    public static class CloudEventDelegate implements CloudEvent, CloudEventReader {

        private CloudEvent delegate;
        private String datacontenttype;
        private byte[] data;
        private URI dataschema;

        public CloudEventDelegate(CloudEvent delegate, String datacontenttype, byte[] data, String dataschema) {
            this.delegate = delegate;
            this.datacontenttype = datacontenttype;
            this.data = data;
            this.dataschema = URI.create(dataschema);
        }

        @Override
        public SpecVersion getSpecVersion() {
            return delegate.getSpecVersion();
        }

        @Override
        public String getId() {
            return delegate.getId();
        }

        @Override
        public String getType() {
            return delegate.getType();
        }

        @Override
        public URI getSource() {
            return delegate.getSource();
        }

        @Override
        public String getDataContentType() {
            return datacontenttype;
        }

        @Override
        public URI getDataSchema() {
            return dataschema;
        }

        @Override
        public String getSubject() {
            return delegate.getSubject();
        }

        @Override
        public OffsetDateTime getTime() {
            return delegate.getTime();
        }

        @Override
        public Object getAttribute(String attributeName) throws IllegalArgumentException {
            return delegate.getAttribute(attributeName);
        }

        @Override
        public Object getExtension(String extensionName) {
            return delegate.getExtension(extensionName);
        }

        @Override
        public Set<String> getExtensionNames() {
            return delegate.getExtensionNames();
        }

        @Override
        public byte[] getData() {
            return data;
        }

        @Override
        public <T extends CloudEventWriter<V>, V> V read(CloudEventWriterFactory<T, V> writerFactory) throws CloudEventRWException, IllegalStateException {
            CloudEventWriter<V> visitor = writerFactory.create(this.getSpecVersion());
            this.readAttributes(visitor);
            this.readExtensions(visitor);

            if (this.data != null) {
                return visitor.end(this.data);
            }

            return visitor.end();
        }

        @Override
        public void readAttributes(CloudEventAttributesWriter writer) throws CloudEventRWException {
            writer.withAttribute(
                    "id",
                    this.getId()
                );
                writer.withAttribute(
                    "source",
                    this.getSource()
                );
                writer.withAttribute(
                    "type",
                    this.getType()
                );
                if (this.datacontenttype != null) {
                    writer.withAttribute(
                        "datacontenttype",
                        this.datacontenttype
                    );
                }
                if (this.getDataSchema() != null) {
                    writer.withAttribute(
                        "dataschema",
                        this.getDataSchema()
                    );
                }
                if (this.getSubject() != null) {
                    writer.withAttribute(
                        "subject",
                        this.getSubject()
                    );
                }
                if (this.getTime() != null) {
                    writer.withAttribute(
                        "time",
                        this.getTime()
                    );
                }
        }

        @Override
        public void readExtensions(CloudEventExtensionsWriter visitor) throws CloudEventRWException {
            // TODO to be improved
            for (String extensionName : this.getExtensionNames()) {
                Object extension = getExtension(extensionName);
                if (extension instanceof String) {
                    visitor.withExtension(extensionName, (String) extension);
                } else if (extension instanceof Number) {
                    visitor.withExtension(extensionName, (Number) extension);
                } else if (extension instanceof Boolean) {
                    visitor.withExtension(extensionName, (Boolean) extension);
                } else {
                    // This should never happen because we build that map only through our builders
                    throw new IllegalStateException("Illegal value inside extensions map: " + extension);
                }
            }
        }

    }


        // private InputStream lookupSchema(CloudEvent event) {
    //     if (event.getDataSchema() != null && event.getDataSchema().toString().startsWith("/apicurio")) {
    //         String[] apicurioArtifactTokens = event.getDataSchema().toString().split("/");
    //         String artifactId = null;
    //         if (apicurioArtifactTokens.length > 1) {
    //             artifactId = apicurioArtifactTokens[1];
    //         }
    //         String version = null;
    //         if (apicurioArtifactTokens.length > 2) {
    //             version = apicurioArtifactTokens[2];
    //         }
    //         if (artifactId == null) {
    //             throw new IllegalStateException("Bad apicurio dataschema URI");
    //         }
    //         long globalId;
    //         if (version == null) {
    //             globalId = registryClient.getArtifactMetaData(artifactId).getGlobalId();
    //             //!!! we could just call this and retrieve global id from response headers, if we include it for sure
    //             // return registryClient.getLatestArtifact(artifactId);
    //         } else {
    //             if (version.length() > 1 && version.toLowerCase().startsWith("v")) {
    //                 version = version.substring(1);
    //             }
    //             Integer artifactVersion = Integer.parseInt(version);
    //             globalId = registryClient.getArtifactVersionMetaData(artifactId, artifactVersion).getGlobalId();
    //             //!!! we could just call this and retrieve global id from response headers, if we include it for sure
    //             // return registryClient.getArtifactVersion(artifactId, artifactVersion);
    //         }
    //         return registryClient.getArtifactByGlobalId(globalId);
    //     } else if (event.getDataSchema() != null && event.getDataSchema().toString().startsWith("apicurio-global-id-")) {
    //         String apicurioGlobalId = event.getDataSchema().toString().substring("apicurio-global-id-".length());
    //         long globalId = Long.parseLong(apicurioGlobalId);
    //         return registryClient.getArtifactByGlobalId(globalId);
    //     } else {
    //         String artifactId = event.getType();
    //         long globalId = registryClient.getArtifactMetaData(artifactId).getGlobalId();
    //         return registryClient.getArtifactByGlobalId(globalId);
    //         // return registryClient.getLatestArtifact(event.getType());
    //     }
    // }


}