package com.multiclinicas.api.controllers;

import java.util.List;

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
import com.multiclinicas.api.dtos.UsuarioAdminCreateDTO;
import com.multiclinicas.api.dtos.UsuarioAdminDTO;
import com.multiclinicas.api.mappers.UsuarioAdminMapper;
import com.multiclinicas.api.models.UsuarioAdmin;
import com.multiclinicas.api.services.UsuarioAdminService;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/usuario-admin")
@RequiredArgsConstructor
@Tag(name = "Usuários Administrativos", description = "Gestão de usuários administrativos da clínica")
@ApiResponses(value = {
        @ApiResponse(responseCode = "401", description = "Não Autenticado (Token ausente ou inválido)"),
        @ApiResponse(responseCode = "403", description = "Não Autorizado (Sem permissão de acesso ou Tenant inativo)")
})
public class UsuarioAdminController {

    private final UsuarioAdminService usuarioAdminService;
    private final UsuarioAdminMapper usuarioAdminMapper;

    @GetMapping()
    public ResponseEntity<List<UsuarioAdminDTO>> findAll() {
        Long clinicId = TenantContext.getClinicId();
        List<UsuarioAdmin> usuarios = usuarioAdminService.findAllByClinicId(clinicId);
        return ResponseEntity.ok(usuarios.stream().map(usuarioAdminMapper::toDTO).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioAdminDTO> findById(@PathVariable Long id) {
        Long clinicId = TenantContext.getClinicId();
        UsuarioAdmin usuarioAdmin = usuarioAdminService.findByIdAndClinicId(id, clinicId);
        return ResponseEntity.ok(usuarioAdminMapper.toDTO(usuarioAdmin));
    }

    @PostMapping
    public ResponseEntity<UsuarioAdminDTO> create(@RequestBody @Valid UsuarioAdminCreateDTO dto) {
        Long clinicID = TenantContext.getClinicId();
        UsuarioAdmin usuario = usuarioAdminMapper.toEntity(dto);
        UsuarioAdmin createUsuario = usuarioAdminService.createUsuarioAdmin(clinicID, usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioAdminMapper.toDTO(createUsuario));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioAdminDTO> update(@PathVariable Long id,
            @RequestBody @Valid UsuarioAdminCreateDTO dto) {
        Long clinicId = TenantContext.getClinicId();
        UsuarioAdmin usuario = usuarioAdminMapper.toEntity(dto);
        UsuarioAdmin updatedUsuario = usuarioAdminService.updateUsuarioAdmin(id, clinicId, usuario);
        return ResponseEntity.ok(usuarioAdminMapper.toDTO(updatedUsuario));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Long clinicId = TenantContext.getClinicId();
        usuarioAdminService.delete(id, clinicId);
        return ResponseEntity.noContent().build();
    }

}
