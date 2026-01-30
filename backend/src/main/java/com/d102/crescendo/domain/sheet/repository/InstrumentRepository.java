package com.d102.crescendo.domain.sheet.repository;

import com.d102.crescendo.domain.sheet.entity.Instrument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstrumentRepository extends JpaRepository<Instrument, Integer> {
}