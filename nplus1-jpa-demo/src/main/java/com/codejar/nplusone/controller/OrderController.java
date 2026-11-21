package com.codejar.nplusone.controller;

import com.codejar.nplusone.dto.DemoResult;
import com.codejar.nplusone.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/n-plus-one")
    public DemoResult nPlusOne() {
        return orderService.nPlusOne();
    }

    @GetMapping("/join-fetch")
    public DemoResult joinFetch() {
        return orderService.joinFetch();
    }

    @GetMapping("/entity-graph")
    public DemoResult entityGraph() {
        return orderService.entityGraph();
    }

    @GetMapping("/batch-fetch")
    public DemoResult batchFetch() {
        return orderService.batchFetch();
    }

    @GetMapping("/dto-projection")
    public DemoResult dtoProjection() {
        return orderService.dtoProjection();
    }

    /** Menjalankan semua strategi dan menampilkan perbandingannya. */
    @GetMapping("/compare")
    public List<DemoResult> compare() {
        return List.of(
                orderService.nPlusOne().withoutOrders(),
                orderService.joinFetch().withoutOrders(),
                orderService.entityGraph().withoutOrders(),
                orderService.batchFetch().withoutOrders(),
                orderService.dtoProjection().withoutOrders());
    }
}
