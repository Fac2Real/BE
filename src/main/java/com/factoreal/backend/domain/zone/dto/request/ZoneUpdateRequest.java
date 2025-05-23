package com.factoreal.backend.domain.zone.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ZoneUpdateRequest {
    private String zoneName; // 수정할 공간명
}
