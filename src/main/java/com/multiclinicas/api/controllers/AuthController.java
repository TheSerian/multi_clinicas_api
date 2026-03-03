package com.multiclinicas.api.controllers;

import com.multiclinicas.api.dtos.LoginRequestDTO;
import com.multiclinicas.api.dtos.LoginResponseDTO;
import com.multiclinicas.api.exceptions.BusinessException;
import com.multiclinicas.api.models.UsuarioAdmin;
import com.multiclinicas.api.repositories.UsuarioAdminRepository;
import com.multiclinicas.api.services.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import com.multiclinicas.api.models.enums.Role;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints de login e geração de token")
public class AuthController {

    private final UsuarioAdminRepository usuarioAdminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Operation(summary = "Realizar login", description = "Autentica o usuário e retorna o token JWT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login efetuado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest,
            HttpServletRequest request) {
        String clinicIdHeader = request.getHeader("X-Clinic-ID");
        UsuarioAdmin user;

        if (clinicIdHeader != null && !clinicIdHeader.isBlank()) {
            try {
                Long targetClinicId = Long.parseLong(clinicIdHeader);
                // Busca o usuário estritamente vinculado à clínica informada
                user = usuarioAdminRepository.findByEmailAndClinicaId(loginRequest.email(), targetClinicId)
                        .orElseThrow(() -> new BusinessException("Credenciais inválidas"));

                // GAP 2 — Bloqueia login se a clínica estiver inativa (inadimplência /
                // cancelamento)
                if (user.getClinica() != null && !user.getClinica().getAtivo()) {
                    throw new BusinessException("Acesso negado. A assinatura desta clínica está inativa.");
                }

                // Isolamento de Tenant no Login:
                // Um Super Admin não pode logar pelas rotas/subdomínios de Clínicas
                if (user.getRole() == Role.SUPER_ADMIN) {
                    throw new BusinessException("Credenciais inválidas");
                }
            } catch (NumberFormatException e) {
                throw new BusinessException("Credenciais inválidas");
            }
        } else {
            // Sem header de clínica (Login do Painel Master):
            // Busca apenas o usuário global cujo clinic_id é null
            user = usuarioAdminRepository.findByEmailAndClinicaIsNull(loginRequest.email())
                    .orElseThrow(() -> new BusinessException("Credenciais inválidas"));

            // Apenas SUPER_ADMIN pode usar a rota de login global para acessar o master
            // dashboard.
            if (user.getRole() != Role.SUPER_ADMIN) {
                throw new BusinessException("Credenciais inválidas");
            }
        }

        if (!passwordEncoder.matches(loginRequest.senha(), user.getSenhaHash())) {
            throw new BusinessException("Credenciais inválidas");
        }

        Long clinicId = user.getClinica() != null ? user.getClinica().getId() : null;

        String token = jwtService.generateToken(user.getId(), user.getRole().name(), clinicId);

        LoginResponseDTO response = new LoginResponseDTO(
                token,
                user.getId(),
                user.getNome(),
                user.getRole().name(),
                clinicId);

        return ResponseEntity.ok(response);
    }
}
