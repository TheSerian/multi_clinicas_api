package com.multiclinicas.api.controllers;

import com.multiclinicas.api.dtos.ClinicaCreateDTO;
import com.multiclinicas.api.dtos.ClinicaUpdateDTO;
import com.multiclinicas.api.dtos.ClinicaDTO;
import com.multiclinicas.api.mappers.ClinicaMapper;
import com.multiclinicas.api.models.Clinica;
import com.multiclinicas.api.services.ClinicaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;

@RestController
@RequestMapping("/clinicas")
@RequiredArgsConstructor
@Tag(name = "Clínicas", description = "Gerenciamento de clínicas do sistema")
public class ClinicaController {

        private final ClinicaService clinicaService;

        private final ClinicaMapper clinicaMapper;

        @Operation(summary = "Listar todas as clínicas", description = "Retorna uma lista de todas as clínicas cadastradas")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Lista recuperada com sucesso")
        })
        @GetMapping
        public ResponseEntity<List<ClinicaDTO>> getAllClinicas() {
                List<Clinica> clinicas = clinicaService.findAll();
                List<ClinicaDTO> dtos = clinicas.stream()
                                .map(clinicaMapper::toDTO).toList();
                return ResponseEntity.ok(dtos);
        }

        @Operation(summary = "Buscar clínica por ID", description = "Retorna os dados de uma clínica específica")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Clínica encontrada"),
                        @ApiResponse(responseCode = "404", description = "Clínica não encontrada")
        })
        @GetMapping("/{id}")
        public ResponseEntity<ClinicaDTO> getClinicaById(@PathVariable Long id) {
                Clinica clinica = clinicaService.findById(id);
                return ResponseEntity.ok(clinicaMapper.toDTO(clinica));
        }

        @Operation(summary = "Buscar clínica por subdomínio", description = "Retorna os dados da clínica baseado no subdomínio da URL")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Clínica encontrada"),
                        @ApiResponse(responseCode = "404", description = "Clínica não encontrada")
        })
        @GetMapping("/subdominio/{subdominio}")
        public ResponseEntity<ClinicaDTO> getClinicaBySubdominio(@PathVariable String subdominio) {
                Clinica clinica = clinicaService.findBySubdominio(subdominio);
                return ResponseEntity.ok(clinicaMapper.toDTO(clinica));
        }

        @Operation(summary = "Criar nova clínica", description = "Cadastra uma nova clínica no sistema")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Clínica criada com sucesso"),
                        @ApiResponse(responseCode = "400", description = "Dados inválidos")
        })
        @PostMapping
        public ResponseEntity<ClinicaDTO> createClinica(@RequestBody @Valid ClinicaCreateDTO clinicaCreateDTO) {
                Clinica clinica = clinicaMapper.toEntity(clinicaCreateDTO);
                Clinica savedClinica = clinicaService.create(clinica, clinicaCreateDTO.adminNome(),
                                clinicaCreateDTO.adminEmail(), clinicaCreateDTO.adminSenha());
                return ResponseEntity.status(HttpStatus.CREATED).body(clinicaMapper.toDTO(savedClinica));
        }

        @Operation(summary = "Atualizar clínica", description = "Atualiza os dados de uma clínica existente")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Clínica atualizada com sucesso"),
                        @ApiResponse(responseCode = "404", description = "Clínica não encontrada"),
                        @ApiResponse(responseCode = "400", description = "Dados inválidos")
        })
        @PutMapping("/{id}")
        public ResponseEntity<ClinicaDTO> updateClinica(@PathVariable Long id,
                        @RequestBody @Valid ClinicaUpdateDTO clinicaUpdateDTO) {
                Clinica clinica = clinicaMapper.toEntity(clinicaUpdateDTO);
                Clinica updatedClinica = clinicaService.update(id, clinica);
                return ResponseEntity.ok(clinicaMapper.toDTO(updatedClinica));
        }

        @Operation(summary = "Excluir clínica", description = "Remove uma clínica do sistema")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "204", description = "Clínica excluída com sucesso"),
                        @ApiResponse(responseCode = "404", description = "Clínica não encontrada")
        })
        @DeleteMapping("/{id}")
        public ResponseEntity<Void> deleteClinica(@PathVariable Long id) {
                clinicaService.delete(id);
                return ResponseEntity.noContent().build();
        }
}
