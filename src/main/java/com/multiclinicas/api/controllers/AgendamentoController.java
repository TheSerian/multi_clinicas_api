package com.multiclinicas.api.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.multiclinicas.api.config.tenant.TenantContext;
import com.multiclinicas.api.dtos.AgendamentoCreateDTO;
import com.multiclinicas.api.dtos.AgendamentoDTO;
import com.multiclinicas.api.dtos.AgendamentoRemarcarDTO;
import com.multiclinicas.api.dtos.AgendamentoStatusDTO;
import com.multiclinicas.api.mappers.AgendamentoMapper;
import com.multiclinicas.api.models.Agendamento;
import com.multiclinicas.api.services.AgendamentoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/agendamentos")
@RequiredArgsConstructor
@Tag(name = "Agendamentos", description = "Gestão de marcação de consultas")
@ApiResponses(value = {
        @ApiResponse(responseCode = "401", description = "Não Autenticado (Token ausente ou inválido)"),
        @ApiResponse(responseCode = "403", description = "Não Autorizado (Sem permissão de acesso ou Tenant inativo)")
})
public class AgendamentoController {

    private final AgendamentoService agendamentoService;
    private final AgendamentoMapper agendamentoMapper;

    @Operation(summary = "Listar agendamentos", description = "Lista todos os agendamentos da clínica")
    @GetMapping
    public ResponseEntity<List<AgendamentoDTO>> findAll() {
        Long clinicId = TenantContext.getClinicId();
        List<Agendamento> agendamentos = agendamentoService.findAllByClinicId(clinicId);
        List<AgendamentoDTO> dtos = agendamentos.stream().map(agendamentoMapper::toDTO).toList();
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Buscar agendamento por ID", description = "Retorna um agendamento específico")
    @GetMapping("/{id}")
    public ResponseEntity<AgendamentoDTO> findById(@PathVariable Long id) {
        Long clinicId = TenantContext.getClinicId();
        Agendamento agendamento = agendamentoService.findByIdAndClinicId(id, clinicId);
        return ResponseEntity.ok(agendamentoMapper.toDTO(agendamento));
    }

    @Operation(summary = "Criar agendamento", description = "Agenda uma consulta validando disponibilidade e regras da clínica")
    @PostMapping
    public ResponseEntity<AgendamentoDTO> create(@RequestBody @Valid AgendamentoCreateDTO dto) {
        Long clinicId = TenantContext.getClinicId();
        Agendamento agendamento = agendamentoService.create(clinicId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(agendamentoMapper.toDTO(agendamento));
    }

    @Operation(summary = "Remarcar agendamento", description = "Altera a data e hora de um agendamento existente")
    @PutMapping("/{id}/remarcar")
    public ResponseEntity<AgendamentoDTO> remarcar(
            @PathVariable Long id,
            @RequestBody @Valid AgendamentoRemarcarDTO dto) {
        Long clinicId = TenantContext.getClinicId();
        Agendamento agendamento = agendamentoService.remarcar(id, clinicId, dto);
        return ResponseEntity.ok(agendamentoMapper.toDTO(agendamento));
    }

    @Operation(summary = "Cancelar agendamento", description = "Cancela um agendamento. Use canceladoPelaClinica=true para cancelamento pela clínica")
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<AgendamentoDTO> cancelar(
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean canceladoPelaClinica) {
        Long clinicId = TenantContext.getClinicId();
        Agendamento agendamento = agendamentoService.cancelar(id, clinicId, canceladoPelaClinica);
        return ResponseEntity.ok(agendamentoMapper.toDTO(agendamento));
    }

    @Operation(summary = "Atualizar status", description = "Atualiza o status do agendamento (confirmar, realizar, faltou)")
    @PatchMapping("/{id}/status")
    public ResponseEntity<AgendamentoDTO> atualizarStatus(
            @PathVariable Long id,
            @RequestBody @Valid AgendamentoStatusDTO dto) {
        Long clinicId = TenantContext.getClinicId();
        Agendamento agendamento = agendamentoService.atualizarStatus(id, clinicId, dto);
        return ResponseEntity.ok(agendamentoMapper.toDTO(agendamento));
    }
}
