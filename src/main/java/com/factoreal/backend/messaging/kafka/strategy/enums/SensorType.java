package com.factoreal.backend.messaging.kafka.strategy.enums;

public enum SensorType {
    current,
    dust,
    temp,
    humid,
    vibration,
    power,
    pressure,
    voc;
    public static SensorType getSensorType(String sensorType){
        return SensorType.valueOf(sensorType);
    }
}