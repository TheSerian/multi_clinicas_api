package com.multiclinicas.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.multiclinicas.api.config.WebConfig;
import com.multiclinicas.api.config.tenant.TenantContext;
import com.multiclinicas.api.config.tenant.TenantInterceptor;
import com.multiclinicas.api.dtos.EspecialidadeCreateDTO;
import com.multiclinicas.api.dtos.EspecialidadeDTO;
import com.multiclinicas.api.exceptions.BusinessException;
import com.multiclinicas.api.exceptions.ResourceNotFoundException;
import com.multiclinicas.api.mappers.EspecialidadeMapper;
import com.multiclinicas.api.models.Clinica;
import com.multiclinicas.api.models.Especialidade;
import com.multiclinicas.api.repositories.ClinicaRepository;
import com.multiclinicas.api.services.EspecialidadeService;
import org.junit.jupiter.api.AfterEach;
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

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = EspecialidadeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ WebConfig.class, TenantInterceptor.class })
class EspecialidadeControllerTest {

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.multiclinicas.api.config.JwtAuthenticationFilter jwtAuthenticationFilter;


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EspecialidadeService especialidadeService;

    @MockitoBean
    private EspecialidadeMapper especialidadeMapper;

    @MockitoBean
    private ClinicaRepository clinicaRepository;

    private Especialidade especialidade;
    private EspecialidadeDTO especialidadeDTO;
    private EspecialidadeCreateDTO createDTO;
    private Clinica clinica;
    private Long clinicaId;
    private Long especialidadeId;

