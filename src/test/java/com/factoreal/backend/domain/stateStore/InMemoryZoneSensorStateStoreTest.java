package com.factoreal.backend.domain.stateStore;

import com.factoreal.backend.messaging.kafka.strategy.enums.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InMemoryZoneSensorStateStoreTest {

    private InMemoryZoneSensorStateStore store;
    private final String zoneId = "zoneA";
    private final String sensor1 = "sensor1";
    private final String sensor2 = "sensor2";

    @BeforeEach
    void setUp() {
        store = new InMemoryZoneSensorStateStore(null);
    }

    @Test
    @DisplayName("Default zone risk level is INFO when no sensors set")
    void defaultZoneRiskIsInfo() {
        assertEquals(RiskLevel.INFO, store.getZoneRiskLevel(zoneId));
    }

    @Test
    @DisplayName("Default sensor risk level is INFO when not set")
    void defaultSensorRiskIsInfo() {
        assertEquals(RiskLevel.INFO, store.getSensorRiskLevel(zoneId, sensor1));
    }

    @Test
    @DisplayName("Setting a sensor risk level persists and returns correctly")
    void setAndGetSensorRiskLevel() {
        store.setSensorRiskLevel(zoneId, sensor1, RiskLevel.WARNING);
        assertEquals(RiskLevel.WARNING, store.getSensorRiskLevel(zoneId, sensor1));
    }

    @Test
    @DisplayName("Zone risk level reflects highest sensor level")
    void zoneRiskReflectsHighestSensorLevel() {
        store.setSensorRiskLevel(zoneId, sensor1, RiskLevel.WARNING);
        // Zone should be WARNING since only one WARNING sensor
        assertEquals(RiskLevel.WARNING, store.getZoneRiskLevel(zoneId));

        // Add a CRITICAL sensor
        store.setSensorRiskLevel(zoneId, sensor2, RiskLevel.CRITICAL);
        assertEquals(RiskLevel.CRITICAL, store.getZoneRiskLevel(zoneId));
    }

    @Test
    @DisplayName("Updating sensor risk level adjusts zone counts correctly")
    void updatingSensorRiskAdjustCounts() {
        store.setSensorRiskLevel(zoneId, sensor1, RiskLevel.INFO);
        assertEquals(RiskLevel.INFO, store.getZoneRiskLevel(zoneId));

        // Escalate sensor1 to WARNING
        store.setSensorRiskLevel(zoneId, sensor1, RiskLevel.WARNING);
        assertEquals(RiskLevel.WARNING, store.getZoneRiskLevel(zoneId));

        // Escalate sensor1 to CRITICAL
        store.setSensorRiskLevel(zoneId, sensor1, RiskLevel.CRITICAL);
        assertEquals(RiskLevel.CRITICAL, store.getZoneRiskLevel(zoneId));

        // Downgrade sensor1 back to INFO
        store.setSensorRiskLevel(zoneId, sensor1, RiskLevel.INFO);
        assertEquals(RiskLevel.INFO, store.getZoneRiskLevel(zoneId),
                "After downgrading last sensor, zone risk should return to INFO");
    }
}