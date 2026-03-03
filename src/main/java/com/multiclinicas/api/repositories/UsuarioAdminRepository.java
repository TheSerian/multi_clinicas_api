package com.multiclinicas.api.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.multiclinicas.api.models.UsuarioAdmin;

@Repository
public interface UsuarioAdminRepository extends JpaRepository<UsuarioAdmin, Long> {
    List<UsuarioAdmin> findAllByClinicaId(Long clinicId);

    Optional<UsuarioAdmin> findByIdAndClinicaId(Long id, Long clinicId);

    Optional<UsuarioAdmin> findByEmailAndClinicaId(String email, Long clinicId);

    Optional<UsuarioAdmin> findByEmailAndClinicaIsNull(String email);
}
