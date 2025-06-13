package com.factoreal.backend.domain.wearable.dao;

import com.factoreal.backend.domain.wearable.entity.Wearable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WearableRepository extends JpaRepository<Wearable, Long> {

}
