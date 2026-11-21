package com.codejar.nplusone.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders") // "order" itu kata kunci SQL, jadi tabelnya dinamai "orders"
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerName;

    // @OneToMany itu LAZY secara default.
    // Artinya: items BELUM diambil saat Order di-load.
    // Hibernate baru query ke database saat getItems() benar-benar dipakai.
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {
        // dibutuhkan JPA
    }

    public Order(String customerName) {
        this.customerName = customerName;
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public Long getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public List<OrderItem> getItems() {
        return items;
    }
}
