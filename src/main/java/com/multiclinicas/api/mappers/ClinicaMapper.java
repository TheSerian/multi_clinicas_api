package com.multiclinicas.api.mappers;

import com.multiclinicas.api.dtos.ClinicaCreateDTO;
import com.multiclinicas.api.dtos.ClinicaDTO;
import com.multiclinicas.api.models.Clinica;
import org.springframework.stereotype.Component;

@Component
public class ClinicaMapper {

    public ClinicaDTO toDTO(Clinica clinica) {
        if (clinica == null) {
            return null;
        }
        return new ClinicaDTO(
                clinica.getId(),
                clinica.getNomeFantasia(),
                clinica.getSubdominio(),
                clinica.getAtivo(),
                clinica.getCreatedAt());
    }

    public Clinica toEntity(ClinicaCreateDTO dto) {
        if (dto == null) {
            return null;
        }
        Clinica clinica = new Clinica();
        clinica.setNomeFantasia(dto.nomeFantasia());
        clinica.setSubdominio(dto.subdominio());
        clinica.setAtivo(dto.ativo() != null ? dto.ativo() : true);
        return clinica;
    }

    public Clinica toEntity(com.multiclinicas.api.dtos.ClinicaUpdateDTO dto) {
        if (dto == null) {
            return null;
        }
        Clinica clinica = new Clinica();
        clinica.setNomeFantasia(dto.nomeFantasia());
        clinica.setSubdominio(dto.subdominio());
        clinica.setAtivo(dto.ativo() != null ? dto.ativo() : true);
        return clinica;
    }
}
