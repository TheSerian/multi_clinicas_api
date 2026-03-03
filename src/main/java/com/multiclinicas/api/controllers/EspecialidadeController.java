package com.multiclinicas.api.controllers;

import com.multiclinicas.api.config.tenant.TenantContext;
import com.multiclinicas.api.dtos.EspecialidadeCreateDTO;
import com.multiclinicas.api.dtos.EspecialidadeDTO;
import com.multiclinicas.api.mappers.EspecialidadeMapper;
import com.multiclinicas.api.models.Especialidade;
import com.multiclinicas.api.services.EspecialidadeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/especialidades")
@RequiredArgsConstructor
@Tag(name = "Especialidades", description = "Gestão de especialidades médicas da clínica")
@ApiResponses(value = {
        @ApiResponse(responseCode = "401", description = "Não Autenticado (Token ausente ou inválido)"),
        @ApiResponse(responseCode = "403", description = "Não Autorizado (Sem permissão de acesso ou Tenant inativo)")
})
public class EspecialidadeController {

    private final EspecialidadeService especialidadeService;
    private final EspecialidadeMapper especialidadeMapper;

    @GetMapping
    public ResponseEntity<List<EspecialidadeDTO>> findAll() {
        Long clinicId = TenantContext.getClinicId();

        List<Especialidade> especialidades = especialidadeService.findAllByClinicId(clinicId);

        return ResponseEntity.ok(
                especialidades.stream()
                        .map(especialidadeMapper::toDTO)
                        .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EspecialidadeDTO> findById(@PathVariable Long id) {
        Long clinicId = TenantContext.getClinicId();

        Especialidade especialidade = especialidadeService.findByIdAndClinicId(id, clinicId);

        return ResponseEntity.ok(especialidadeMapper.toDTO(especialidade));
    }

    @PostMapping
    public ResponseEntity<EspecialidadeDTO> create(@Valid @RequestBody EspecialidadeCreateDTO dto) {
        Long clinicId = TenantContext.getClinicId();

        Especialidade especialidade = especialidadeMapper.toEntity(dto);

        Especialidade createdEspecialidade = especialidadeService.create(clinicId, especialidade);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(especialidadeMapper.toDTO(createdEspecialidade));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EspecialidadeDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody EspecialidadeCreateDTO dto) {

        Long clinicId = TenantContext.getClinicId();

        Especialidade especialidadeAtualizada = especialidadeMapper.toEntity(dto);

        Especialidade updatedEspecialidade = especialidadeService.update(id, clinicId, especialidadeAtualizada);

        return ResponseEntity.ok(especialidadeMapper.toDTO(updatedEspecialidade));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Long clinicId = TenantContext.getClinicId();

        especialidadeService.delete(id, clinicId);

        return ResponseEntity.noContent().build();
    }
}