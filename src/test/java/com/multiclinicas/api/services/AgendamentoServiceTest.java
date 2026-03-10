package com.multiclinicas.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.multiclinicas.api.config.tenant.TenantContext;
import com.multiclinicas.api.dtos.AgendamentoDTO;
import com.multiclinicas.api.mappers.AgendamentoMapper;
import com.multiclinicas.api.dtos.AgendamentoCreateDTO;
import com.multiclinicas.api.dtos.AgendamentoRemarcarDTO;
import com.multiclinicas.api.dtos.AgendamentoStatusDTO;
import com.multiclinicas.api.exceptions.BusinessException;
import com.multiclinicas.api.exceptions.ResourceConflictException;
import com.multiclinicas.api.exceptions.ResourceNotFoundException;
import com.multiclinicas.api.models.Agendamento;
import com.multiclinicas.api.models.Clinica;
import com.multiclinicas.api.models.GradeHorario;
import com.multiclinicas.api.models.Medico;
import com.multiclinicas.api.models.Paciente;
import com.multiclinicas.api.models.enums.StatusAgendamento;
import com.multiclinicas.api.models.enums.TipoPagamento;
import com.multiclinicas.api.repositories.AgendamentoRepository;
import com.multiclinicas.api.repositories.ClinicaRepository;
import com.multiclinicas.api.repositories.GradeHorarioRepository;
import com.multiclinicas.api.repositories.MedicoRepository;
import com.multiclinicas.api.repositories.PacienteRepository;
import com.multiclinicas.api.repositories.PlanoSaudeRepository;

@ExtendWith(MockitoExtension.class)
class AgendamentoServiceTest {

    private static final Long CLINIC_ID = 1L;
    private static final Long PACIENTE_ID = 10L;
    private static final Long MEDICO_ID = 20L;

    @Mock
    private AgendamentoRepository agendamentoRepository;
    @Mock
    private ClinicaRepository clinicaRepository;
    @Mock
    private MedicoRepository medicoRepository;
    @Mock
    private PacienteRepository pacienteRepository;
    @Mock
    private PlanoSaudeRepository planoSaudeRepository;
    @Mock
    private GradeHorarioRepository gradeHorarioRepository;
        @Mock
        private EmailService emailService;
        @Mock
        private AgendamentoMapper agendamentoMapper;

    @InjectMocks
    private AgendamentoServiceImpl agendamentoService;

    private Clinica clinica;
    private Paciente paciente;
    private Medico medico;
    private GradeHorario gradeHorario;

    @BeforeEach
    void setUp() {
        TenantContext.setClinicId(CLINIC_ID);
        SecurityContextHolder.clearContext();

        clinica = new Clinica();
        clinica.setId(CLINIC_ID);

        paciente = new Paciente();
        paciente.setId(PACIENTE_ID);
        paciente.setClinica(clinica);

        medico = new Medico();
        medico.setId(MEDICO_ID);
        medico.setClinica(clinica);
        medico.setAtivo(true);
        medico.setDuracaoConsulta(30);

        gradeHorario = new GradeHorario();
        gradeHorario.setMedico(medico);
        gradeHorario.setDiaSemana(1); // Segunda
        gradeHorario.setHoraInicio(LocalTime.of(8, 0));
        gradeHorario.setHoraFim(LocalTime.of(18, 0));
    }

    @Nested
    @DisplayName("Testes de Listagem")
    class ListagemTests {

        @Test
        @DisplayName("Deve listar os agendamentos do paciente autenticado")
        void shouldListMyAppointments() {
            Agendamento agendamento = new Agendamento();
            agendamento.setId(1L);
            agendamento.setClinica(clinica);
            agendamento.setPaciente(paciente);
            agendamento.setMedico(medico);
            agendamento.setDataConsulta(LocalDate.now().plusDays(3));
            agendamento.setHoraInicio(LocalTime.of(9, 0));
            agendamento.setHoraFim(LocalTime.of(9, 30));
            agendamento.setStatus(StatusAgendamento.AGENDADO);

            AgendamentoDTO dto = new AgendamentoDTO(
                    1L,
                    PACIENTE_ID,
                    "Paciente Teste",
                    MEDICO_ID,
                    "Médico Teste",
                    agendamento.getDataConsulta(),
                    agendamento.getHoraInicio(),
                    agendamento.getHoraFim(),
                    agendamento.getStatus(),
                    TipoPagamento.PARTICULAR,
                    null,
                    null);

            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            String.valueOf(PACIENTE_ID),
                            null,
                            List.of(() -> "ROLE_PACIENTE")));

