package com.multiclinicas.api.dtos;

import java.util.Set;

public record MedicoDTO (
			Long id,
			String nome,
			String cpf,
			String crm,
			Long clinicId,
			String nomeClinica, //nome fantasia da clinica
			String telefone,
			String telefoneSecundario,
			Integer duracaoConsulta,
			Set<String> especialidades,
			Boolean ativo
		){

}