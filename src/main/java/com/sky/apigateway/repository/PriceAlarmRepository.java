package com.sky.apigateway.repository;

import com.sky.apigateway.model.PriceAlarm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PriceAlarmRepository extends JpaRepository<PriceAlarm, Long> {
    List<PriceAlarm> findByUserId(int userId);
}
