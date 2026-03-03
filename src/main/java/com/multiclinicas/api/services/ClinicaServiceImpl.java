package com.multiclinicas.api.services;

import com.multiclinicas.api.exceptions.ResourceNotFoundException;
import com.multiclinicas.api.models.Clinica;
import com.multiclinicas.api.repositories.ClinicaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import com.multiclinicas.api.exceptions.BusinessException;
import com.multiclinicas.api.exceptions.ResourceConflictException;
import com.multiclinicas.api.models.UsuarioAdmin;
import com.multiclinicas.api.models.enums.Role;
import com.multiclinicas.api.repositories.UsuarioAdminRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
public class ClinicaServiceImpl implements ClinicaService {

    private final ClinicaRepository clinicaRepository;
    private final UsuarioAdminRepository usuarioAdminRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<Clinica> findAll() {
        return clinicaRepository.findAll();
    }

    @Override
    public Clinica findById(Long id) {
        return clinicaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Clínica não encontrada com ID: " + id));
    }

    @Override
    public Clinica findBySubdominio(String subdominio) {
        return clinicaRepository.findBySubdominio(subdominio)
                .orElseThrow(() -> new ResourceNotFoundException("Clínica não encontrada para o subdomínio: " + subdominio));
    }

    @Override
    @Transactional
    public Clinica create(Clinica clinica, String adminNome, String adminEmail, String adminSenha) {
        if (clinicaRepository.existsBySubdominio(clinica.getSubdominio())) {
            throw new ResourceConflictException("Subdomínio já está em uso: " + clinica.getSubdominio());
        }
        Clinica novaClinica = clinicaRepository.save(clinica);

        UsuarioAdmin admin = new UsuarioAdmin();
        admin.setClinica(novaClinica);
        admin.setNome(adminNome);
        admin.setEmail(adminEmail);
        admin.setSenhaHash(passwordEncoder.encode(adminSenha));
        admin.setRole(Role.ADMIN);

        usuarioAdminRepository.save(admin);

        return novaClinica;
    }

    @Override
    @Transactional
    public Clinica update(Long id, Clinica clinicaAtualizada) {
        Clinica clinicaExistente = findById(id);

        if (!clinicaExistente.getSubdominio().equals(clinicaAtualizada.getSubdominio()) &&
                clinicaRepository.existsBySubdominio(clinicaAtualizada.getSubdominio())) {
            throw new BusinessException("Novo subdomínio já está em uso: " + clinicaAtualizada.getSubdominio());
        }

        clinicaExistente.setNomeFantasia(clinicaAtualizada.getNomeFantasia());
        clinicaExistente.setSubdominio(clinicaAtualizada.getSubdominio());
        clinicaExistente.setAtivo(clinicaAtualizada.getAtivo());

        return clinicaRepository.save(clinicaExistente);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!clinicaRepository.existsById(id)) {
            throw new ResourceNotFoundException("Clínica não encontrada para exclusão com ID: " + id);
        }
        clinicaRepository.deleteById(id);
    }
}
