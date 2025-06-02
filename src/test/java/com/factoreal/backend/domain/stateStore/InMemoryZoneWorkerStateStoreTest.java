package com.factoreal.backend.domain.stateStore;

import com.factoreal.backend.messaging.kafka.strategy.enums.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryZoneWorkerStateStoreTest {

    private InMemoryZoneWorkerStateStore store;
    private static final String ZONE_A = "zone-A";
    private static final String ZONE_B = "zone-B";
    private static final String W1 = "worker-1";
    private static final String W2 = "worker-2";

    @BeforeEach
    void setUp() {
        store = new InMemoryZoneWorkerStateStore();
    }

    @Test
    @DisplayName("새로운 존은 기본 위험 등급이 INFO 이다")
    void defaultZoneRiskIsInfo() {
        assertThat(store.getZoneRiskLevel(ZONE_A)).isEqualTo(RiskLevel.INFO);
    }

    @Nested
    @DisplayName("setWorkerRiskLevel 동작")
    class SetWorkerRiskLevel {

        @Test
        @DisplayName("단일 워커 위험 등급을 설정하면 존 위험 등급이 동일해진다")
        void singleWorkerSetsZoneRisk() {
            store.setWorkerRiskLevel(ZONE_A, W1, RiskLevel.WARNING);

            assertThat(store.getWorkerRiskLevel(W1)).isEqualTo(RiskLevel.WARNING);
            assertThat(store.getZoneRiskLevel(ZONE_A)).isEqualTo(RiskLevel.WARNING);
        }

        @Test
        @DisplayName("더 높은 위험 등급의 워커가 추가되면 존 위험 등급이 상승한다")
        void addHigherRiskRaisesZoneRisk() {
            store.setWorkerRiskLevel(ZONE_A, W1, RiskLevel.WARNING);
            store.setWorkerRiskLevel(ZONE_A, W2, RiskLevel.CRITICAL);

            assertThat(store.getZoneRiskLevel(ZONE_A)).isEqualTo(RiskLevel.CRITICAL);
        }

        @Test
        @DisplayName("워커 위험 등급이 낮아지면 카운트가 갱신되고 존 위험 등급도 재평가된다")
        void lowerWorkerRiskUpdatesCounts() {
            // W1 WARNING, W2 CRITICAL → 존 == CRITICAL
            store.setWorkerRiskLevel(ZONE_A, W1, RiskLevel.WARNING);
            store.setWorkerRiskLevel(ZONE_A, W2, RiskLevel.CRITICAL);
            assertThat(store.getZoneRiskLevel(ZONE_A)).isEqualTo(RiskLevel.CRITICAL);

            // W2를 INFO로 다운그레이드 → 남아있는 최고 등급은 WARNING
            store.setWorkerRiskLevel(ZONE_A, W2, RiskLevel.INFO);

            assertThat(store.getWorkerRiskLevel(W2)).isEqualTo(RiskLevel.INFO);
            assertThat(store.getZoneRiskLevel(ZONE_A)).isEqualTo(RiskLevel.WARNING);
        }
    }

    @Nested
    @DisplayName("moveWorkerRiskLevel 동작")
    class MoveWorkerRiskLevel {

        @Test
        @DisplayName("워커를 이동시키면 이전 워커 카운트 감소, 새 워커 카운트 증가")
        void moveRiskLevelBetweenWorkers() {
            store.setWorkerRiskLevel(ZONE_A, W1, RiskLevel.CRITICAL);
            assertThat(store.getWorkerRiskLevel(W1)).isEqualTo(RiskLevel.CRITICAL);
            assertThat(store.getZoneRiskLevel(ZONE_A)).isEqualTo(RiskLevel.CRITICAL);

            // ZONE_A → ZONE_B 이동
            store.setWorkerRiskLevel(ZONE_B, W1, RiskLevel.WARNING);
            assertThat(store.getZoneRiskLevel(ZONE_A)).isEqualTo(RiskLevel.INFO);
            assertThat(store.getZoneRiskLevel(ZONE_B)).isEqualTo(RiskLevel.WARNING);
        }
    }
}
