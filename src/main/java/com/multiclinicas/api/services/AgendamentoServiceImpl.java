package com.multiclinicas.api.services;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import com.multiclinicas.api.config.tenant.TenantContext;
import com.multiclinicas.api.dtos.AgendamentoDTO;
import com.multiclinicas.api.dtos.AgendamentoCreateDTO;
import com.multiclinicas.api.dtos.AgendamentoRemarcarDTO;
import com.multiclinicas.api.dtos.AgendamentoStatusDTO;
import com.multiclinicas.api.exceptions.BusinessException;
import com.multiclinicas.api.exceptions.ResourceConflictException;
import com.multiclinicas.api.exceptions.ResourceNotFoundException;
import com.multiclinicas.api.mappers.AgendamentoMapper;
import com.multiclinicas.api.models.Agendamento;
import com.multiclinicas.api.models.Clinica;
import com.multiclinicas.api.models.GradeHorario;
import com.multiclinicas.api.models.Medico;
import com.multiclinicas.api.models.Paciente;
import com.multiclinicas.api.models.PlanoSaude;
import com.multiclinicas.api.models.enums.StatusAgendamento;
import com.multiclinicas.api.models.enums.TipoPagamento;
import com.multiclinicas.api.repositories.AgendamentoRepository;
import com.multiclinicas.api.repositories.ClinicaRepository;
import com.multiclinicas.api.repositories.GradeHorarioRepository;
import com.multiclinicas.api.repositories.MedicoRepository;
import com.multiclinicas.api.repositories.PacienteRepository;
import com.multiclinicas.api.repositories.PlanoSaudeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AgendamentoServiceImpl implements AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final ClinicaRepository clinicaRepository;
    private final MedicoRepository medicoRepository;
    private final PacienteRepository pacienteRepository;
    private final PlanoSaudeRepository planoSaudeRepository;
    private final GradeHorarioRepository gradeHorarioRepository;
    private final EmailService emailService;
    private final AgendamentoMapper agendamentoMapper;

    private static final Map<DayOfWeek, String> DIAS_SEMANA_PT = Map.of(
            DayOfWeek.MONDAY, "Segunda-feira",
            DayOfWeek.TUESDAY, "Terça-feira",
            DayOfWeek.WEDNESDAY, "Quarta-feira",
            DayOfWeek.THURSDAY, "Quinta-feira",
            DayOfWeek.FRIDAY, "Sexta-feira",
            DayOfWeek.SATURDAY, "Sábado",
            DayOfWeek.SUNDAY, "Domingo");

    @Override
    @Transactional(readOnly = true)
    public List<Agendamento> findAllByClinicId(Long clinicId) {
        return agendamentoRepository.findAllByClinicaId(clinicId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgendamentoDTO> buscarMeusAgendamentos() {
        Long clinicId = TenantContext.getClinicId();
        if (clinicId == null) {
            throw new AccessDeniedException("Clínica não identificada na requisição.");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new AccessDeniedException("Usuário não autenticado.");
        }

        boolean isPaciente = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_PACIENTE".equals(authority.getAuthority()));
        if (!isPaciente) {
            throw new AccessDeniedException("Acesso permitido apenas para pacientes.");
        }

        Long pacienteId;
        try {
            pacienteId = Long.parseLong(authentication.getPrincipal().toString());
        } catch (NumberFormatException ex) {
            throw new AccessDeniedException("Usuário autenticado inválido.");
        }

        pacienteRepository.findByIdAndClinicaId(pacienteId, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado para esta clínica"));

        return agendamentoRepository.findByPacienteIdAndClinicaIdOrderByDataConsultaDescHoraInicioDesc(pacienteId, clinicId)
                .stream()
                .map(agendamentoMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Agendamento findByIdAndClinicId(Long id, Long clinicId) {
        return agendamentoRepository.findById(id)
                .filter(a -> a.getClinica().getId().equals(clinicId))
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));
    }

    @Override
    @Transactional
    public Agendamento create(Long clinicId, AgendamentoCreateDTO dto) {
        Clinica clinica = clinicaRepository.findById(clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Clínica não encontrada"));

        Paciente paciente = pacienteRepository.findByIdAndClinicaId(dto.pacienteId(), clinicId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Paciente não encontrado ou não pertence a esta clínica"));

        Medico medico = medicoRepository.findByIdAndClinicaId(dto.medicoId(), clinicId);
        if (medico == null) {
            throw new ResourceNotFoundException("Médico não encontrado ou não pertence a esta clínica");
        }
        if (!medico.getAtivo()) {
            throw new BusinessException("Não é possível agendar com um médico inativo");
        }

        LocalTime horaInicio = dto.horaInicio();
        LocalTime horaFim = horaInicio.plusMinutes(medico.getDuracaoConsulta());

        validarHorarioFuturo(dto.dataConsulta(), horaInicio);
        validarHorarioAtendimentoMedico(medico, dto.dataConsulta(), horaInicio, horaFim);
        validarConflitoHorario(clinicId, medico.getId(), dto.dataConsulta(), horaInicio, horaFim, null);

        PlanoSaude planoSaude = validarPlanoSaude(dto.tipoPagamento(), dto.planoSaudeId(), clinicId);

        Agendamento agendamento = new Agendamento();
        agendamento.setClinica(clinica);
        agendamento.setPaciente(paciente);
        agendamento.setMedico(medico);
        agendamento.setDataConsulta(dto.dataConsulta());
        agendamento.setHoraInicio(horaInicio);
        agendamento.setHoraFim(horaFim);
        agendamento.setStatus(StatusAgendamento.AGENDADO);
        agendamento.setTipoPagamento(dto.tipoPagamento());
        agendamento.setPlanoSaude(planoSaude);
        agendamento.setObservacoes(dto.observacoes());

        Agendamento agendamentoSalvo = agendamentoRepository.save(agendamento);
        
        if (paciente.getEmail() != null && !paciente.getEmail().trim().isEmpty()) {
        	String dataFormatada = dto.dataConsulta().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        	String assunto = "Confirmação de Agendamento - " + clinica.getNomeFantasia();
        	
        	String mensagem = String.format(
        			"Olá, %s, seu agendamento foi confirmado com sucesso!\n\n" + 
        			"Detalhes da consulta:\n" + 
        			"Médico(a): Dr(a). %s\n" + 
        			"Data: %s\n" + 
        			"Horário: %s\n" + 
        			"Clínica: %s\n\n" + 
        			"Agradecemos a preferência!",
        			paciente.getNome(),
        			medico.getNome(),
        			dataFormatada,
        			horaInicio.toString(),
        			clinica.getNomeFantasia()
				);
        	emailService.enviarEmail(paciente.getEmail(), assunto, mensagem);
        }
        return agendamentoSalvo;
    }

    @Override
    @Transactional
    public Agendamento remarcar(Long id, Long clinicId, AgendamentoRemarcarDTO dto) {
        Agendamento agendamento = findByIdAndClinicId(id, clinicId);

        if (agendamento.getStatus() == StatusAgendamento.CANCELADO_CLINICA ||
                agendamento.getStatus() == StatusAgendamento.CANCELADO_PACIENTE ||
                agendamento.getStatus() == StatusAgendamento.REALIZADO) {
            throw new BusinessException("Não é possível remarcar um agendamento " +
                    traduzirStatus(agendamento.getStatus()) + ".");
        }

        Medico medico = agendamento.getMedico();
        LocalTime novaHoraFim = dto.novaHoraInicio().plusMinutes(medico.getDuracaoConsulta());

        validarHorarioFuturo(dto.novaDataConsulta(), dto.novaHoraInicio());
        validarHorarioAtendimentoMedico(medico, dto.novaDataConsulta(), dto.novaHoraInicio(), novaHoraFim);
        validarConflitoHorario(clinicId, medico.getId(), dto.novaDataConsulta(), dto.novaHoraInicio(), novaHoraFim, id);

        agendamento.setDataConsulta(dto.novaDataConsulta());
        agendamento.setHoraInicio(dto.novaHoraInicio());
        agendamento.setHoraFim(novaHoraFim);
        agendamento.setStatus(StatusAgendamento.AGENDADO);

        return agendamentoRepository.save(agendamento);
    }

    @Override
    @Transactional
    public Agendamento cancelar(Long id, Long clinicId, boolean canceladoPelaClinica) {
        Agendamento agendamento = findByIdAndClinicId(id, clinicId);

        if (agendamento.getStatus() == StatusAgendamento.CANCELADO_CLINICA ||
                agendamento.getStatus() == StatusAgendamento.CANCELADO_PACIENTE) {
            throw new BusinessException("Este agendamento já foi cancelado.");
        }

        if (agendamento.getStatus() == StatusAgendamento.REALIZADO) {
            throw new BusinessException("Não é possível cancelar um agendamento que já foi realizado.");
        }

        agendamento.setStatus(
                canceladoPelaClinica ? StatusAgendamento.CANCELADO_CLINICA : StatusAgendamento.CANCELADO_PACIENTE);

        return agendamentoRepository.save(agendamento);
    }

    @Override
    @Transactional
    public Agendamento atualizarStatus(Long id, Long clinicId, AgendamentoStatusDTO dto) {
        Agendamento agendamento = findByIdAndClinicId(id, clinicId);

        StatusAgendamento statusAtual = agendamento.getStatus();
        StatusAgendamento novoStatus = dto.novoStatus();

        if (statusAtual == StatusAgendamento.CANCELADO_CLINICA ||
                statusAtual == StatusAgendamento.CANCELADO_PACIENTE) {
            throw new BusinessException("Não é possível alterar o status de um agendamento cancelado.");
        }

        if (statusAtual == StatusAgendamento.REALIZADO || statusAtual == StatusAgendamento.FALTOU) {
            throw new BusinessException("Não é possível alterar o status de um agendamento já finalizado.");
        }

        if (novoStatus == StatusAgendamento.CANCELADO_CLINICA || novoStatus == StatusAgendamento.CANCELADO_PACIENTE) {
            throw new BusinessException("Use o endpoint de cancelamento para cancelar agendamentos.");
        }

        agendamento.setStatus(novoStatus);
        return agendamentoRepository.save(agendamento);
    }

    private void validarHorarioFuturo(LocalDate data, LocalTime hora) {
        LocalDate hoje = LocalDate.now();
        if (data.isEqual(hoje) && hora.isBefore(LocalTime.now())) {
            throw new BusinessException("Não é possível agendar para um horário que já passou.");
        }
    }

    private void validarHorarioAtendimentoMedico(Medico medico, LocalDate data, LocalTime inicio, LocalTime fim) {
        int diaSemana = data.getDayOfWeek().getValue();
        String diaSemanaStr = DIAS_SEMANA_PT.get(data.getDayOfWeek());

        List<GradeHorario> grades = gradeHorarioRepository.findAllByMedicoIdAndDiaSemana(medico.getId(), diaSemana);

        if (grades.isEmpty()) {
            throw new BusinessException("O médico não atende neste dia da semana (" + diaSemanaStr + ").");
        }

        boolean horarioValido = grades.stream()
                .anyMatch(grade -> !inicio.isBefore(grade.getHoraInicio()) && !fim.isAfter(grade.getHoraFim()));

        if (!horarioValido) {
            StringBuilder periodos = new StringBuilder();
            for (int i = 0; i < grades.size(); i++) {
                GradeHorario g = grades.get(i);
                periodos.append(g.getHoraInicio()).append(" às ").append(g.getHoraFim());
                if (i < grades.size() - 1)
                    periodos.append(", ");
            }
            throw new BusinessException("O horário solicitado está fora do período de atendimento do médico. " +
                    "Horários disponíveis em " + diaSemanaStr + ": " + periodos + ".");
        }
    }

    private void validarConflitoHorario(Long clinicId, Long medicoId, LocalDate data,
            LocalTime inicio, LocalTime fim, Long agendamentoIdIgnorar) {
        boolean conflito = agendamentoRepository.existsConflict(clinicId, medicoId, data, inicio, fim);

        if (conflito && agendamentoIdIgnorar == null) {
            throw new ResourceConflictException("O médico já possui agendamento neste horário.");
        }

        if (conflito && agendamentoIdIgnorar != null) {
            List<Agendamento> agendamentosConflitantes = agendamentoRepository
                    .findByMedicoIdAndDataConsultaAndClinicaId(medicoId, data, clinicId)
                    .stream()
                    .filter(a -> !a.getId().equals(agendamentoIdIgnorar))
                    .filter(a -> a.getStatus() != StatusAgendamento.CANCELADO_CLINICA &&
                            a.getStatus() != StatusAgendamento.CANCELADO_PACIENTE)
                    .filter(a -> inicio.isBefore(a.getHoraFim()) && fim.isAfter(a.getHoraInicio()))
                    .toList();

            if (!agendamentosConflitantes.isEmpty()) {
                throw new ResourceConflictException("O médico já possui agendamento neste horário.");
            }
        }
    }

    private PlanoSaude validarPlanoSaude(TipoPagamento tipoPagamento, Long planoSaudeId, Long clinicId) {
        if (tipoPagamento != TipoPagamento.CONVENIO) {
            return null;
        }

        if (planoSaudeId == null) {
            throw new BusinessException("Para agendamentos via convênio, o plano de saúde é obrigatório.");
        }

        PlanoSaude planoSaude = planoSaudeRepository.findById(planoSaudeId)
                .filter(p -> p.getClinica().getId().equals(clinicId))
                .orElseThrow(() -> new ResourceNotFoundException("Plano de saúde inválido para esta clínica"));

        if (!planoSaude.getAtivo()) {
            throw new BusinessException("Este plano de saúde está inativo.");
        }

        return planoSaude;
    }

    private String traduzirStatus(StatusAgendamento status) {
        return switch (status) {
            case AGENDADO -> "agendado";
            case CONFIRMADO -> "confirmado";
            case CANCELADO_PACIENTE -> "cancelado pelo paciente";
            case CANCELADO_CLINICA -> "cancelado pela clínica";
            case REALIZADO -> "realizado";
            case FALTOU -> "marcado como falta";
        };
    }
}
