package com.multiclinicas.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
        @Schema(description = "E-mail do usuário", example = "admin@multiclinicas.com") @NotBlank(message = "O e-mail é obrigatório") @Email(message = "E-mail inválido") String email,

        @Schema(description = "Senha do usuário", example = "123456") @NotBlank(message = "A senha é obrigatória") String senha) {
}
