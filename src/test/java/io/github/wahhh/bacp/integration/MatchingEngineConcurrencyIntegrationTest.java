package io.github.wahhh.bacp.integration;

import io.github.wahhh.bacp.dto.request.OrderCreateRequest;
import io.github.wahhh.bacp.service.trade.MatchingEngine;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Stresses the matcher under real MySQL + Redis + Redisson with concurrent takers on one symbol.
 */
@Slf4j
class MatchingEngineConcurrencyIntegrationTest extends AbstractTestcontainersIntegrationTest {

    @Autowired
    private MatchingEngine matchingEngine;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long ethId;

    private Long usdtId;

    private final List<Long> userIds = new ArrayList<>();

    @BeforeEach
    void seedUsersAndBalances() {
        ethId = jdbcTemplate.queryForObject(
                "SELECT id FROM t_currency WHERE symbol='ETH' AND deleted=0 ORDER BY id ASC LIMIT 1", Long.class);
        usdtId = jdbcTemplate.queryForObject(
                "SELECT id FROM t_currency WHERE symbol='USDT' AND deleted=0 ORDER BY id ASC LIMIT 1", Long.class);
        userIds.clear();
        for (int i = 0; i < 8; i++) {
            String u = "mce-" + i + "-" + ThreadLocalRandom.current().nextInt(1_000_000);
            jdbcTemplate.update(
                    "INSERT INTO t_sys_user (username, password_hash, status, deleted) VALUES (?, 'x', 1, 0)", u);
            Long uid = jdbcTemplate.queryForObject("SELECT id FROM t_sys_user WHERE username=?", Long.class, u);
            userIds.add(uid);
            jdbcTemplate.update(
                    """
                            INSERT INTO t_balance (user_id, currency_id, available_amount, frozen_amount, version, deleted)
                            VALUES (?, ?, ?, 0, 0, 0)
                            """,
                    uid, usdtId, new BigDecimal("5000000"));
            jdbcTemplate.update(
                    """
                            INSERT INTO t_balance (user_id, currency_id, available_amount, frozen_amount, version, deleted)
                            VALUES (?, ?, ?, 0, 0, 0)
                            """,
                    uid, ethId, new BigDecimal("5000"));
        }
    }

    @Test
    void concurrentLimitOrdersPreserveFillInvariant() throws Exception {
        int threads = 16;
        int perThread = 20;
        String inList = userIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        long t0 = System.nanoTime();
        for (int t = 0; t < threads; t++) {
            final int ft = t;
            futures.add(pool.submit(() -> {
                try {
                    start.await();
                    Random rnd = new Random(911 + ft);
                    Long uid = userIds.get(ft % userIds.size());
                    for (int k = 0; k < perThread; k++) {
                        boolean buy = rnd.nextBoolean();
                        OrderCreateRequest r = new OrderCreateRequest();
                        r.setSymbol("ETH-USDT");
                        r.setSide(buy ? "BUY" : "SELL");
                        r.setOrderType("LIMIT");
                        BigDecimal px = buy
                                ? new BigDecimal("1800").add(new BigDecimal(rnd.nextInt(400)))
                                : new BigDecimal("2200").subtract(new BigDecimal(rnd.nextInt(400)));
                        r.setPrice(px.max(new BigDecimal("1500")).min(new BigDecimal("2500")));
                        r.setQuantity(new BigDecimal("0.01").add(new BigDecimal(rnd.nextInt(50)).multiply(new BigDecimal("0.001"))));
                        matchingEngine.placeOrder(uid, r);
                    }
                } catch (Exception ex) {
                    throw new IllegalStateException(ex);
                }
            }));
        }
        start.countDown();
        for (Future<?> f : futures) {
            f.get(10, TimeUnit.MINUTES);
        }
        pool.shutdown();
        long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
        log.info("Concurrent matcher done in {} ms (threads={} each={})", ms, threads, perThread);

        Integer bad = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FROM t_order
                        WHERE symbol='ETH-USDT' AND user_id IN (%s) AND filled_quantity > quantity
                        """.formatted(inList),
                Integer.class);
        assertEquals(0, bad);

        BigDecimal buyVol = jdbcTemplate.queryForObject(
                """
                        SELECT COALESCE(SUM(filled_quantity),0) FROM t_order
                        WHERE symbol='ETH-USDT' AND side='BUY' AND user_id IN (%s)
                        """.formatted(inList),
                BigDecimal.class);
        BigDecimal sellVol = jdbcTemplate.queryForObject(
                """
                        SELECT COALESCE(SUM(filled_quantity),0) FROM t_order
                        WHERE symbol='ETH-USDT' AND side='SELL' AND user_id IN (%s)
                        """.formatted(inList),
                BigDecimal.class);
        assertTrue(buyVol.subtract(sellVol).abs().compareTo(new BigDecimal("0.00000001")) <= 0,
                () -> "buy filled sum should match sell filled sum: buy=" + buyVol + " sell=" + sellVol);

        BigDecimal minAv = jdbcTemplate.queryForObject(
                """
                        SELECT MIN(available_amount) FROM t_balance WHERE user_id IN (%s)
                        """.formatted(inList),
                BigDecimal.class);
        BigDecimal minFr = jdbcTemplate.queryForObject(
                """
                        SELECT MIN(frozen_amount) FROM t_balance WHERE user_id IN (%s)
                        """.formatted(inList),
                BigDecimal.class);
        assertTrue(minAv.compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(minFr.compareTo(BigDecimal.ZERO) >= 0);
    }
}
