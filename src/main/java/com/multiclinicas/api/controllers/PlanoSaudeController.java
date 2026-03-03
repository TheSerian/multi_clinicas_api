package com.multiclinicas.api.controllers;

import com.multiclinicas.api.config.tenant.TenantContext;
import com.multiclinicas.api.dtos.PlanoSaudeCreateDTO;
import com.multiclinicas.api.dtos.PlanoSaudeDTO;
import com.multiclinicas.api.mappers.PlanoSaudeMapper;
import com.multiclinicas.api.models.PlanoSaude;
import com.multiclinicas.api.services.PlanoSaudeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/planos-saude")
@Tag(name = "Planos de Saúde", description = "Gestão de convênios e planos de saúde")
@ApiResponses(value = {
        @ApiResponse(responseCode = "401", description = "Não Autenticado (Token ausente ou inválido)"),
        @ApiResponse(responseCode = "403", description = "Não Autorizado (Sem permissão de acesso ou Tenant inativo)")
})
public class PlanoSaudeController {

    private final PlanoSaudeService planoSaudeService;
    private final PlanoSaudeMapper planoSaudeMapper;

    public PlanoSaudeController(PlanoSaudeService planoSaudeService, PlanoSaudeMapper planoSaudeMapper) {
        this.planoSaudeService = planoSaudeService;
        this.planoSaudeMapper = planoSaudeMapper;
    }

    @GetMapping
    public ResponseEntity<List<PlanoSaudeDTO>> findAll() {
        Long clinicId = TenantContext.getClinicId();
        List<PlanoSaude> planos = planoSaudeService.findAllByClinicId(clinicId);
        return ResponseEntity.ok(planos.stream()
                .map(planoSaudeMapper::toDTO)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlanoSaudeDTO> findById(@PathVariable Long id) {
        Long clinicId = TenantContext.getClinicId();
        PlanoSaude plano = planoSaudeService.findByIdAndClinicId(id, clinicId);
        return ResponseEntity.ok(planoSaudeMapper.toDTO(plano));
    }

    @PostMapping
    public ResponseEntity<PlanoSaudeDTO> create(@RequestBody @Valid PlanoSaudeCreateDTO dto) {
        Long clinicId = TenantContext.getClinicId();
        PlanoSaude plano = planoSaudeMapper.toEntity(dto);
        PlanoSaude createdPlano = planoSaudeService.create(clinicId, plano);
        return ResponseEntity.status(HttpStatus.CREATED).body(planoSaudeMapper.toDTO(createdPlano));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlanoSaudeDTO> update(@PathVariable Long id,
            @RequestBody @Valid PlanoSaudeCreateDTO dto) {
        Long clinicId = TenantContext.getClinicId();
        PlanoSaude plano = planoSaudeMapper.toEntity(dto);
        PlanoSaude updatedPlano = planoSaudeService.update(id, clinicId, plano);
        return ResponseEntity.ok(planoSaudeMapper.toDTO(updatedPlano));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Long clinicId = TenantContext.getClinicId();
        planoSaudeService.delete(id, clinicId);
        return ResponseEntity.noContent().build();
    }
}
