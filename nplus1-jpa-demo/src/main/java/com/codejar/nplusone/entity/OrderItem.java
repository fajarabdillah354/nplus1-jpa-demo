package com.codejar.nplusone.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productName;

    private int quantity;

    private BigDecimal price;

    // @ManyToOne itu EAGER secara default, jadi di sini sengaja dibuat LAZY.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    protected OrderItem() {
        // dibutuhkan JPA
    }

    public OrderItem(String productName, int quantity, BigDecimal price) {
        this.productName = productName;
        this.quantity = quantity;
        this.price = price;
    }

    void setOrder(Order order) {
        this.order = order;
    }

    public Long getId() {
        return id;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }
}
