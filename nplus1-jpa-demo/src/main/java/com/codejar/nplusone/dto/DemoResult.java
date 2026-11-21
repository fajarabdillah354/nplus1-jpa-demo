package com.codejar.nplusone.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Hasil satu kali percobaan: strategi apa, berapa query, berapa lama.
 */
@JsonInclude(JsonInclude.Include.NON_NULL) // field yang null tidak ikut ditampilkan
public record DemoResult(
        String strategy,
        String description,
        long queryCount,
        long durationMs,
        int orderCount,
        List<OrderSummary> orders) {

    /** Versi tanpa daftar order, dipakai di endpoint /compare supaya ringkas. */
    public DemoResult withoutOrders() {
        return new DemoResult(strategy, description, queryCount, durationMs, orderCount, null);
    }
}
