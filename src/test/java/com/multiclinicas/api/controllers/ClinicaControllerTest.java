package com.multiclinicas.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.multiclinicas.api.config.WebConfig;
import com.multiclinicas.api.config.tenant.TenantInterceptor;
import com.multiclinicas.api.dtos.ClinicaCreateDTO;
import com.multiclinicas.api.dtos.ClinicaDTO;
import com.multiclinicas.api.exceptions.ResourceNotFoundException;
import com.multiclinicas.api.mappers.ClinicaMapper;
import com.multiclinicas.api.models.Clinica;
import com.multiclinicas.api.repositories.ClinicaRepository;
import com.multiclinicas.api.services.ClinicaService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClinicaController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ WebConfig.class, TenantInterceptor.class })
class ClinicaControllerTest {

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.multiclinicas.api.config.JwtAuthenticationFilter jwtAuthenticationFilter;


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClinicaService clinicaService;

    @MockitoBean
    private ClinicaMapper clinicaMapper;

    @MockitoBean
    private ClinicaRepository clinicaRepository;

    @BeforeEach
    void setup() {
        // Necessário para o TenantInterceptor, mesmo que excluído, para garantir que o
        // contexto suba
        com.multiclinicas.api.models.Clinica clinica = new com.multiclinicas.api.models.Clinica();
        clinica.setId(1L);
        clinica.setAtivo(true);
        when(clinicaRepository.findById(any())).thenReturn(java.util.Optional.of(clinica));
    }

    @Test
    @DisplayName("Deve retornar lista de clínicas")
    void shouldReturnListOfClinicas() throws Exception {
        // Given
        Clinica clinica = new Clinica();
        clinica.setId(1L);
        ClinicaDTO clinicaDTO = new ClinicaDTO(1L, "Clinica A", "clinica-a", true, null);

        when(clinicaService.findAll()).thenReturn(List.of(clinica));
        when(clinicaMapper.toDTO(clinica)).thenReturn(clinicaDTO);

        // When & Then
        mockMvc.perform(get("/clinicas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].nomeFantasia").value("Clinica A"));
    }

    @Test
    @DisplayName("Deve retornar clínica por ID")
    void shouldReturnClinicaById() throws Exception {
        // Given
        Long id = 1L;
        Clinica clinica = new Clinica();
        clinica.setId(id);
        ClinicaDTO clinicaDTO = new ClinicaDTO(id, "Clinica A", "clinica-a", true, null);

        when(clinicaService.findById(id)).thenReturn(clinica);
        when(clinicaMapper.toDTO(clinica)).thenReturn(clinicaDTO);

        // When & Then
        mockMvc.perform(get("/clinicas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.nomeFantasia").value("Clinica A"));
    }

    @Test
    @DisplayName("Deve retornar 404 quando clínica não encontrada")
    void shouldReturn404WhenClinicaNotFound() throws Exception {
        // Given
        Long id = 1L;
        when(clinicaService.findById(id)).thenThrow(new ResourceNotFoundException("Clínica não encontrada"));

        // When & Then
        mockMvc.perform(get("/clinicas/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve criar clínica com sucesso")
    void shouldCreateClinicaSuccessfully() throws Exception {
        // Given
        ClinicaCreateDTO createDTO = new ClinicaCreateDTO("Clinica Nova", "clinica-nova", true, "Admin", "admin@teste.com", "senha123");
        Clinica clinica = new Clinica();
        clinica.setId(1L);
        ClinicaDTO responseDTO = new ClinicaDTO(1L, "Clinica Nova", "clinica-nova", true, null);

        when(clinicaMapper.toEntity(any(ClinicaCreateDTO.class))).thenReturn(clinica);
        when(clinicaService.create(any(Clinica.class), any(), any(), any())).thenReturn(clinica);
        when(clinicaMapper.toDTO(clinica)).thenReturn(responseDTO);

        // When & Then
        mockMvc.perform(post("/clinicas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nomeFantasia").value("Clinica Nova"));
    }

    @Test
    @DisplayName("Deve retornar 400 ao criar clínica com dados inválidos")
    void shouldReturn400WhenCreatingInvalidClinica() throws Exception {
        // Given
        ClinicaCreateDTO invalidDTO = new ClinicaCreateDTO("", "INVALID SUBDOMAIN", true, "123", "Rua A", "Cidade A");

        // When & Then
        mockMvc.perform(post("/clinicas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve atualizar clínica com sucesso")
    void shouldUpdateClinicaSuccessfully() throws Exception {
        // Given
        Long id = 1L;
        com.multiclinicas.api.dtos.ClinicaUpdateDTO updateDTO = new com.multiclinicas.api.dtos.ClinicaUpdateDTO(
                "Clinica Atualizada", "clinica-atualizada", true);
        Clinica clinica = new Clinica();
        clinica.setId(id);
        ClinicaDTO responseDTO = new ClinicaDTO(id, "Clinica Atualizada", "clinica-atualizada", true, null);

        when(clinicaMapper.toEntity(any(com.multiclinicas.api.dtos.ClinicaUpdateDTO.class))).thenReturn(clinica);
        when(clinicaService.update(eq(id), any(Clinica.class))).thenReturn(clinica);
        when(clinicaMapper.toDTO(clinica)).thenReturn(responseDTO);

        // When & Then
        mockMvc.perform(put("/clinicas/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeFantasia").value("Clinica Atualizada"));
    }

    @Test
    @DisplayName("Deve deletar clínica com sucesso")
    void shouldDeleteClinicaSuccessfully() throws Exception {
        // Given
        Long id = 1L;

        // When & Then
        mockMvc.perform(delete("/clinicas/{id}", id))
                .andExpect(status().isNoContent());
    }
}
