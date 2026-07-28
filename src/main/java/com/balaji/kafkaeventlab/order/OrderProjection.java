package com.balaji.kafkaeventlab.order;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
public class OrderProjection {

    @Id
    private UUID orderId;

    private String customerId;
    private String itemSku;
    private Integer quantity;
    private BigDecimal amount;
    private String status;
    private Instant lastUpdatedAt;

    protected OrderProjection() {
    }

    public OrderProjection(
            UUID orderId,
            String customerId,
            String itemSku,
            Integer quantity,
            BigDecimal amount,
            String status,
            Instant lastUpdatedAt
    ) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.itemSku = itemSku;
        this.quantity = quantity;
        this.amount = amount;
        this.status = status;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public void changeStatus(String status, Instant lastUpdatedAt) {
        this.status = status;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getItemSku() {
        return itemSku;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }

    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }
}
