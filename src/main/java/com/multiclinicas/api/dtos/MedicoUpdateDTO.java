package com.multiclinicas.api.dtos;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para atualização de médico via PUT.
 * CPF e CRM são imutáveis após o cadastro, por isso não são aceitos neste DTO.
 * O Service não os atualiza mesmo que fossem enviados.
 */
public record MedicoUpdateDTO(
		@NotBlank(message = "O nome é obrigatório") String nome,
		@NotBlank(message = "É necessário adicionar pelo menos um número de telefone") String telefone,
		@NotNull(message = "A duração da consulta é obrigatória") Integer duracaoConsulta,
		@NotNull(message = "É necessário adicionar pelo menos uma especialidade") Set<Long> especialidadeIds,
		String telefoneSecundario,
		Boolean ativo) {

}
