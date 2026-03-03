package com.multiclinicas.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponseDTO(
        @Schema(description = "Token JWT de acesso", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...") String token,

        @Schema(description = "ID do usuário logado", example = "1") Long id,

        @Schema(description = "Nome do usuário", example = "Administrador") String nome,

        @Schema(description = "Função/Role do usuário", example = "ADMIN") String role,

        @Schema(description = "ID da clínica associada (pode ser nulo para SUPER_ADMIN)", example = "1") Long clinicId
) {
}
