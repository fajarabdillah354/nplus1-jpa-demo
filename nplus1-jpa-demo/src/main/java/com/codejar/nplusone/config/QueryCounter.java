package com.codejar.nplusone.config;

import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * Menghitung berapa SQL yang dikirim Hibernate ke database.
 *
 * Hibernate memanggil inspect() setiap kali mau menjalankan SQL, jadi di sini
 * kita cukup menambah angka. Hitungannya per thread (ThreadLocal), jadi tiap
 * request punya hitungan sendiri.
 *
 * Didaftarkan lewat application.properties:
 * spring.jpa.properties.hibernate.session_factory.statement_inspector
 */
public class QueryCounter implements StatementInspector {

    private static final ThreadLocal<Long> COUNT = ThreadLocal.withInitial(() -> 0L);

    @Override
    public String inspect(String sql) {
        COUNT.set(COUNT.get() + 1);
        return sql; // SQL-nya tidak diubah
    }

    public static void reset() {
        COUNT.set(0L);
    }

    public static long get() {
        return COUNT.get();
    }
}
