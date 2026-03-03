package com.multiclinicas.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ClinicaCreateDTO(
        @Schema(description = "Nome fantasia da clínica", example = "Clínica Saúde Total") @NotBlank(message = "O nome fantasia é obrigatório") String nomeFantasia,

        @Schema(description = "Subdomínio para acesso (apenas letras minúsculas, números e hífens)", example = "saude-total") @NotBlank(message = "O subdomínio é obrigatório") @Pattern(regexp = "^[a-z0-9-]+$", message = "Subdomínio deve conter apenas letras minúsculas, números e hífens") String subdominio,

        @Schema(description = "Status inicial da clínica", example = "true", defaultValue = "true") Boolean ativo,

        @Schema(description = "Nome do gestor administrador", example = "Administrador") @NotBlank(message = "O nome do gestor é obrigatório") String adminNome,

        @Schema(description = "E-mail do gestor", example = "admin@clinica.com") @NotBlank(message = "O e-mail do gestor é obrigatório") @Email(message = "E-mail inválido") String adminEmail,

        @Schema(description = "Senha do gestor", example = "123456") @NotBlank(message = "A senha do gestor é obrigatória") String adminSenha) {
}
