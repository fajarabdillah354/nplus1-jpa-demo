package com.codejar.nplusone.repository;

import com.codejar.nplusone.dto.OrderSummary;
import com.codejar.nplusone.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // MASALAH: findAll() bawaan JpaRepository cuma mengambil tabel orders.
    // Items baru diambil saat getItems() dipanggil, satu query per order.

    // SOLUSI 1: JOIN FETCH
    // Order dan items diambil sekaligus dalam satu query pakai JOIN.
    @Query("select o from Order o join fetch o.items")
    List<Order> findAllWithJoinFetch();

    // SOLUSI 2: @EntityGraph
    // Efeknya sama dengan JOIN FETCH, tapi cukup menyebut nama relasinya.
    @EntityGraph(attributePaths = "items")
    @Query("select o from Order o")
    List<Order> findAllWithEntityGraph();

    // SOLUSI 4: DTO projection
    // Tidak mengambil entity sama sekali. Jumlah item dan total harga
    // langsung dihitung di database.
    @Query("""
            select new com.codejar.nplusone.dto.OrderSummary(
                o.id, o.customerName, count(i), sum(i.price * i.quantity))
            from Order o left join o.items i
            group by o.id, o.customerName
            order by o.id
            """)
    List<OrderSummary> findAllSummaries();
}
