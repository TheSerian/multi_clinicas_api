package com.multiclinicas.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.multiclinicas.api.config.WebConfig;
import com.multiclinicas.api.config.tenant.TenantContext;
import com.multiclinicas.api.config.tenant.TenantInterceptor;
import com.multiclinicas.api.dtos.CreateEnderecoDTO;
import com.multiclinicas.api.dtos.EnderecoDTO;
import com.multiclinicas.api.dtos.PacienteCreateDTO;
import com.multiclinicas.api.dtos.PacienteDTO;
import com.multiclinicas.api.exceptions.ResourceNotFoundException;
import com.multiclinicas.api.mappers.PacienteMapper;
import com.multiclinicas.api.models.Clinica;
import com.multiclinicas.api.models.Paciente;
import com.multiclinicas.api.repositories.ClinicaRepository;
import com.multiclinicas.api.services.ClinicaService;
import com.multiclinicas.api.services.PacienteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PacienteController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ WebConfig.class, TenantInterceptor.class })
class PacienteControllerTest {

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.multiclinicas.api.config.JwtAuthenticationFilter jwtAuthenticationFilter;


        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private PacienteService pacienteService;

        @MockitoBean
        private PacienteMapper pacienteMapper;

        @MockitoBean
        private ClinicaService clinicaService;

        @MockitoBean
        private ClinicaRepository clinicaRepository;

        private PacienteCreateDTO pacienteCreateDTO;
        private PacienteDTO pacienteDTO;
        private Paciente paciente;

        private final Long CLINIC_ID = 10L;

        @BeforeEach
        void setup() {
                TenantContext.setClinicId(CLINIC_ID);

                com.multiclinicas.api.models.Clinica clinica = new com.multiclinicas.api.models.Clinica();
        clinica.setId(1L);
        clinica.setAtivo(true);
        when(clinicaRepository.findById(any())).thenReturn(java.util.Optional.of(clinica));

                CreateEnderecoDTO enderecoCreate = new CreateEnderecoDTO(
                                "00000-000", "rua 3", "1", "casa", "Centro", "Recife", "PE", "brasil");

                EnderecoDTO enderecoRetorno = new EnderecoDTO(
                                1L, "Rua Teste", "123", "Apto 1", "Centro", "Cidade", "SP", "00000-000", "brasil");

                pacienteCreateDTO = new PacienteCreateDTO(
                                "João Silva",
                                "joao@email.com",
                                "123.456.789-00",
                                "11999999999",
                                null,
                                "senhaForte123",
                                enderecoCreate);

                pacienteDTO = new PacienteDTO(
                                1L,
                                CLINIC_ID,
                                "João Silva",
                                "joao@email.com",
                                "123.456.789-00",
                                "11999999999",
                                null,
                                enderecoRetorno);

                paciente = new Paciente();
                paciente.setId(1L);
                paciente.setNome("João Silva");
                paciente.setEmail("joao@email.com");
        }

        @Test
        @DisplayName("Deve retornar lista de pacientes")
        void shouldReturnListOfPacientes() throws Exception {
                when(pacienteService.findAll(CLINIC_ID)).thenReturn(List.of(paciente));
                when(pacienteMapper.toDto(paciente)).thenReturn(pacienteDTO);

                mockMvc.perform(
                                get("/pacientes")
                                                .header("X-Clinic-ID", CLINIC_ID))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].id").value(1L))
                                .andExpect(jsonPath("$[0].nome").value("João Silva"));
        }

        @Test
        @DisplayName("Deve retornar paciente por ID")
        void shouldReturnPacienteById() throws Exception {
                when(pacienteService.findById(1L, CLINIC_ID)).thenReturn(paciente);
                when(pacienteMapper.toDto(paciente)).thenReturn(pacienteDTO);

                mockMvc.perform(
                                get("/pacientes/1")
                                                .header("X-Clinic-ID", CLINIC_ID))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(1L))
                                .andExpect(jsonPath("$.email").value("joao@email.com"));
        }

        @Test
        @DisplayName("Deve retornar 404 quando paciente não encontrado")
        void shouldReturn404WhenPacienteNotFound() throws Exception {
                doThrow(new ResourceNotFoundException("Paciente não encontrado nesta clínica"))
                                .when(pacienteService).findById(999L, CLINIC_ID);

                mockMvc.perform(
                                get("/pacientes/999")
                                                .header("X-Clinic-ID", CLINIC_ID))
                                .andDo(print())
                                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Deve criar paciente com sucesso")
        void shouldCreatePacienteSuccessfully() throws Exception {
                Clinica clinicaMock = new Clinica();
                clinicaMock.setId(CLINIC_ID);

                when(pacienteMapper.toEntity(any())).thenReturn(paciente);
                when(clinicaService.findById(CLINIC_ID)).thenReturn(clinicaMock);

                // CORREÇÃO: Adicionado eq(CLINIC_ID) para corresponder à assinatura do Service
                when(pacienteService.create(eq(CLINIC_ID), any(Paciente.class))).thenReturn(paciente);

                when(pacienteMapper.toDto(paciente)).thenReturn(pacienteDTO);

                mockMvc.perform(
                                post("/pacientes")
                                                .header("X-Clinic-ID", CLINIC_ID)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(pacienteCreateDTO)))
                                .andDo(print())
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(1L))
                                .andExpect(jsonPath("$.nome").value("João Silva"));
        }

        @Test
        @DisplayName("Deve retornar 400 ao criar paciente com dados inválidos")
        void shouldReturn400WhenCreatingInvalidPaciente() throws Exception {
                PacienteCreateDTO invalidDTO = new PacienteCreateDTO(
                                "", "inválido", "", "", null, null, null);

                mockMvc.perform(
                                post("/pacientes")
                                                .header("X-Clinic-ID", CLINIC_ID)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(invalidDTO)))
                                .andDo(print())
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Deve atualizar paciente com sucesso")
        void shouldUpdatePacienteSuccessfully() throws Exception {
                when(pacienteMapper.toEntity(any())).thenReturn(paciente);
                when(pacienteService.update(eq(1L), any(Paciente.class), eq(CLINIC_ID)))
                                .thenReturn(paciente);
                when(pacienteMapper.toDto(paciente)).thenReturn(pacienteDTO);

                mockMvc.perform(
                                put("/pacientes/1")
                                                .header("X-Clinic-ID", CLINIC_ID)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(pacienteCreateDTO)))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.nome").value("João Silva"));
        }

        @Test
        @DisplayName("Deve deletar paciente com sucesso")
        void shouldDeletePacienteSuccessfully() throws Exception {
                mockMvc.perform(
                                delete("/pacientes/1")
                                                .header("X-Clinic-ID", CLINIC_ID))
                                .andDo(print())
                                .andExpect(status().isNoContent());

                // Verifica se o service foi chamado com o ID correto e o ClinicID do contexto
                org.mockito.Mockito.verify(pacienteService).delete(eq(1L), eq(CLINIC_ID));
        }
}