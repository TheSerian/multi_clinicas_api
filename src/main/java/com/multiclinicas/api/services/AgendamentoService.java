package com.multiclinicas.api.services;

import java.util.List;

import com.multiclinicas.api.dtos.AgendamentoDTO;
import com.multiclinicas.api.dtos.AgendamentoCreateDTO;
import com.multiclinicas.api.dtos.AgendamentoRemarcarDTO;
import com.multiclinicas.api.dtos.AgendamentoStatusDTO;
import com.multiclinicas.api.dtos.AgendamentoTokenDTO;
import com.multiclinicas.api.models.Agendamento;

public interface AgendamentoService {
    List<Agendamento> findAllByClinicId(Long clinicId);

    List<AgendamentoDTO> buscarMeusAgendamentos();

    Agendamento findByIdAndClinicId(Long id, Long clinicId);

    Agendamento create(Long clinicId, AgendamentoCreateDTO dto);

    Agendamento remarcar(Long id, Long clinicId, AgendamentoRemarcarDTO dto);

    Agendamento cancelar(Long id, Long clinicId, boolean canceladoPeloClinica);

    Agendamento atualizarStatus(Long id, Long clinicId, AgendamentoStatusDTO dto);
    
    Agendamento atualizarToken(Long id, Long clinicId, AgendamentoTokenDTO dto);
}