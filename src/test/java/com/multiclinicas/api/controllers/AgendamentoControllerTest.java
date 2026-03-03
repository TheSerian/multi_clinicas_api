package com.multiclinicas.api.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.multiclinicas.api.config.WebConfig;
import com.multiclinicas.api.config.tenant.TenantContext;
import com.multiclinicas.api.config.tenant.TenantInterceptor;
import com.multiclinicas.api.models.Clinica;
import com.multiclinicas.api.dtos.AgendamentoCreateDTO;
import com.multiclinicas.api.dtos.AgendamentoDTO;
import com.multiclinicas.api.dtos.AgendamentoRemarcarDTO;
import com.multiclinicas.api.dtos.AgendamentoStatusDTO;
import com.multiclinicas.api.exceptions.BusinessException;
import com.multiclinicas.api.exceptions.ResourceNotFoundException;
import com.multiclinicas.api.mappers.AgendamentoMapper;
import com.multiclinicas.api.models.Agendamento;
import com.multiclinicas.api.models.enums.StatusAgendamento;
import com.multiclinicas.api.models.enums.TipoPagamento;
import com.multiclinicas.api.repositories.ClinicaRepository;
import com.multiclinicas.api.services.AgendamentoService;
import com.multiclinicas.api.services.JwtService;

@WebMvcTest(AgendamentoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ WebConfig.class, TenantInterceptor.class })
class AgendamentoControllerTest {

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.multiclinicas.api.config.JwtAuthenticationFilter jwtAuthenticationFilter;


    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @MockitoBean
    private AgendamentoService agendamentoService;

    @MockitoBean
    private AgendamentoMapper agendamentoMapper;

    @MockitoBean
    private ClinicaRepository clinicaRepository;

    @MockitoBean
    private JwtService jwtService;

    private final Long CLINIC_ID = 1L;

    private AgendamentoDTO agendamentoDTO;
    private Agendamento agendamento;

    @BeforeEach
    void setUp() {
        TenantContext.setClinicId(CLINIC_ID);

        Clinica clinica = new Clinica();
        clinica.setId(CLINIC_ID);
        clinica.setAtivo(true);
        when(clinicaRepository.findById(any())).thenReturn(Optional.of(clinica));

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        agendamento = new Agendamento();
        agendamento.setId(1L);
        agendamento.setStatus(StatusAgendamento.AGENDADO);

        agendamentoDTO = new AgendamentoDTO(
                1L, 10L, "João Paciente",
                20L, "Dr. House",
                LocalDate.now().plusDays(7), LocalTime.of(9, 0), LocalTime.of(9, 30),
                StatusAgendamento.AGENDADO, TipoPagamento.PARTICULAR,
                null, "Consulta de rotina");
    }

    @Nested
    @DisplayName("GET /agendamentos")
    class ListarTests {

