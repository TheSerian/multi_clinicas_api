package com.multiclinicas.api.mappers;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.multiclinicas.api.dtos.MedicoCreateDTO;
import com.multiclinicas.api.dtos.MedicoDTO;
import com.multiclinicas.api.dtos.MedicoUpdateDTO;
import com.multiclinicas.api.models.Especialidade;
import com.multiclinicas.api.models.Medico;

@Component
public class MedicoMapper {
	
	public MedicoDTO toDTO (Medico medico) {
		if (medico == null) {
			return null;
		}
		return new MedicoDTO(
				medico.getId(),
				medico.getNome(),
				medico.getCpf(),
				medico.getCrm(),
				medico.getClinica().getId(),
				medico.getClinica().getNomeFantasia(),
				medico.getTelefone(),
				medico.getTelefoneSecundario(),
				medico.getDuracaoConsulta(),
				medico.getEspecialidades().stream().map(Especialidade::getNome)
				.collect(Collectors.toSet()),
				medico.getAtivo());
				
	}
	
	public Medico toEntity(MedicoCreateDTO dto) {
        if (dto == null) {
            return null;
        }
        Medico medico = new Medico();
        medico.setNome(dto.nome());
        medico.setCpf(dto.cpf());
        medico.setCrm(dto.crm());
        medico.setTelefone(dto.telefone());
        medico.setDuracaoConsulta(dto.duracaoConsulta());
        medico.setTelefoneSecundario(dto.telefoneSecundario());
        medico.setAtivo(dto.ativo() != null ? dto.ativo() : true);
        return medico;
    }

	public Medico toEntity(MedicoUpdateDTO dto) {
        if (dto == null) {
            return null;
        }
        Medico medico = new Medico();
        medico.setNome(dto.nome());
        medico.setTelefone(dto.telefone());
        medico.setDuracaoConsulta(dto.duracaoConsulta());
        medico.setTelefoneSecundario(dto.telefoneSecundario());
        medico.setAtivo(dto.ativo() != null ? dto.ativo() : true);
        return medico;
    }
	
}