    @BeforeEach
    void setUp() {
        clinicaId = 1L;
        especialidadeId = 1L;

        com.multiclinicas.api.models.Clinica clinica = new com.multiclinicas.api.models.Clinica();
        clinica.setId(1L);
        clinica.setAtivo(true);
        when(clinicaRepository.findById(clinicaId)).thenReturn(java.util.Optional.of(clinica));

        TenantContext.setClinicId(clinicaId);

        clinica = new Clinica();
        clinica.setId(clinicaId);
        clinica.setNomeFantasia("Clínica Saúde Total");


        especialidade = new Especialidade();
        especialidade.setId(especialidadeId);
        especialidade.setNome("Cardiologia");
        especialidade.setClinica(clinica);

        especialidadeDTO = new EspecialidadeDTO(
                especialidadeId,
                "Cardiologia",
                clinicaId,
                "Clínica Saúde Total"
        );

        createDTO = new EspecialidadeCreateDTO("Cardiologia");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("GET /especialidades - Deve listar todas as especialidades da clínica")
    void deveListarTodasEspecialidadesDaClinica() throws Exception {
        Especialidade esp1 = new Especialidade(1L, clinica, "Cardiologia");
        Especialidade esp2 = new Especialidade(2L, clinica, "Pediatria");
        Especialidade esp3 = new Especialidade(3L, clinica, "Ortopedia");

        EspecialidadeDTO dto1 = new EspecialidadeDTO(1L, "Cardiologia", clinicaId, "Clínica Saúde Total");
        EspecialidadeDTO dto2 = new EspecialidadeDTO(2L, "Pediatria", clinicaId, "Clínica Saúde Total");
        EspecialidadeDTO dto3 = new EspecialidadeDTO(3L, "Ortopedia", clinicaId, "Clínica Saúde Total");

        List<Especialidade> especialidades = Arrays.asList(esp1, esp2, esp3);

        when(especialidadeService.findAllByClinicId(clinicaId)).thenReturn(especialidades);
        when(especialidadeMapper.toDTO(esp1)).thenReturn(dto1);
        when(especialidadeMapper.toDTO(esp2)).thenReturn(dto2);
        when(especialidadeMapper.toDTO(esp3)).thenReturn(dto3);

        mockMvc.perform(get("/especialidades")
                        .header("X-Clinic-ID", clinicaId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].nome", is("Cardiologia")))
                .andExpect(jsonPath("$[1].nome", is("Pediatria")))
                .andExpect(jsonPath("$[2].nome", is("Ortopedia")));

        verify(especialidadeService, times(1)).findAllByClinicId(clinicaId);
        verify(especialidadeMapper, times(3)).toDTO(any(Especialidade.class));
    }

    @Test
    @DisplayName("GET /especialidades - Deve retornar lista vazia quando não há especialidades")
    void deveRetornarListaVaziaQuandoNaoHaEspecialidades() throws Exception {
        when(especialidadeService.findAllByClinicId(clinicaId)).thenReturn(Arrays.asList());


        mockMvc.perform(get("/especialidades")
                        .header("X-Clinic-ID", clinicaId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(especialidadeService, times(1)).findAllByClinicId(clinicaId);
    }

    @Test
    @DisplayName("GET /especialidades/{id} - Deve buscar especialidade por ID")
    void deveBuscarEspecialidadePorId() throws Exception {

        when(especialidadeService.findByIdAndClinicId(especialidadeId, clinicaId))
                .thenReturn(especialidade);
        when(especialidadeMapper.toDTO(especialidade)).thenReturn(especialidadeDTO);

        mockMvc.perform(get("/especialidades/{id}", especialidadeId)
                        .header("X-Clinic-ID", clinicaId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(especialidadeId.intValue())))
                .andExpect(jsonPath("$.nome", is("Cardiologia")))
                .andExpect(jsonPath("$.clinicaId", is(clinicaId.intValue())))
                .andExpect(jsonPath("$.nomeClinica", is("Clínica Saúde Total")));

        verify(especialidadeService, times(1)).findByIdAndClinicId(especialidadeId, clinicaId);
        verify(especialidadeMapper, times(1)).toDTO(especialidade);
    }

    @Test
    @DisplayName("GET /especialidades/{id} - Deve retornar 404 quando especialidade não existe")
    void deveRetornar404QuandoEspecialidadeNaoExiste() throws Exception {

        when(especialidadeService.findByIdAndClinicId(999L, clinicaId))
                .thenThrow(new ResourceNotFoundException("Especialidade não encontrada"));

        mockMvc.perform(get("/especialidades/{id}", 999L)
                        .header("X-Clinic-ID", clinicaId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(especialidadeService, times(1)).findByIdAndClinicId(999L, clinicaId);
        verify(especialidadeMapper, never()).toDTO(any());
    }

    @Test
    @DisplayName("POST /especialidades - Deve criar especialidade com sucesso")
    void deveCriarEspecialidadeComSucesso() throws Exception {

        Especialidade novaEspecialidade = new Especialidade();
        novaEspecialidade.setNome("Cardiologia");

        when(especialidadeMapper.toEntity(createDTO)).thenReturn(novaEspecialidade);
        when(especialidadeService.create(clinicaId, novaEspecialidade)).thenReturn(especialidade);
        when(especialidadeMapper.toDTO(especialidade)).thenReturn(especialidadeDTO);

        mockMvc.perform(post("/especialidades")
                        .header("X-Clinic-ID", clinicaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(especialidadeId.intValue())))
                .andExpect(jsonPath("$.nome", is("Cardiologia")))
                .andExpect(jsonPath("$.clinicaId", is(clinicaId.intValue())));

        verify(especialidadeMapper, times(1)).toEntity(createDTO);
        verify(especialidadeService, times(1)).create(clinicaId, novaEspecialidade);
        verify(especialidadeMapper, times(1)).toDTO(especialidade);
    }

    @Test
    @DisplayName("POST /especialidades - Deve retornar 400 quando nome é null")
    void deveRetornar400QuandoNomeNull() throws Exception {
        EspecialidadeCreateDTO dtoInvalido = new EspecialidadeCreateDTO(null);

        mockMvc.perform(post("/especialidades")
                        .header("X-Clinic-ID", clinicaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoInvalido)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(especialidadeMapper, never()).toEntity(any());
        verify(especialidadeService, never()).create(any(), any());
    }

    @Test
    @DisplayName("POST /especialidades - Deve retornar 400 quando nome é vazio")
    void deveRetornar400QuandoNomeVazio() throws Exception {

        EspecialidadeCreateDTO dtoInvalido = new EspecialidadeCreateDTO("");

        mockMvc.perform(post("/especialidades")
                        .header("X-Clinic-ID", clinicaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoInvalido)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(especialidadeMapper, never()).toEntity(any());
        verify(especialidadeService, never()).create(any(), any());
    }

    @Test
    @DisplayName("POST /especialidades - Deve retornar 400 quando nome é muito curto")
    void deveRetornar400QuandoNomeMuitoCurto() throws Exception {

        EspecialidadeCreateDTO dtoInvalido = new EspecialidadeCreateDTO("AB");

        mockMvc.perform(post("/especialidades")
                        .header("X-Clinic-ID", clinicaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoInvalido)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(especialidadeMapper, never()).toEntity(any());
        verify(especialidadeService, never()).create(any(), any());
    }

    @Test
    @DisplayName("POST /especialidades - Deve retornar 409 ao criar especialidade duplicada")
    void deveRetornar409AoCriarEspecialidadeDuplicada() throws Exception {

        Especialidade novaEspecialidade = new Especialidade();
        novaEspecialidade.setNome("Cardiologia");

        when(especialidadeMapper.toEntity(createDTO)).thenReturn(novaEspecialidade);
        when(especialidadeService.create(clinicaId, novaEspecialidade))
                .thenThrow(new BusinessException("Já existe uma especialidade com o nome 'Cardiologia' nesta clínica"));

        mockMvc.perform(post("/especialidades")
                        .header("X-Clinic-ID", clinicaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(especialidadeService, times(1)).create(eq(clinicaId), any(Especialidade.class));
    }

    @Test
    @DisplayName("PUT /especialidades/{id} - Deve atualizar especialidade com sucesso")
    void deveAtualizarEspecialidadeComSucesso() throws Exception {

        EspecialidadeCreateDTO updateDTO = new EspecialidadeCreateDTO("Cardiologia Pediátrica");

        Especialidade especialidadeAtualizada = new Especialidade();
        especialidadeAtualizada.setNome("Cardiologia Pediátrica");

        Especialidade especialidadeRetornada = new Especialidade();
        especialidadeRetornada.setId(especialidadeId);
        especialidadeRetornada.setNome("Cardiologia Pediátrica");
        especialidadeRetornada.setClinica(clinica);

        EspecialidadeDTO dtoAtualizado = new EspecialidadeDTO(
                especialidadeId,
                "Cardiologia Pediátrica",
                clinicaId,
                "Clínica Saúde Total"
        );

        when(especialidadeMapper.toEntity(updateDTO)).thenReturn(especialidadeAtualizada);
        when(especialidadeService.update(especialidadeId, clinicaId, especialidadeAtualizada))
                .thenReturn(especialidadeRetornada);
        when(especialidadeMapper.toDTO(especialidadeRetornada)).thenReturn(dtoAtualizado);

        mockMvc.perform(put("/especialidades/{id}", especialidadeId)
                        .header("X-Clinic-ID", clinicaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(especialidadeId.intValue())))
                .andExpect(jsonPath("$.nome", is("Cardiologia Pediátrica")));

        verify(especialidadeMapper, times(1)).toEntity(updateDTO);
        verify(especialidadeService, times(1)).update(especialidadeId, clinicaId, especialidadeAtualizada);
        verify(especialidadeMapper, times(1)).toDTO(especialidadeRetornada);
    }

    @Test
    @DisplayName("PUT /especialidades/{id} - Deve retornar 400 com dados inválidos")
    void deveRetornar400AoAtualizarComDadosInvalidos() throws Exception {

        EspecialidadeCreateDTO dtoInvalido = new EspecialidadeCreateDTO("");

        mockMvc.perform(put("/especialidades/{id}", especialidadeId)
                        .header("X-Clinic-ID", clinicaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoInvalido)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(especialidadeMapper, never()).toEntity(any());
        verify(especialidadeService, never()).update(any(), any(), any());
    }

    @Test
    @DisplayName("PUT /especialidades/{id} - Deve retornar 409 ao atualizar para nome duplicado")
    void deveRetornar409AoAtualizarParaNomeDuplicado() throws Exception {

        EspecialidadeCreateDTO updateDTO = new EspecialidadeCreateDTO("Pediatria");

        Especialidade especialidadeAtualizada = new Especialidade();
        especialidadeAtualizada.setNome("Pediatria");

        when(especialidadeMapper.toEntity(updateDTO)).thenReturn(especialidadeAtualizada);
        when(especialidadeService.update(eq(especialidadeId), eq(clinicaId), any(Especialidade.class)))
                .thenThrow(new BusinessException("Já existe uma especialidade com o nome 'Pediatria' nesta clínica"));

        mockMvc.perform(put("/especialidades/{id}", especialidadeId)
                        .header("X-Clinic-ID", clinicaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(especialidadeService, times(1))
                .update(eq(especialidadeId), eq(clinicaId), any(Especialidade.class));
    }

    @Test
    @DisplayName("DELETE /especialidades/{id} - Deve deletar especialidade com sucesso")
    void deveDeletarEspecialidadeComSucesso() throws Exception {

        doNothing().when(especialidadeService).delete(especialidadeId, clinicaId);

        mockMvc.perform(delete("/especialidades/{id}", especialidadeId)
                        .header("X-Clinic-ID", clinicaId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNoContent());

        verify(especialidadeService, times(1)).delete(especialidadeId, clinicaId);
    }

    @Test
    @DisplayName("DELETE /especialidades/{id} - Deve retornar 404 ao deletar especialidade inexistente")
    void deveRetornar404AoDeletarEspecialidadeInexistente() throws Exception {

        doThrow(new ResourceNotFoundException("Especialidade não encontrada"))
                .when(especialidadeService).delete(999L, clinicaId);

        mockMvc.perform(delete("/especialidades/{id}", 999L)
                        .header("X-Clinic-ID", clinicaId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(especialidadeService, times(1)).delete(999L, clinicaId);
    }

    @Test
    @DisplayName("POST /especialidades - Deve aceitar nome com caracteres especiais")
    void deveAceitarNomeComCaracteresEspeciais() throws Exception {

        EspecialidadeCreateDTO dtoComAcentos = new EspecialidadeCreateDTO("Cirurgia Pediátrica");

        Especialidade novaEspecialidade = new Especialidade();
        novaEspecialidade.setNome("Cirurgia Pediátrica");

        Especialidade especialidadeCriada = new Especialidade();
        especialidadeCriada.setId(1L);
        especialidadeCriada.setNome("Cirurgia Pediátrica");
        especialidadeCriada.setClinica(clinica);

        EspecialidadeDTO especialidadeComAcentos = new EspecialidadeDTO(
                1L,
                "Cirurgia Pediátrica",
                clinicaId,
                "Clínica Saúde Total"
        );

        when(especialidadeMapper.toEntity(dtoComAcentos)).thenReturn(novaEspecialidade);
        when(especialidadeService.create(clinicaId, novaEspecialidade)).thenReturn(especialidadeCriada);
        when(especialidadeMapper.toDTO(especialidadeCriada)).thenReturn(especialidadeComAcentos);

        mockMvc.perform(post("/especialidades")
                        .header("X-Clinic-ID", clinicaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoComAcentos)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome", is("Cirurgia Pediátrica")));

        verify(especialidadeMapper, times(1)).toEntity(dtoComAcentos);
        verify(especialidadeService, times(1)).create(clinicaId, novaEspecialidade);
    }
}