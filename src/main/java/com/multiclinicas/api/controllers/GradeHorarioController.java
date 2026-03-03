package com.multiclinicas.api.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.multiclinicas.api.config.tenant.TenantContext;
import com.multiclinicas.api.dtos.GradeHorarioCreateDTO;
import com.multiclinicas.api.dtos.GradeHorarioDTO;
import com.multiclinicas.api.mappers.GradeHorarioMapper;
import com.multiclinicas.api.models.GradeHorario;
import com.multiclinicas.api.services.GradeHorarioService;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/grade-horario")
@RequiredArgsConstructor
@Tag(name = "Grade de Horários", description = "Gestão de disponibilidade dos médicos")
@ApiResponses(value = {
        @ApiResponse(responseCode = "401", description = "Não Autenticado (Token ausente ou inválido)"),
        @ApiResponse(responseCode = "403", description = "Não Autorizado (Sem permissão de acesso ou Tenant inativo)")
})
public class GradeHorarioController {

    private final GradeHorarioService gradeHorarioService;
    private final GradeHorarioMapper gradeHorarioMapper;

    @GetMapping
    public ResponseEntity<List<GradeHorarioDTO>> findAll() {
        Long clinicId = TenantContext.getClinicId();
        List<GradeHorario> grades = gradeHorarioService.findAllByClinicId(clinicId);
        return ResponseEntity.ok(grades.stream().map(gradeHorarioMapper::toDTO).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GradeHorarioDTO> findById(@PathVariable Long id) {
        Long clinicId = TenantContext.getClinicId();
        GradeHorario grade = gradeHorarioService.findByIdAndClinicId(id, clinicId);
        return ResponseEntity.ok(gradeHorarioMapper.toDTO(grade));
    }

    @PostMapping
    public ResponseEntity<GradeHorarioDTO> create(@RequestBody @Valid GradeHorarioCreateDTO dto) {
        Long clinicId = TenantContext.getClinicId();
        GradeHorario grade = gradeHorarioMapper.toEntity(dto);
        GradeHorario createdGrade = gradeHorarioService.create(clinicId, dto.medicoId(), grade);
        return ResponseEntity.status(HttpStatus.CREATED).body(gradeHorarioMapper.toDTO(createdGrade));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Long clinicId = TenantContext.getClinicId();
        gradeHorarioService.delete(id, clinicId);
        return ResponseEntity.noContent().build();
    }
}