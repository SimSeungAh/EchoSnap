package com.smartrecycle.backend.domain.residence.repository;

import com.smartrecycle.backend.domain.residence.entity.Residence;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResidenceRepository
    extends JpaRepository<Residence, Long> {
}