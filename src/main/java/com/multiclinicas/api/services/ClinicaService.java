package com.multiclinicas.api.services;

import com.multiclinicas.api.models.Clinica;
import java.util.List;

public interface ClinicaService {
    List<Clinica> findAll();

    Clinica findById(Long id);

    Clinica findBySubdominio(String subdominio);

    Clinica create(Clinica clinica, String adminNome, String adminEmail, String adminSenha);

    Clinica update(Long id, Clinica clinica);

    void delete(Long id);
}