            when(pacienteRepository.findByIdAndClinicaId(PACIENTE_ID, CLINIC_ID)).thenReturn(Optional.of(paciente));
            when(agendamentoRepository.findByPacienteIdAndClinicaIdOrderByDataConsultaDescHoraInicioDesc(PACIENTE_ID, CLINIC_ID))
                    .thenReturn(List.of(agendamento));
            when(agendamentoMapper.toDTO(agendamento)).thenReturn(dto);

            List<AgendamentoDTO> result = agendamentoService.buscarMeusAgendamentos();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).nomeMedico()).isEqualTo("Médico Teste");
            verify(agendamentoRepository).findByPacienteIdAndClinicaIdOrderByDataConsultaDescHoraInicioDesc(PACIENTE_ID,
                    CLINIC_ID);
        }

        @Test
        @DisplayName("Deve negar acesso quando o usuário autenticado não for paciente")
        void shouldDenyAccessWhenUserIsNotPaciente() {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            "999",
                            null,
                            List.of(() -> "ROLE_ADMIN")));

            assertThatThrownBy(() -> agendamentoService.buscarMeusAgendamentos())
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("apenas para pacientes");
        }

        @Test
        @DisplayName("Deve listar todos os agendamentos da clínica")
        void shouldListAllAgendamentos() {
            Agendamento a1 = new Agendamento();
            a1.setId(1L);
            Agendamento a2 = new Agendamento();
            a2.setId(2L);

            when(agendamentoRepository.findAllByClinicaId(CLINIC_ID))
                    .thenReturn(List.of(a1, a2));

            List<Agendamento> result = agendamentoService.findAllByClinicId(CLINIC_ID);

            assertThat(result).hasSize(2);
            verify(agendamentoRepository).findAllByClinicaId(CLINIC_ID);
        }

        @Test
        @DisplayName("Deve retornar agendamento por ID")
        void shouldFindById() {
            Long id = 1L;
            Agendamento agendamento = new Agendamento();
            agendamento.setId(id);
            agendamento.setClinica(clinica);

            when(agendamentoRepository.findById(id)).thenReturn(Optional.of(agendamento));

            Agendamento result = agendamentoService.findByIdAndClinicId(id, CLINIC_ID);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(id);
        }

        @Test
        @DisplayName("Deve lançar exceção quando agendamento não encontrado")
        void shouldThrowWhenNotFound() {
            when(agendamentoRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> agendamentoService.findByIdAndClinicId(999L, CLINIC_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Testes de Criação")
    class CriacaoTests {

        @Test
        @DisplayName("Deve criar agendamento com sucesso")
        void shouldCreateAgendamento() {
            LocalDate dataFutura = LocalDate.now().plusDays(7);
            // Ajusta para segunda-feira se não for
            while (dataFutura.getDayOfWeek().getValue() != 1) {
                dataFutura = dataFutura.plusDays(1);
            }

            AgendamentoCreateDTO dto = new AgendamentoCreateDTO(
                    PACIENTE_ID,
                    MEDICO_ID,
                    dataFutura,
                    LocalTime.of(9, 0),
                    TipoPagamento.PARTICULAR,
                    null,
                    "Primeira consulta");

            when(clinicaRepository.findById(CLINIC_ID)).thenReturn(Optional.of(clinica));
            when(pacienteRepository.findByIdAndClinicaId(PACIENTE_ID, CLINIC_ID))
                    .thenReturn(Optional.of(paciente));
            when(medicoRepository.findByIdAndClinicaId(MEDICO_ID, CLINIC_ID)).thenReturn(medico);
            when(gradeHorarioRepository.findAllByMedicoIdAndDiaSemana(MEDICO_ID, 1))
                    .thenReturn(List.of(gradeHorario));
            when(agendamentoRepository.existsConflict(any(), any(), any(), any(), any()))
                    .thenReturn(false);
            when(agendamentoRepository.save(any(Agendamento.class)))
                    .thenAnswer(inv -> {
                        Agendamento a = inv.getArgument(0);
                        a.setId(1L);
                        return a;
                    });

            Agendamento result = agendamentoService.create(CLINIC_ID, dto);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(StatusAgendamento.AGENDADO);
            assertThat(result.getHoraFim()).isEqualTo(LocalTime.of(9, 30));
            verify(agendamentoRepository).save(any(Agendamento.class));
        }

        @Test
        @DisplayName("Deve lançar exceção quando médico inativo")
        void shouldThrowWhenMedicoInativo() {
            medico.setAtivo(false);

            AgendamentoCreateDTO dto = new AgendamentoCreateDTO(
                    PACIENTE_ID, MEDICO_ID,
                    LocalDate.now().plusDays(1), LocalTime.of(9, 0),
                    TipoPagamento.PARTICULAR, null, null);

            when(clinicaRepository.findById(CLINIC_ID)).thenReturn(Optional.of(clinica));
            when(pacienteRepository.findByIdAndClinicaId(PACIENTE_ID, CLINIC_ID))
                    .thenReturn(Optional.of(paciente));
            when(medicoRepository.findByIdAndClinicaId(MEDICO_ID, CLINIC_ID)).thenReturn(medico);

            assertThatThrownBy(() -> agendamentoService.create(CLINIC_ID, dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("médico inativo");
        }

        @Test
        @DisplayName("Deve lançar exceção quando há conflito de horário")
        void shouldThrowWhenConflitoHorario() {
            LocalDate dataFutura = LocalDate.now().plusDays(7);
            while (dataFutura.getDayOfWeek().getValue() != 1) {
                dataFutura = dataFutura.plusDays(1);
            }

            AgendamentoCreateDTO dto = new AgendamentoCreateDTO(
                    PACIENTE_ID, MEDICO_ID,
                    dataFutura, LocalTime.of(9, 0),
                    TipoPagamento.PARTICULAR, null, null);

            when(clinicaRepository.findById(CLINIC_ID)).thenReturn(Optional.of(clinica));
            when(pacienteRepository.findByIdAndClinicaId(PACIENTE_ID, CLINIC_ID))
                    .thenReturn(Optional.of(paciente));
            when(medicoRepository.findByIdAndClinicaId(MEDICO_ID, CLINIC_ID)).thenReturn(medico);
            when(gradeHorarioRepository.findAllByMedicoIdAndDiaSemana(MEDICO_ID, 1))
                    .thenReturn(List.of(gradeHorario));
            when(agendamentoRepository.existsConflict(any(), any(), any(), any(), any()))
                    .thenReturn(true);

            assertThatThrownBy(() -> agendamentoService.create(CLINIC_ID, dto))
                    .isInstanceOf(ResourceConflictException.class)
                    .hasMessageContaining("já possui agendamento");
        }

        @Test
        @DisplayName("Deve lançar exceção quando médico não atende no dia")
        void shouldThrowWhenMedicoNaoAtendeNoDia() {
            LocalDate dataFutura = LocalDate.now().plusDays(7);
            while (dataFutura.getDayOfWeek().getValue() != 2) { // Terça
                dataFutura = dataFutura.plusDays(1);
            }

            AgendamentoCreateDTO dto = new AgendamentoCreateDTO(
                    PACIENTE_ID, MEDICO_ID,
                    dataFutura, LocalTime.of(9, 0),
                    TipoPagamento.PARTICULAR, null, null);

            when(clinicaRepository.findById(CLINIC_ID)).thenReturn(Optional.of(clinica));
            when(pacienteRepository.findByIdAndClinicaId(PACIENTE_ID, CLINIC_ID))
                    .thenReturn(Optional.of(paciente));
            when(medicoRepository.findByIdAndClinicaId(MEDICO_ID, CLINIC_ID)).thenReturn(medico);
            when(gradeHorarioRepository.findAllByMedicoIdAndDiaSemana(MEDICO_ID, 2))
                    .thenReturn(List.of()); // Não atende terça

            assertThatThrownBy(() -> agendamentoService.create(CLINIC_ID, dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("não atende neste dia");
        }

        @Test
        @DisplayName("Deve lançar exceção para convênio sem plano de saúde")
        void shouldThrowWhenConvenioSemPlano() {
            LocalDate dataFutura = LocalDate.now().plusDays(7);
            while (dataFutura.getDayOfWeek().getValue() != 1) {
                dataFutura = dataFutura.plusDays(1);
            }

            AgendamentoCreateDTO dto = new AgendamentoCreateDTO(
                    PACIENTE_ID, MEDICO_ID,
                    dataFutura, LocalTime.of(9, 0),
                    TipoPagamento.CONVENIO, null, null);

            when(clinicaRepository.findById(CLINIC_ID)).thenReturn(Optional.of(clinica));
            when(pacienteRepository.findByIdAndClinicaId(PACIENTE_ID, CLINIC_ID))
                    .thenReturn(Optional.of(paciente));
            when(medicoRepository.findByIdAndClinicaId(MEDICO_ID, CLINIC_ID)).thenReturn(medico);
            when(gradeHorarioRepository.findAllByMedicoIdAndDiaSemana(MEDICO_ID, 1))
                    .thenReturn(List.of(gradeHorario));
            when(agendamentoRepository.existsConflict(any(), any(), any(), any(), any()))
                    .thenReturn(false);

            assertThatThrownBy(() -> agendamentoService.create(CLINIC_ID, dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("plano de saúde é obrigatório");
        }
    }

    @Nested
    @DisplayName("Testes de Cancelamento")
    class CancelamentoTests {

        @Test
        @DisplayName("Deve cancelar agendamento pela clínica")
        void shouldCancelarPelaClinica() {
            Long id = 1L;
            Agendamento agendamento = new Agendamento();
            agendamento.setId(id);
            agendamento.setClinica(clinica);
            agendamento.setStatus(StatusAgendamento.AGENDADO);

            when(agendamentoRepository.findById(id)).thenReturn(Optional.of(agendamento));
            when(agendamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Agendamento result = agendamentoService.cancelar(id, CLINIC_ID, true);

            assertThat(result.getStatus()).isEqualTo(StatusAgendamento.CANCELADO_CLINICA);
        }

        @Test
        @DisplayName("Deve cancelar agendamento pelo paciente")
        void shouldCancelarPeloPaciente() {
            Long id = 1L;
            Agendamento agendamento = new Agendamento();
            agendamento.setId(id);
            agendamento.setClinica(clinica);
            agendamento.setStatus(StatusAgendamento.CONFIRMADO);

            when(agendamentoRepository.findById(id)).thenReturn(Optional.of(agendamento));
            when(agendamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Agendamento result = agendamentoService.cancelar(id, CLINIC_ID, false);

            assertThat(result.getStatus()).isEqualTo(StatusAgendamento.CANCELADO_PACIENTE);
        }

        @Test
        @DisplayName("Deve lançar exceção ao cancelar agendamento já cancelado")
        void shouldThrowWhenAlreadyCanceled() {
            Long id = 1L;
            Agendamento agendamento = new Agendamento();
            agendamento.setId(id);
            agendamento.setClinica(clinica);
            agendamento.setStatus(StatusAgendamento.CANCELADO_CLINICA);

            when(agendamentoRepository.findById(id)).thenReturn(Optional.of(agendamento));

            assertThatThrownBy(() -> agendamentoService.cancelar(id, CLINIC_ID, true))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("já foi cancelado");
        }

        @Test
        @DisplayName("Deve lançar exceção ao cancelar agendamento realizado")
        void shouldThrowWhenCancelingRealizado() {
            Long id = 1L;
            Agendamento agendamento = new Agendamento();
            agendamento.setId(id);
            agendamento.setClinica(clinica);
            agendamento.setStatus(StatusAgendamento.REALIZADO);

            when(agendamentoRepository.findById(id)).thenReturn(Optional.of(agendamento));

            assertThatThrownBy(() -> agendamentoService.cancelar(id, CLINIC_ID, true))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("já foi realizado");
        }
    }

    @Nested
    @DisplayName("Testes de Remarcação")
    class RemarcacaoTests {

        @Test
        @DisplayName("Deve remarcar agendamento com sucesso")
        void shouldRemarcarAgendamento() {
            Long id = 1L;
            LocalDate novaData = LocalDate.now().plusDays(14);
            while (novaData.getDayOfWeek().getValue() != 1) {
                novaData = novaData.plusDays(1);
            }

            Agendamento agendamento = new Agendamento();
            agendamento.setId(id);
            agendamento.setClinica(clinica);
            agendamento.setMedico(medico);
            agendamento.setStatus(StatusAgendamento.AGENDADO);

            AgendamentoRemarcarDTO dto = new AgendamentoRemarcarDTO(novaData, LocalTime.of(10, 0));

            when(agendamentoRepository.findById(id)).thenReturn(Optional.of(agendamento));
            when(gradeHorarioRepository.findAllByMedicoIdAndDiaSemana(MEDICO_ID, 1))
                    .thenReturn(List.of(gradeHorario));
            when(agendamentoRepository.existsConflict(any(), any(), any(), any(), any()))
                    .thenReturn(false);
            when(agendamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Agendamento result = agendamentoService.remarcar(id, CLINIC_ID, dto);

            assertThat(result.getDataConsulta()).isEqualTo(novaData);
            assertThat(result.getHoraInicio()).isEqualTo(LocalTime.of(10, 0));
            assertThat(result.getStatus()).isEqualTo(StatusAgendamento.AGENDADO);
        }

        @Test
        @DisplayName("Deve lançar exceção ao remarcar agendamento cancelado")
        void shouldThrowWhenRemarcandoCancelado() {
            Long id = 1L;
            Agendamento agendamento = new Agendamento();
            agendamento.setId(id);
            agendamento.setClinica(clinica);
            agendamento.setStatus(StatusAgendamento.CANCELADO_PACIENTE);

            when(agendamentoRepository.findById(id)).thenReturn(Optional.of(agendamento));

            AgendamentoRemarcarDTO dto = new AgendamentoRemarcarDTO(
                    LocalDate.now().plusDays(7), LocalTime.of(10, 0));

            assertThatThrownBy(() -> agendamentoService.remarcar(id, CLINIC_ID, dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Não é possível remarcar");
        }
    }

    @Nested
    @DisplayName("Testes de Atualização de Status")
    class StatusTests {

        @Test
        @DisplayName("Deve atualizar status para CONFIRMADO")
        void shouldUpdateStatusToConfirmado() {
            Long id = 1L;
            Agendamento agendamento = new Agendamento();
            agendamento.setId(id);
            agendamento.setClinica(clinica);
            agendamento.setStatus(StatusAgendamento.AGENDADO);

            AgendamentoStatusDTO dto = new AgendamentoStatusDTO(StatusAgendamento.CONFIRMADO);

            when(agendamentoRepository.findById(id)).thenReturn(Optional.of(agendamento));
            when(agendamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Agendamento result = agendamentoService.atualizarStatus(id, CLINIC_ID, dto);

            assertThat(result.getStatus()).isEqualTo(StatusAgendamento.CONFIRMADO);
        }

        @Test
        @DisplayName("Deve lançar exceção ao atualizar status de agendamento cancelado")
        void shouldThrowWhenUpdatingCanceledStatus() {
            Long id = 1L;
            Agendamento agendamento = new Agendamento();
            agendamento.setId(id);
            agendamento.setClinica(clinica);
            agendamento.setStatus(StatusAgendamento.CANCELADO_CLINICA);

            AgendamentoStatusDTO dto = new AgendamentoStatusDTO(StatusAgendamento.CONFIRMADO);

            when(agendamentoRepository.findById(id)).thenReturn(Optional.of(agendamento));

            assertThatThrownBy(() -> agendamentoService.atualizarStatus(id, CLINIC_ID, dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("cancelado");
        }

        @Test
        @DisplayName("Deve lançar exceção ao tentar cancelar via atualização de status")
        void shouldThrowWhenTryingToCancelViaStatus() {
            Long id = 1L;
            Agendamento agendamento = new Agendamento();
            agendamento.setId(id);
            agendamento.setClinica(clinica);
            agendamento.setStatus(StatusAgendamento.AGENDADO);

            AgendamentoStatusDTO dto = new AgendamentoStatusDTO(StatusAgendamento.CANCELADO_CLINICA);

            when(agendamentoRepository.findById(id)).thenReturn(Optional.of(agendamento));

            assertThatThrownBy(() -> agendamentoService.atualizarStatus(id, CLINIC_ID, dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("endpoint de cancelamento");
        }
    }
}
