package com.multiclinicas.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.multiclinicas.api.dtos.PlanoSaudeCreateDTO;
import com.multiclinicas.api.dtos.PlanoSaudeDTO;
import com.multiclinicas.api.mappers.PlanoSaudeMapper;
import com.multiclinicas.api.models.PlanoSaude;
import com.multiclinicas.api.config.WebConfig;
import com.multiclinicas.api.config.tenant.TenantInterceptor;
import com.multiclinicas.api.repositories.ClinicaRepository;
import com.multiclinicas.api.services.PlanoSaudeService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlanoSaudeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ WebConfig.class, TenantInterceptor.class })
class PlanoSaudeControllerTest {

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.multiclinicas.api.config.JwtAuthenticationFilter jwtAuthenticationFilter;


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PlanoSaudeService planoSaudeService;

    @MockitoBean
    private PlanoSaudeMapper planoSaudeMapper;

    @MockitoBean
    private ClinicaRepository clinicaRepository;

    @BeforeEach
    void setup() {
        com.multiclinicas.api.models.Clinica clinica = new com.multiclinicas.api.models.Clinica();
        clinica.setId(1L);
        clinica.setAtivo(true);
        when(clinicaRepository.findById(1L)).thenReturn(java.util.Optional.of(clinica));
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request se header X-Clinic-ID estiver faltando")
    void shouldReturn400WhenHeaderMissing() throws Exception {
        PlanoSaudeCreateDTO dto = new PlanoSaudeCreateDTO("Unimed", true);

        mockMvc.perform(post("/planos-saude")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest()); // O Spring valida a falta do header
    }

    @Test
    @DisplayName("Deve criar plano com sucesso quando payload e header válidos")
    void shouldCreatePlanoWhenValid() throws Exception {
        Long clinicId = 1L;
        PlanoSaudeCreateDTO dto = new PlanoSaudeCreateDTO("Unimed", true);

        // Mocks
        PlanoSaude entity = new PlanoSaude();
        entity.setId(10L);
        entity.setNome("Unimed");

        PlanoSaudeDTO responseDTO = new PlanoSaudeDTO(10L, "Unimed", true);

        when(planoSaudeMapper.toEntity(any())).thenReturn(entity);
        when(planoSaudeService.create(eq(clinicId), any())).thenReturn(entity);
        when(planoSaudeMapper.toDTO(entity)).thenReturn(responseDTO);

        mockMvc.perform(post("/planos-saude")
                .header("X-Clinic-ID", clinicId) // Simulando o Header
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.nome").value("Unimed"));
    }

    @Test
    @DisplayName("Deve validar campos obrigatórios do DTO")
    void shouldValidateMandatoryFields() throws Exception {
        // Nome em branco
        PlanoSaudeCreateDTO dto = new PlanoSaudeCreateDTO("", true);

        mockMvc.perform(post("/planos-saude")
                .header("X-Clinic-ID", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.nome").exists()); // Verifica se o erro do campo nome retornou
    }
}