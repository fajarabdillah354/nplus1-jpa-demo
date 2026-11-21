package com.codejar.nplusone.config;

import com.codejar.nplusone.entity.Order;
import com.codejar.nplusone.entity.OrderItem;
import com.codejar.nplusone.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Mengisi database dengan data contoh saat aplikasi start.
 * Default: 100 order, masing-masing 3 item.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final String[] PRODUCTS = {"Keyboard", "Mouse", "Monitor", "Headset", "Webcam"};

    private final OrderRepository orderRepository;
    private final int orderCount;
    private final int itemsPerOrder;

    public DataSeeder(OrderRepository orderRepository,
                      @Value("${demo.order-count}") int orderCount,
                      @Value("${demo.items-per-order}") int itemsPerOrder) {
        this.orderRepository = orderRepository;
        this.orderCount = orderCount;
        this.itemsPerOrder = itemsPerOrder;
    }

    @Override
    public void run(String... args) {
        List<Order> orders = new ArrayList<>();
        for (int i = 1; i <= orderCount; i++) {
            Order order = new Order("Customer " + i);
            for (int j = 0; j < itemsPerOrder; j++) {
                String product = PRODUCTS[(i + j) % PRODUCTS.length];
                BigDecimal price = BigDecimal.valueOf(50_000L * (j + 1));
                order.addItem(new OrderItem(product, j + 1, price));
            }
            orders.add(order);
        }
        orderRepository.saveAll(orders);
    }
}
