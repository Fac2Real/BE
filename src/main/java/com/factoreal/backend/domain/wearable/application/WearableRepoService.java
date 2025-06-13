package com.factoreal.backend.domain.wearable.application;

import com.factoreal.backend.domain.wearable.dao.WearableRepository;
import com.factoreal.backend.domain.wearable.entity.Wearable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WearableRepoService {
    private final WearableRepository wearableRepository;

    public Wearable saveWearable(Wearable wearable) {
        return wearableRepository.save(wearable);
    }
}
