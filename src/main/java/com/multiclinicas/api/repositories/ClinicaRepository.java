package com.multiclinicas.api.repositories;

import com.multiclinicas.api.models.Clinica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClinicaRepository extends JpaRepository<Clinica, Long> {
    boolean existsBySubdominio(String subdominio);
    
    Optional<Clinica> findBySubdominio(String subdominio);
}
