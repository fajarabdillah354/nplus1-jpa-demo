package com.codejar.nplusone.dto;

import java.math.BigDecimal;

/**
 * Ringkasan satu order. Semua strategi menghasilkan data yang sama persis,
 * yang beda cuma jumlah query untuk mendapatkannya.
 */
public record OrderSummary(Long id, String customerName, Long itemCount, BigDecimal total) {
}
