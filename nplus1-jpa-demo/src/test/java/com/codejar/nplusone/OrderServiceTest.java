package com.codejar.nplusone;

import com.codejar.nplusone.dto.DemoResult;
import com.codejar.nplusone.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test ini mengunci jumlah query tiap strategi.
 * Kalau suatu hari ada yang tidak sengaja memunculkan N+1 lagi, test-nya merah.
 *
 * Data: 100 order, masing-masing 3 item (lihat application.properties).
 */
@SpringBootTest
class OrderServiceTest {

    @Autowired
    OrderService orderService;

    @Test
    void nPlusOne_menjalankan101Query() {
        DemoResult result = orderService.nPlusOne();

        assertThat(result.orderCount()).isEqualTo(100);
        assertThat(result.queryCount()).isEqualTo(101); // 1 + N
    }

    @Test
    void joinFetch_cukup1Query() {
        assertThat(orderService.joinFetch().queryCount()).isEqualTo(1);
    }

    @Test
    void entityGraph_cukup1Query() {
        assertThat(orderService.entityGraph().queryCount()).isEqualTo(1);
    }

    @Test
    void batchFetch_cukup3Query() {
        // 1 query order + 2 batch (100 order / batch size 50)
        assertThat(orderService.batchFetch().queryCount()).isEqualTo(3);
    }

    @Test
    void dtoProjection_cukup1Query() {
        assertThat(orderService.dtoProjection().queryCount()).isEqualTo(1);
    }

    @Test
    void semuaStrategiMenghasilkanDataYangSama() {
        var expected = orderService.nPlusOne().orders();

        assertThat(orderService.joinFetch().orders()).usingRecursiveComparison().isEqualTo(expected);
        assertThat(orderService.entityGraph().orders()).usingRecursiveComparison().isEqualTo(expected);
        assertThat(orderService.batchFetch().orders()).usingRecursiveComparison().isEqualTo(expected);
        assertThat(orderService.dtoProjection().orders())
                .usingRecursiveComparison()
                .withComparatorForType(java.math.BigDecimal::compareTo, java.math.BigDecimal.class)
                .isEqualTo(expected);
    }
}
