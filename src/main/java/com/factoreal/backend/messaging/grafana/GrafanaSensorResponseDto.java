package com.factoreal.backend.messaging.grafana;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GrafanaSensorResponseDto {
    private final String sensorId;
    private final String sensorType;
    private final String iframeUrl;
}
