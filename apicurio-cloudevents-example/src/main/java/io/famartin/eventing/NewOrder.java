package io.famartin.eventing;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class NewOrder {
    
    public static final String newOrderEventType = "io.famartin.new-order";

    private String itemId;
    private Integer quantity;

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

}