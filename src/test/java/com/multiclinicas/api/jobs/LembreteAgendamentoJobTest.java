package com.multiclinicas.api.jobs;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.multiclinicas.api.models.Agendamento;
import com.multiclinicas.api.models.Medico;
import com.multiclinicas.api.models.Paciente;
import com.multiclinicas.api.models.enums.StatusAgendamento;
import com.multiclinicas.api.repositories.AgendamentoRepository;
import com.multiclinicas.api.services.EmailService;

@ExtendWith(MockitoExtension.class)
public class LembreteAgendamentoJobTest {

    @InjectMocks
    private LembreteAgendamentoJob lembreteJob;

    @Mock
    private AgendamentoRepository agendamentoRepository;

    @Mock
    private EmailService emailService;

    @Test
    void deveDispararEmailParaAgendamentosDeAmanha() {
        LocalDate amanha = LocalDate.now().plusDays(1);
        
        Paciente paciente = new Paciente();
        paciente.setNome("João Silva");
        paciente.setEmail("joao@teste.com");

        Medico medico = new Medico();
        medico.setNome("Dr. House");

        Agendamento agendamento = new Agendamento();
        agendamento.setPaciente(paciente);
        agendamento.setMedico(medico);
        agendamento.setDataConsulta(amanha);
        agendamento.setHoraInicio(LocalTime.of(10, 0));
        agendamento.setStatus(StatusAgendamento.AGENDADO);

        when(agendamentoRepository.findByDataConsultaAndStatus(amanha, StatusAgendamento.AGENDADO))
                .thenReturn(Arrays.asList(agendamento));

        lembreteJob.dispararLembretesDeConsulta();

        verify(emailService, times(1)).enviarEmail(eq("joao@teste.com"), anyString(), anyString());
    }

    @Test
    void naoDeveDispararEmailSeNaoHouverAgendamentos() {
        LocalDate amanha = LocalDate.now().plusDays(1);
        when(agendamentoRepository.findByDataConsultaAndStatus(amanha, StatusAgendamento.AGENDADO))
                .thenReturn(Collections.emptyList());

        lembreteJob.dispararLembretesDeConsulta();

        verify(emailService, never()).enviarEmail(anyString(), anyString(), anyString());
    }
}