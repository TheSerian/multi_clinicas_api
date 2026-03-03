package com.multiclinicas.api.controllers;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.multiclinicas.api.config.tenant.TenantContext;
import com.multiclinicas.api.dtos.MedicoCreateDTO;
import com.multiclinicas.api.dtos.MedicoDTO;
import com.multiclinicas.api.dtos.MedicoUpdateDTO;
import com.multiclinicas.api.mappers.MedicoMapper;
import com.multiclinicas.api.models.Medico;
import com.multiclinicas.api.services.MedicoService;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/medicos")
@RequiredArgsConstructor
@Tag(name = "Médicos", description = "Gestão de médicos da clínica")
@ApiResponses(value = {
        @ApiResponse(responseCode = "401", description = "Não Autenticado (Token ausente ou inválido)"),
        @ApiResponse(responseCode = "403", description = "Não Autorizado (Sem permissão de acesso ou Tenant inativo)")
})
public class MedicoController {

    private final MedicoService medicoService;
    private final MedicoMapper medicoMapper;

    @GetMapping
    public ResponseEntity<List<MedicoDTO>> findAll() {

        Long clinicId = TenantContext.getClinicId();

        List<Medico> medicos = medicoService.findAllByClinicId(clinicId);

        List<MedicoDTO> dtos = medicos.stream()
                .map(medicoMapper::toDTO)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/ativos")
    public ResponseEntity<List<MedicoDTO>> findAllActive() {

        Long clinicId = TenantContext.getClinicId();

        List<Medico> medicos = medicoService.findAllActiveByClinicId(clinicId);

        List<MedicoDTO> dtos = medicos.stream()
                .map(medicoMapper::toDTO)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicoDTO> findById(@PathVariable Long id) {

        Long clinicId = TenantContext.getClinicId();

        Medico medico = medicoService.findByIdAndClinicId(id, clinicId);

        return ResponseEntity.ok(medicoMapper.toDTO(medico));
    }

    @PostMapping
    public ResponseEntity<MedicoDTO> create(@RequestBody @Valid MedicoCreateDTO dto) {

        Long clinicId = TenantContext.getClinicId();

        Medico medico = medicoMapper.toEntity(dto);

        Set<Long> especialidadeIds = dto.especialidadeIds();

        Medico createdMedico = medicoService.create(clinicId, medico, especialidadeIds);

        return ResponseEntity.status(HttpStatus.CREATED).body(medicoMapper.toDTO(createdMedico));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MedicoDTO> update(@PathVariable Long id,
            @RequestBody @Valid MedicoUpdateDTO dto) {
        Long clinicId = TenantContext.getClinicId();

        Medico medicoAtualizado = medicoMapper.toEntity(dto);

        Set<Long> especialidadeIds = dto.especialidadeIds();

        Medico updatedMedico = medicoService.update(id, clinicId, medicoAtualizado, especialidadeIds);

        return ResponseEntity.ok(medicoMapper.toDTO(updatedMedico));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Long clinicId = TenantContext.getClinicId();

        medicoService.delete(id, clinicId);

        return ResponseEntity.noContent().build();
    }
}