package com.echosnap.backend.domain.residence.repository;

import com.echosnap.backend.domain.residence.entity.Residence;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResidenceRepository
    extends JpaRepository<Residence, Long> {
}