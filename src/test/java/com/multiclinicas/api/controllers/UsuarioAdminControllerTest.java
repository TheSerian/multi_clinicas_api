package com.multiclinicas.api.controllers;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.multiclinicas.api.config.WebConfig;
import com.multiclinicas.api.config.tenant.TenantInterceptor;
import com.multiclinicas.api.dtos.CreateEnderecoDTO;
import com.multiclinicas.api.dtos.EnderecoDTO;
import com.multiclinicas.api.dtos.UsuarioAdminCreateDTO;
import com.multiclinicas.api.dtos.UsuarioAdminDTO;
import com.multiclinicas.api.exceptions.ResourceNotFoundException;
import com.multiclinicas.api.mappers.UsuarioAdminMapper;
import com.multiclinicas.api.models.Endereco;
import com.multiclinicas.api.models.UsuarioAdmin;
import com.multiclinicas.api.models.enums.Role;
import com.multiclinicas.api.repositories.ClinicaRepository;
import com.multiclinicas.api.services.UsuarioAdminService;

@WebMvcTest(UsuarioAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ WebConfig.class, TenantInterceptor.class })
class UsuarioAdminControllerTest {

    @MockitoBean
    private com.multiclinicas.api.config.JwtAuthenticationFilter jwtAuthenticationFilter;


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UsuarioAdminService usuarioAdminService;

    @MockitoBean
    private UsuarioAdminMapper usuarioAdminMapper;

    @MockitoBean
    private ClinicaRepository clinicaRepository;

    private final Long clinicId = 1L;
    private UsuarioAdmin usuarioAdmin;
    private UsuarioAdminDTO usuarioAdminDTO;
    private UsuarioAdminCreateDTO usuarioAdminCreateDTO;
    private EnderecoDTO enderecoDTO;

