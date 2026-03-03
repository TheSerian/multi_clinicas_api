package com.multiclinicas.api.controllers;

import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.multiclinicas.api.config.WebConfig;
import com.multiclinicas.api.config.tenant.TenantInterceptor;
import com.multiclinicas.api.dtos.GradeHorarioCreateDTO;
import com.multiclinicas.api.dtos.GradeHorarioDTO;
import com.multiclinicas.api.exceptions.ResourceNotFoundException;
import com.multiclinicas.api.mappers.GradeHorarioMapper;
import com.multiclinicas.api.models.GradeHorario;
import com.multiclinicas.api.repositories.ClinicaRepository;
import com.multiclinicas.api.services.GradeHorarioService;

@WebMvcTest(GradeHorarioController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ WebConfig.class, TenantInterceptor.class })
class GradeHorarioControllerTest {

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.multiclinicas.api.config.JwtAuthenticationFilter jwtAuthenticationFilter;


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GradeHorarioService gradeHorarioService;

    @MockitoBean
    private GradeHorarioMapper gradeHorarioMapper;

    @MockitoBean
    private ClinicaRepository clinicaRepository;

    private final Long clinicId = 1L;
    private final Long gradeId = 100L;
    private GradeHorarioDTO gradeDTO;
    private GradeHorarioCreateDTO createDTO;
    private GradeHorario gradeEntity;

    @BeforeEach
    void setup() {
        com.multiclinicas.api.models.Clinica clinica = new com.multiclinicas.api.models.Clinica();
        clinica.setId(clinicId);
        clinica.setAtivo(true);
        when(clinicaRepository.findById(clinicId)).thenReturn(java.util.Optional.of(clinica));

        LocalTime inicio = LocalTime.of(8, 0);
        LocalTime fim = LocalTime.of(12, 0);

        gradeEntity = new GradeHorario();
        gradeEntity.setId(gradeId);
        gradeEntity.setDiaSemana(1);
        gradeEntity.setHoraInicio(inicio);
        gradeEntity.setHoraFim(fim);

        gradeDTO = new GradeHorarioDTO(gradeId, 10L, 1, inicio, fim);
        createDTO = new GradeHorarioCreateDTO(10L, 1, inicio, fim);
    }

    @Test
    @DisplayName("Deve retornar lista de grades")
    void shouldReturnListOfGrades() throws Exception {
        when(gradeHorarioService.findAllByClinicId(clinicId)).thenReturn(List.of(gradeEntity));
        when(gradeHorarioMapper.toDTO(any(GradeHorario.class))).thenReturn(gradeDTO);

        mockMvc.perform(get("/grade-horario")
                .header("X-Clinic-ID", clinicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(gradeId))
                .andExpect(jsonPath("$[0].diaSemana").value(1));
    }

    @Test
    @DisplayName("Deve buscar grade por ID")
    void shouldFindById() throws Exception {
        when(gradeHorarioService.findByIdAndClinicId(gradeId, clinicId)).thenReturn(gradeEntity);
        when(gradeHorarioMapper.toDTO(any(GradeHorario.class))).thenReturn(gradeDTO);

        mockMvc.perform(get("/grade-horario/{id}", gradeId)
                .header("X-Clinic-ID", clinicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(gradeId));
    }

    @Test
    @DisplayName("Deve criar grade com sucesso")
    void shouldCreateGrade() throws Exception {
        when(gradeHorarioMapper.toEntity(any(GradeHorarioCreateDTO.class))).thenReturn(gradeEntity);
        when(gradeHorarioService.create(eq(clinicId), eq(createDTO.medicoId()), any(GradeHorario.class))).thenReturn(gradeEntity);
        when(gradeHorarioMapper.toDTO(any(GradeHorario.class))).thenReturn(gradeDTO);

        mockMvc.perform(post("/grade-horario")
                .header("X-Clinic-ID", clinicId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(gradeId));
    }

    @Test
    @DisplayName("Deve retornar 404 ao buscar grade inexistente")
    void shouldReturn404WhenNotFound() throws Exception {
        when(gradeHorarioService.findByIdAndClinicId(999L, clinicId))
                .thenThrow(new ResourceNotFoundException("Grade não encontrada"));

        mockMvc.perform(get("/grade-horario/{id}", 999L)
                .header("X-Clinic-ID", clinicId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve deletar grade com sucesso")
    void shouldDeleteGrade() throws Exception {
        doNothing().when(gradeHorarioService).delete(gradeId, clinicId);

        mockMvc.perform(delete("/grade-horario/{id}", gradeId)
                .header("X-Clinic-ID", clinicId))
                .andExpect(status().isNoContent());
    }
}