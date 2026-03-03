package com.multiclinicas.api.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;
import java.util.Set;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.multiclinicas.api.config.WebConfig;
import com.multiclinicas.api.config.tenant.TenantInterceptor;
import com.multiclinicas.api.dtos.MedicoCreateDTO;
import com.multiclinicas.api.dtos.MedicoDTO;
import com.multiclinicas.api.exceptions.ResourceNotFoundException;
import com.multiclinicas.api.mappers.MedicoMapper;
import com.multiclinicas.api.models.Medico;
import com.multiclinicas.api.repositories.ClinicaRepository;
import com.multiclinicas.api.services.MedicoService;

@WebMvcTest(MedicoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ WebConfig.class, TenantInterceptor.class })
class MedicoControllerTest {

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.multiclinicas.api.config.JwtAuthenticationFilter jwtAuthenticationFilter;


        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private MedicoService medicoService;

        @MockitoBean
        private MedicoMapper medicoMapper;

        @MockitoBean
        private ClinicaRepository clinicaRepository;

        private final Long clinicId = 1L;
        private Medico medico;
        private MedicoDTO medicoDTO;
        private MedicoCreateDTO medicoCreateDTO;

        @BeforeEach
        void setup() {
                com.multiclinicas.api.models.Clinica clinica = new com.multiclinicas.api.models.Clinica();
        clinica.setId(clinicId);
        clinica.setAtivo(true);
        when(clinicaRepository.findById(clinicId)).thenReturn(java.util.Optional.of(clinica));

                medico = new Medico();
                medico.setId(1L);
                medico.setNome("Dr. House");
                medico.setCrm("12345/PE");

                medicoDTO = new MedicoDTO(1L, "Dr. House", "123.456.789-00", "12345/PE", clinicId, "X-Clinic-Id", "87999999999", "Rua B", 30,
                                Collections.emptySet(), true);
                medicoCreateDTO = new MedicoCreateDTO("Dr. House", "209.163.430-14", "12345/PE", "87999999999", 30,
                                Set.of(10L), null, true);
        }

        @Test
        @DisplayName("Deve retornar 400 Bad Request se header X-Clinic-ID estiver faltando")
        void shouldReturn400WhenHeaderMissing() throws Exception {
                mockMvc.perform(get("/medicos"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Deve retornar lista de médicos da clínica")
        void shouldReturnListOfMedicosForClinic() throws Exception {
                when(medicoService.findAllByClinicId(clinicId)).thenReturn(List.of(medico));
                when(medicoMapper.toDTO(medico)).thenReturn(medicoDTO);

                mockMvc.perform(get("/medicos")
                                .header("X-Clinic-ID", clinicId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].id").value(medico.getId().longValue()))
                                .andExpect(jsonPath("$[0].nome").value(medico.getNome()))
                                .andExpect(jsonPath("$[0].crm").value(medico.getCrm()));
        }

        @Test
        @DisplayName("Deve retornar médico por ID e Clínica")
        void shouldReturnMedicoById() throws Exception {
                Long medicoId = 1L;
                when(medicoService.findByIdAndClinicId(medicoId, clinicId)).thenReturn(medico);
                when(medicoMapper.toDTO(medico)).thenReturn(medicoDTO);

                mockMvc.perform(get("/medicos/{id}", medicoId)
                                .header("X-Clinic-ID", clinicId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(medico.getId().longValue()))
                                .andExpect(jsonPath("$.crm").value(medico.getCrm()));
        }

        @Test
        @DisplayName("Deve atualizar um médico com sucesso")
        void shouldUpdateMedicoSuccessfully() throws Exception {
                Long medicoId = 1L;
                Medico medicoAtualizado = new Medico();
                medicoAtualizado.setId(medicoId);
                medicoAtualizado.setNome("Doutor House");

                MedicoDTO dtoAtualizado = new MedicoDTO(medicoId, "Doutor House", null, null, null, null, null, null, null,
                                Collections.emptySet(), true);

                when(medicoMapper.toEntity(any(com.multiclinicas.api.dtos.MedicoUpdateDTO.class))).thenReturn(medico);
                when(medicoService.update(eq(medicoId), eq(clinicId), any(Medico.class), any()))
                                .thenReturn(medicoAtualizado);
                when(medicoMapper.toDTO(any(Medico.class))).thenReturn(dtoAtualizado);

                mockMvc.perform(put("/medicos/{id}", medicoId)
                                .header("X-Clinic-ID", clinicId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new com.multiclinicas.api.dtos.MedicoUpdateDTO("Doutor House", "12345-SP", 30, java.util.Collections.emptySet(), "dr.house@clinica.com", true))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(medicoId.longValue()))
                                .andExpect(jsonPath("$.nome").value("Doutor House"));
        }

        @Test
        @DisplayName("Deve retornar 404 ao tentar atualizar médico inexistente")
        void shouldReturn404WhenUpdatingNonExistentMedico() throws Exception {
                Long idNaoExistente = 99L;
                when(medicoMapper.toEntity(any(com.multiclinicas.api.dtos.MedicoUpdateDTO.class))).thenReturn(medico);
                when(medicoService.update(eq(idNaoExistente), eq(clinicId), any(Medico.class), any()))
                                .thenThrow(new ResourceNotFoundException("Médico não encontrado."));

                mockMvc.perform(put("/medicos/{id}", idNaoExistente)
                                .header("X-Clinic-ID", clinicId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new com.multiclinicas.api.dtos.MedicoUpdateDTO("Doutor House", "12345-SP", 30, java.util.Collections.emptySet(), "dr.house@clinica.com", true))))
                                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Deve deletar um médico com sucesso")
        void shouldDeleteMedicoSuccessfully() throws Exception {
                Long medicoId = 1L;
                doNothing().when(medicoService).delete(medicoId, clinicId);

                mockMvc.perform(delete("/medicos/{id}", medicoId)
                                .header("X-Clinic-ID", clinicId))
                                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Deve retornar 404 ao tentar deletar médico inexistente")
        void shouldReturn404WhenDeletingNonExistentMedico() throws Exception {
                Long idNaoExistente = 99L;
                doThrow(new ResourceNotFoundException("Médico não encontrado"))
                                .when(medicoService).delete(idNaoExistente, clinicId);

                mockMvc.perform(delete("/medicos/{id}", idNaoExistente)
                                .header("X-Clinic-ID", clinicId))
                                .andExpect(status().isNotFound());
        }

}