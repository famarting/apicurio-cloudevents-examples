package io.famartin.cloudevents;

public class DataSchemaEntry <T> {
    
    private String dataSchema;
    private T schema;

    public String getDataSchema() {
        return dataSchema;
    }

    public void setDataSchema(String dataSchema) {
        this.dataSchema = dataSchema;
    }

    public T getSchema() {
        return schema;
    }

    public void setSchema(T schema) {
        this.schema = schema;
    }
    
}