        @Test
        @DisplayName("Deve listar agendamentos com sucesso")
        void shouldListAgendamentos() throws Exception {
            when(agendamentoService.findAllByClinicId(CLINIC_ID))
                    .thenReturn(List.of(agendamento));
            when(agendamentoMapper.toDTO(agendamento)).thenReturn(agendamentoDTO);

            mockMvc.perform(
                    get("/agendamentos")
                            .header("X-Clinic-ID", CLINIC_ID))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1L))
                    .andExpect(jsonPath("$[0].nomePaciente").value("João Paciente"));
        }
    }

    @Nested
    @DisplayName("GET /agendamentos/{id}")
    class BuscarPorIdTests {

        @Test
        @DisplayName("Deve retornar agendamento por ID")
        void shouldReturnAgendamentoById() throws Exception {
            when(agendamentoService.findByIdAndClinicId(1L, CLINIC_ID))
                    .thenReturn(agendamento);
            when(agendamentoMapper.toDTO(agendamento)).thenReturn(agendamentoDTO);

            mockMvc.perform(
                    get("/agendamentos/1")
                            .header("X-Clinic-ID", CLINIC_ID))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.nomeMedico").value("Dr. House"));
        }

        @Test
        @DisplayName("Deve retornar 404 quando agendamento não encontrado")
        void shouldReturn404WhenNotFound() throws Exception {
            when(agendamentoService.findByIdAndClinicId(999L, CLINIC_ID))
                    .thenThrow(new ResourceNotFoundException("Agendamento não encontrado"));

            mockMvc.perform(
                    get("/agendamentos/999")
                            .header("X-Clinic-ID", CLINIC_ID))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /agendamentos")
    class CriarTests {

        @Test
        @DisplayName("Deve criar agendamento com sucesso")
        void shouldCreateAgendamento() throws Exception {
            AgendamentoCreateDTO createDTO = new AgendamentoCreateDTO(
                    10L, 20L,
                    LocalDate.now().plusDays(7), LocalTime.of(9, 0),
                    TipoPagamento.PARTICULAR, null, "Consulta de rotina");

            when(agendamentoService.create(eq(CLINIC_ID), any(AgendamentoCreateDTO.class)))
                    .thenReturn(agendamento);
            when(agendamentoMapper.toDTO(agendamento)).thenReturn(agendamentoDTO);

            mockMvc.perform(
                    post("/agendamentos")
                            .header("X-Clinic-ID", CLINIC_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDTO)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.status").value("AGENDADO"));
        }

        @Test
        @DisplayName("Deve retornar 400 quando dados inválidos")
        void shouldReturn400WhenInvalidData() throws Exception {
            AgendamentoCreateDTO invalidDTO = new AgendamentoCreateDTO(
                    null, null, null, null, null, null, null);

            mockMvc.perform(
                    post("/agendamentos")
                            .header("X-Clinic-ID", CLINIC_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDTO)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /agendamentos/{id}/remarcar")
    class RemarcarTests {

        @Test
        @DisplayName("Deve remarcar agendamento com sucesso")
        void shouldRemarcarAgendamento() throws Exception {
            AgendamentoRemarcarDTO remarcarDTO = new AgendamentoRemarcarDTO(
                    LocalDate.now().plusDays(14), LocalTime.of(10, 0));

            when(agendamentoService.remarcar(eq(1L), eq(CLINIC_ID), any(AgendamentoRemarcarDTO.class)))
                    .thenReturn(agendamento);
            when(agendamentoMapper.toDTO(agendamento)).thenReturn(agendamentoDTO);

            mockMvc.perform(
                    put("/agendamentos/1/remarcar")
                            .header("X-Clinic-ID", CLINIC_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(remarcarDTO)))
                    .andDo(print())
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PATCH /agendamentos/{id}/cancelar")
    class CancelarTests {

        @Test
        @DisplayName("Deve cancelar agendamento pela clínica")
        void shouldCancelarByClinica() throws Exception {
            agendamento.setStatus(StatusAgendamento.CANCELADO_CLINICA);
            AgendamentoDTO canceledDTO = new AgendamentoDTO(
                    1L, 10L, "João Paciente",
                    20L, "Dr. House",
                    LocalDate.now().plusDays(7), LocalTime.of(9, 0), LocalTime.of(9, 30),
                    StatusAgendamento.CANCELADO_CLINICA, TipoPagamento.PARTICULAR,
                    null, null);

            when(agendamentoService.cancelar(1L, CLINIC_ID, true)).thenReturn(agendamento);
            when(agendamentoMapper.toDTO(agendamento)).thenReturn(canceledDTO);

            mockMvc.perform(
                    patch("/agendamentos/1/cancelar")
                            .header("X-Clinic-ID", CLINIC_ID)
                            .param("canceladoPelaClinica", "true"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELADO_CLINICA"));
        }

        @Test
        @DisplayName("Deve retornar erro ao cancelar agendamento já realizado")
        void shouldReturn400WhenAlreadyRealized() throws Exception {
            when(agendamentoService.cancelar(1L, CLINIC_ID, true))
                    .thenThrow(new BusinessException("Não é possível cancelar um agendamento que já foi realizado."));

            mockMvc.perform(
                    patch("/agendamentos/1/cancelar")
                            .header("X-Clinic-ID", CLINIC_ID)
                            .param("canceladoPelaClinica", "true"))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PATCH /agendamentos/{id}/status")
    class AtualizarStatusTests {

        @Test
        @DisplayName("Deve atualizar status para CONFIRMADO")
        void shouldUpdateStatusToConfirmado() throws Exception {
            AgendamentoStatusDTO statusDTO = new AgendamentoStatusDTO(StatusAgendamento.CONFIRMADO);

            agendamento.setStatus(StatusAgendamento.CONFIRMADO);
            AgendamentoDTO confirmedDTO = new AgendamentoDTO(
                    1L, 10L, "João Paciente",
                    20L, "Dr. House",
                    LocalDate.now().plusDays(7), LocalTime.of(9, 0), LocalTime.of(9, 30),
                    StatusAgendamento.CONFIRMADO, TipoPagamento.PARTICULAR,
                    null, null);

            when(agendamentoService.atualizarStatus(eq(1L), eq(CLINIC_ID), any(AgendamentoStatusDTO.class)))
                    .thenReturn(agendamento);
            when(agendamentoMapper.toDTO(agendamento)).thenReturn(confirmedDTO);

            mockMvc.perform(
                    patch("/agendamentos/1/status")
                            .header("X-Clinic-ID", CLINIC_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(statusDTO)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONFIRMADO"));
        }
    }
}
