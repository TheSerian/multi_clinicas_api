package com.multiclinicas.api.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.multiclinicas.api.mappers.PacienteMapper;
import com.multiclinicas.api.models.Paciente;
import com.multiclinicas.api.services.PacienteService;
import com.multiclinicas.api.config.tenant.TenantContext;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.multiclinicas.api.dtos.PacienteCreateDTO;
import com.multiclinicas.api.dtos.PacienteDTO;
import org.springframework.http.ResponseEntity;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.HttpStatus;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/pacientes")
@RequiredArgsConstructor
@Tag(name = "Pacientes", description = "Gestão de pacientes da clínica")
@ApiResponses(value = {
        @ApiResponse(responseCode = "401", description = "Não Autenticado (Token ausente ou inválido)"),
        @ApiResponse(responseCode = "403", description = "Não Autorizado (Sem permissão de acesso ou Tenant inativo)")
})
public class PacienteController {

    private final PacienteService pacienteService;
    private final PacienteMapper pacienteMapper;

    @GetMapping
    public ResponseEntity<List<PacienteDTO>> getAllPacientes() {
        Long clinicId = TenantContext.getClinicId();
        List<Paciente> pacientes = pacienteService.findAll(clinicId);
        return ResponseEntity.ok(
                pacientes.stream().map(pacienteMapper::toDto).toList());
    }

    @PostMapping
    public ResponseEntity<PacienteDTO> createPaciente(@Valid @RequestBody PacienteCreateDTO dto) {
        Long clinicaId = TenantContext.getClinicId();
        Paciente paciente = pacienteMapper.toEntity(dto);
        Paciente salvo = pacienteService.create(clinicaId, paciente);

        return ResponseEntity.status(HttpStatus.CREATED).body(pacienteMapper.toDto(salvo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PacienteDTO> getPacienteById(@PathVariable Long id) {
        Long clinicId = TenantContext.getClinicId();
        Paciente paciente = pacienteService.findById(id, clinicId);
        return ResponseEntity.ok(pacienteMapper.toDto(paciente));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PacienteDTO> updatePaciente(
            @PathVariable Long id,
            @Valid @RequestBody PacienteCreateDTO dto) {

        Long clinicId = TenantContext.getClinicId();

        Paciente novos = pacienteMapper.toEntity(dto);

        Paciente atualizado = pacienteService.update(id, novos, clinicId);

        return ResponseEntity.ok(pacienteMapper.toDto(atualizado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePaciente(@PathVariable Long id) {
        Long clinicId = TenantContext.getClinicId();
        pacienteService.delete(id, clinicId);
        return ResponseEntity.noContent().build();
    }
}
