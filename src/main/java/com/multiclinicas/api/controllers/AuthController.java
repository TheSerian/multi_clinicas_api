package com.multiclinicas.api.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.multiclinicas.api.dtos.LoginRequestDTO;
import com.multiclinicas.api.dtos.LoginResponseDTO;
import com.multiclinicas.api.exceptions.BusinessException;
import com.multiclinicas.api.models.Paciente;
import com.multiclinicas.api.models.UsuarioAdmin;
import com.multiclinicas.api.models.enums.Role;
import com.multiclinicas.api.repositories.PacienteRepository;
import com.multiclinicas.api.repositories.UsuarioAdminRepository;
import com.multiclinicas.api.services.JwtService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints de login e geração de token")
public class AuthController {

    private final UsuarioAdminRepository usuarioAdminRepository;
    private final PacienteRepository pacienteRepository;
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

        Long userId;
        String userName;
        String userRole;
        String senhaHash;
        Long clinicId = null;

        if (clinicIdHeader != null && !clinicIdHeader.isBlank()) {
            try {
                Long targetClinicId = Long.parseLong(clinicIdHeader);
                var adminOpt = usuarioAdminRepository.findByEmailAndClinicaId(loginRequest.email(), targetClinicId);

                if (adminOpt.isPresent()) {
                    UsuarioAdmin user = adminOpt.get();
                    if (user.getClinica() != null && !user.getClinica().getAtivo()) {
                        throw new BusinessException("Acesso negado. A assinatura desta clínica está inativa.");
                    }
                    if (user.getRole() == Role.SUPER_ADMIN) {
                        throw new BusinessException("Credenciais inválidas");
                    }
                    userId = user.getId();
                    userName = user.getNome();
                    userRole = user.getRole().name();
                    senhaHash = user.getSenhaHash();
                    clinicId = user.getClinica().getId();
                } else {
                    Paciente paciente = pacienteRepository.findByEmailAndClinicaId(loginRequest.email(), targetClinicId)
                            .orElseThrow(() -> new BusinessException("Credenciais inválidas"));
                    userId = paciente.getId();
                    userName = paciente.getNome();
                    userRole = "PACIENTE";
                    senhaHash = paciente.getSenhaHash();
                    clinicId = paciente.getClinica().getId();
                }
            } catch (NumberFormatException e) {
                throw new BusinessException("Credenciais inválidas");
            }
        } else {
            UsuarioAdmin user = usuarioAdminRepository.findByEmailAndClinicaIsNull(loginRequest.email())
                    .orElseThrow(() -> new BusinessException("Credenciais inválidas"));

            if (user.getRole() != Role.SUPER_ADMIN) {
                throw new BusinessException("Credenciais inválidas");
            }
            userId = user.getId();
            userName = user.getNome();
            userRole = user.getRole().name();
            senhaHash = user.getSenhaHash();
        }

        if (!passwordEncoder.matches(loginRequest.senha(), senhaHash)) {
            throw new BusinessException("Credenciais inválidas");
        }

        String token = jwtService.generateToken(userId, userRole, clinicId);

        LoginResponseDTO response = new LoginResponseDTO(
                token,
                userId,
                userName,
                userRole,
                clinicId);

        return ResponseEntity.ok(response);
    }
}