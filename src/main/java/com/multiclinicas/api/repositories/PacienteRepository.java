package com.multiclinicas.api.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.multiclinicas.api.models.Paciente;

@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    List<Paciente> findAllByClinicaId(Long clinicaId);

    Optional<Paciente> findByIdAndClinicaId(Long id, Long clinicaId);

    Optional<Paciente> findByEmailAndClinicaId(String email, Long clinicaId);

    boolean existsByIdAndClinicaId(Long id, Long clinicaId);

    void deleteByIdAndClinicaId(Long id, Long clinicaId);
}
