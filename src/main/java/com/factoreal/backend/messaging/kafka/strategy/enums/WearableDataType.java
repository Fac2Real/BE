package com.factoreal.backend.messaging.kafka.strategy.enums;

public enum WearableDataType {
    heartRate;
    public static WearableDataType getWearableType(String wearableDataType) {
        return WearableDataType.valueOf(wearableDataType);
    }
}
