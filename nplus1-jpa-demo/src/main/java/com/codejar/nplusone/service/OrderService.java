package com.codejar.nplusone.service;

import com.codejar.nplusone.config.QueryCounter;
import com.codejar.nplusone.dto.DemoResult;
import com.codejar.nplusone.dto.OrderSummary;
import com.codejar.nplusone.entity.Order;
import com.codejar.nplusone.entity.OrderItem;
import com.codejar.nplusone.repository.OrderRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private static final int BATCH_SIZE = 50;

    private final OrderRepository orderRepository;
    private final EntityManager entityManager;

    public OrderService(OrderRepository orderRepository, EntityManager entityManager) {
        this.orderRepository = orderRepository;
        this.entityManager = entityManager;
    }

    /**
     * MASALAH: N+1 query.
     * 1 query untuk semua order, lalu 1 query per order saat getItems() dipakai.
     * 100 order = 101 query.
     */
    public DemoResult nPlusOne() {
        return measure("n-plus-one",
                "findAll() lalu getItems() di dalam loop",
                () -> toSummaries(orderRepository.findAll()));
    }

    /**
     * SOLUSI 1: JOIN FETCH. Semua data diambil dalam 1 query.
     */
    public DemoResult joinFetch() {
        return measure("join-fetch",
                "select o from Order o join fetch o.items",
                () -> toSummaries(orderRepository.findAllWithJoinFetch()));
    }

    /**
     * SOLUSI 2: @EntityGraph. Sama seperti JOIN FETCH, 1 query.
     */
    public DemoResult entityGraph() {
        return measure("entity-graph",
                "@EntityGraph(attributePaths = \"items\")",
                () -> toSummaries(orderRepository.findAllWithEntityGraph()));
    }

    /**
     * SOLUSI 3: Batch fetching.
     * Kodenya sama persis dengan nPlusOne(), tapi Hibernate mengambil items
     * secara borongan (50 order sekaligus) pakai "where order_id in (...)".
     * 100 order = 1 + 2 = 3 query.
     *
     * Catatan: di project beneran, biasanya cukup pasang satu baris di
     * application.properties:
     *   spring.jpa.properties.hibernate.default_batch_fetch_size=50
     * Di demo ini diatur per session supaya endpoint /n-plus-one tetap
     * menunjukkan masalah aslinya dan bisa dibandingkan.
     */
    public DemoResult batchFetch() {
        return measure("batch-fetch",
                "findAll() + batch size " + BATCH_SIZE,
                () -> {
                    entityManager.unwrap(Session.class).setFetchBatchSize(BATCH_SIZE);
                    return toSummaries(orderRepository.findAll());
                });
    }

    /**
     * SOLUSI 4: DTO projection. 1 query, dan hitungannya dikerjakan database.
     */
    public DemoResult dtoProjection() {
        return measure("dto-projection",
                "select new OrderSummary(...) + group by",
                orderRepository::findAllSummaries);
    }

    // ------------------------------------------------------------------

    /**
     * Mengubah entity jadi ringkasan. Di sinilah getItems() dipanggil untuk
     * setiap order, dan di sinilah N+1 terjadi kalau items belum di-load.
     */
    private List<OrderSummary> toSummaries(List<Order> orders) {
        return orders.stream()
                .map(order -> {
                    List<OrderItem> items = order.getItems(); // <- pemicu lazy loading
                    BigDecimal total = items.stream()
                            .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new OrderSummary(order.getId(), order.getCustomerName(), (long) items.size(), total);
                })
                .sorted(Comparator.comparing(OrderSummary::id))
                .toList();
    }

    /**
     * Menjalankan satu strategi sambil menghitung jumlah query dan durasinya.
     */
    private DemoResult measure(String strategy, String description, Supplier<List<OrderSummary>> action) {
        QueryCounter.reset();
        long start = System.nanoTime();

        List<OrderSummary> orders = action.get();

        long durationMs = (System.nanoTime() - start) / 1_000_000;
        return new DemoResult(strategy, description, QueryCounter.get(), durationMs, orders.size(), orders);
    }
}
