package com.multiclinicas.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ClinicaUpdateDTO(
        @Schema(description = "Nome fantasia da clínica", example = "Clínica Saúde Total") @NotBlank(message = "O nome fantasia é obrigatório") String nomeFantasia,

        @Schema(description = "Subdomínio para acesso (apenas letras minúsculas, números e hífens)", example = "saude-total") @NotBlank(message = "O subdomínio é obrigatório") @Pattern(regexp = "^[a-z0-9-]+$", message = "Subdomínio deve conter apenas letras minúsculas, números e hífens") String subdominio,

        @Schema(description = "Status da clínica", example = "true", defaultValue = "true") Boolean ativo) {
}