    @BeforeEach
    void setup() {
        com.multiclinicas.api.models.Clinica clinica = new com.multiclinicas.api.models.Clinica();
        clinica.setId(clinicId);
        clinica.setAtivo(true);
        when(clinicaRepository.findById(clinicId)).thenReturn(java.util.Optional.of(clinica));

        Endereco endereco = new Endereco(1L, "12345-678", "Rua Teste", "123", null, "Bairro Teste", "Cidade Teste",
                "TS", "Brasil");
        enderecoDTO = new EnderecoDTO(1L, "12345-678", "Rua Teste", "123", null, "Bairro Teste", "Cidade Teste", "TS",
                "Brasil");
        CreateEnderecoDTO createEnderecoDTO = new CreateEnderecoDTO("12345-678", "Rua Teste", "123", null,
                "Bairro Teste", "Cidade Teste", "TS", "Brasil");

        usuarioAdmin = new UsuarioAdmin();
        usuarioAdmin.setId(1L);
        usuarioAdmin.setNome("Admin Teste");
        usuarioAdmin.setCpf("123.456.789-00");
        usuarioAdmin.setEmail("admin@teste.com");
        usuarioAdmin.setRole(Role.ADMIN);
        usuarioAdmin.setEndereco(endereco);

        usuarioAdminDTO = new UsuarioAdminDTO(1L, "Admin Teste", "123.456.789-00", "99999-9999", null,
                "admin@teste.com", Role.ADMIN, enderecoDTO);
        usuarioAdminCreateDTO = new UsuarioAdminCreateDTO("Admin Teste", "123.456.789-00", "99999-9999", null,
                "admin@teste.com", "senha123", Role.ADMIN, createEnderecoDTO);
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request se header X-Clinic-ID estiver faltando")
    void shouldReturn400WhenHeaderMissing() throws Exception {
        mockMvc.perform(get("/usuario-admin"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve retornar lista de usuários admin para uma clínica")
    void shouldReturnListOfAdminsForClinic() throws Exception {
        when(usuarioAdminService.findAllByClinicId(clinicId)).thenReturn(List.of(usuarioAdmin));
        when(usuarioAdminMapper.toDTO(any(UsuarioAdmin.class))).thenReturn(usuarioAdminDTO);

        mockMvc.perform(get("/usuario-admin")
                .header("X-Clinic-ID", clinicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(usuarioAdmin.getId().longValue()))
                .andExpect(jsonPath("$[0].nome").value(usuarioAdmin.getNome()));
    }

    @Test
    @DisplayName("Deve criar um usuário admin com sucesso")
    void shouldCreateAdminSuccessfully() throws Exception {
        when(usuarioAdminMapper.toEntity(any(UsuarioAdminCreateDTO.class))).thenReturn(usuarioAdmin);
        when(usuarioAdminService.createUsuarioAdmin(eq(clinicId), any(UsuarioAdmin.class))).thenReturn(usuarioAdmin);
        when(usuarioAdminMapper.toDTO(any(UsuarioAdmin.class))).thenReturn(usuarioAdminDTO);

        mockMvc.perform(post("/usuario-admin")
                .header("X-Clinic-ID", clinicId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(usuarioAdminCreateDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(usuarioAdmin.getId().longValue()))
                .andExpect(jsonPath("$.nome").value(usuarioAdmin.getNome()))
                .andExpect(jsonPath("$.email").value(usuarioAdmin.getEmail()));
    }

    @Test
    @DisplayName("Deve atualizar um usuário admin com sucesso")
    void shouldUpdateAdminSuccessfully() throws Exception {
        Long userId = 1L;
        UsuarioAdmin updatedUser = new UsuarioAdmin();
        updatedUser.setId(userId);
        updatedUser.setNome("Admin Atualizado");
        updatedUser.setEmail("admin.atualizado@teste.com");

        UsuarioAdminDTO updatedDto = new UsuarioAdminDTO(userId, "Admin Atualizado", null, null, null,
                "admin.atualizado@teste.com", Role.ADMIN, null);

        when(usuarioAdminMapper.toEntity(any(UsuarioAdminCreateDTO.class))).thenReturn(usuarioAdmin);
        when(usuarioAdminService.updateUsuarioAdmin(eq(userId), eq(clinicId), any(UsuarioAdmin.class)))
                .thenReturn(updatedUser);
        when(usuarioAdminMapper.toDTO(any(UsuarioAdmin.class))).thenReturn(updatedDto);

        mockMvc.perform(put("/usuario-admin/{id}", userId)
                .header("X-Clinic-ID", clinicId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(usuarioAdminCreateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.longValue()))
                .andExpect(jsonPath("$.nome").value("Admin Atualizado"))
                .andExpect(jsonPath("$.email").value("admin.atualizado@teste.com"));
    }

    @Test
    @DisplayName("Deve retornar 404 ao tentar atualizar usuário inexistente")
    void shouldReturn404WhenUpdatingNonExistentUser() throws Exception {
        Long nonExistentId = 99L;
        when(usuarioAdminMapper.toEntity(any(UsuarioAdminCreateDTO.class))).thenReturn(usuarioAdmin);
        when(usuarioAdminService.updateUsuarioAdmin(eq(nonExistentId), eq(clinicId), any(UsuarioAdmin.class)))
                .thenThrow(new ResourceNotFoundException("Usuário não encontrado"));

        mockMvc.perform(put("/usuario-admin/{id}", nonExistentId)
                .header("X-Clinic-ID", clinicId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(usuarioAdminCreateDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve deletar um usuário admin com sucesso")
    void shouldDeleteAdminSuccessfully() throws Exception {
        Long userId = 1L;
        doNothing().when(usuarioAdminService).delete(userId, clinicId);

        mockMvc.perform(delete("/usuario-admin/{id}", userId)
                .header("X-Clinic-ID", clinicId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Deve retornar 404 ao tentar deletar usuário inexistente")
    void shouldReturn404WhenDeletingNonExistentUser() throws Exception {
        Long nonExistentId = 99L;
        doThrow(new ResourceNotFoundException("Usuário não encontrado"))
                .when(usuarioAdminService).delete(nonExistentId, clinicId);

        mockMvc.perform(delete("/usuario-admin/{id}", nonExistentId)
                .header("X-Clinic-ID", clinicId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve retornar 400 quando o estado no endereço for inválido (tamanho excedido)")
    void shouldReturn400WhenEnderecoStateIsInvalid() throws Exception {
        CreateEnderecoDTO enderecoInvalido = new CreateEnderecoDTO(
            "12345-678", "Rua Teste", "123", null, 
            "Bairro Teste", "Cidade Teste", 
            "São Paulo",
            "Brasil"
        );

        UsuarioAdminCreateDTO dtoInvalido = new UsuarioAdminCreateDTO(
            "Admin Teste", "123.456.789-00", "99999-9999", null,
            "admin@teste.com", "senha123", Role.ADMIN, 
            enderecoInvalido
        );

        mockMvc.perform(post("/usuario-admin")
                .header("X-Clinic-ID", clinicId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dtoInvalido)))
                .andExpect(status().isBadRequest());
    }
}