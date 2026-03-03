package com.multiclinicas.api.services;

import com.multiclinicas.api.exceptions.BusinessException;
import com.multiclinicas.api.exceptions.ResourceConflictException;
import com.multiclinicas.api.exceptions.ResourceNotFoundException;
import com.multiclinicas.api.models.Clinica;
import com.multiclinicas.api.repositories.ClinicaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClinicaServiceTest {

    @org.mockito.Mock
    private com.multiclinicas.api.repositories.UsuarioAdminRepository usuarioAdminRepository;


    @org.mockito.Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;


    @Mock
    private ClinicaRepository clinicaRepository;

    @InjectMocks
    private ClinicaServiceImpl clinicaService;

    @Test
    @DisplayName("Deve retornar todas as clínicas")
    void shouldReturnAllClinicas() {
        // Given
        Clinica clinica1 = new Clinica();
        clinica1.setId(1L);
        clinica1.setNomeFantasia("Clinica A");

        Clinica clinica2 = new Clinica();
        clinica2.setId(2L);
        clinica2.setNomeFantasia("Clinica B");

        when(clinicaRepository.findAll()).thenReturn(List.of(clinica1, clinica2));

        // When
        List<Clinica> result = clinicaService.findAll();

        // Then
        assertThat(result).hasSize(2);
        verify(clinicaRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Deve retornar clínica por ID quando existir")
    void shouldReturnClinicaByIdWhenExists() {
        // Given
        Long id = 1L;
        Clinica clinica = new Clinica();
        clinica.setId(id);
        when(clinicaRepository.findById(id)).thenReturn(Optional.of(clinica));

        // When
        Clinica result = clinicaService.findById(id);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id);
        verify(clinicaRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("Deve lançar exceção ao buscar clínica inexistente por ID")
    void shouldThrowExceptionWhenClinicaNotFoundById() {
        // Given
        Long id = 1L;
        when(clinicaRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> clinicaService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Clínica não encontrada com ID: " + id);
    }

    @Test
    @DisplayName("Deve criar clínica com sucesso")
    void shouldCreateClinicaSuccessfully() {
        // Given
        Clinica clinica = new Clinica();
        clinica.setSubdominio("clinica-teste");

        when(clinicaRepository.existsBySubdominio("clinica-teste")).thenReturn(false);
        when(clinicaRepository.save(any(Clinica.class))).thenReturn(clinica);
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");

        // When
        Clinica result = clinicaService.create(clinica, "Admin", "admin@teste.local", "123456");

        // Then
        assertThat(result).isNotNull();
        verify(clinicaRepository).existsBySubdominio("clinica-teste");
        verify(clinicaRepository).save(clinica);
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar clínica com subdomínio existente")
    void shouldThrowExceptionWhenCreatingClinicaWithExistingSubdomain() {
        // Given
        Clinica clinica = new Clinica();
        clinica.setSubdominio("clinica-existente");

        when(clinicaRepository.existsBySubdominio("clinica-existente")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> clinicaService.create(clinica, "Admin", "admin@teste.local", "123456"))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Subdomínio já está em uso");

        verify(clinicaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve atualizar clínica com sucesso")
    void shouldUpdateClinicaSuccessfully() {
        // Given
        Long id = 1L;
        Clinica clinicaExistente = new Clinica();
        clinicaExistente.setId(id);
        clinicaExistente.setSubdominio("sub-antigo");
        clinicaExistente.setNomeFantasia("Nome Antigo");

        Clinica clinicaAtualizada = new Clinica();
        clinicaAtualizada.setSubdominio("sub-novo");
        clinicaAtualizada.setNomeFantasia("Nome Novo");

        when(clinicaRepository.findById(id)).thenReturn(Optional.of(clinicaExistente));
        when(clinicaRepository.existsBySubdominio("sub-novo")).thenReturn(false);
        when(clinicaRepository.save(any(Clinica.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Clinica result = clinicaService.update(id, clinicaAtualizada);

        // Then
        assertThat(result.getNomeFantasia()).isEqualTo("Nome Novo");
        assertThat(result.getSubdominio()).isEqualTo("sub-novo");
        verify(clinicaRepository).save(clinicaExistente);
    }

    @Test
    @DisplayName("Deve lançar exceção ao atualizar clínica com subdomínio já em uso por outra")
    void shouldThrowExceptionWhenUpdatingClinicaWithExistingSubdomain() {
        // Given
        Long id = 1L;
        Clinica clinicaExistente = new Clinica();
        clinicaExistente.setId(id);
        clinicaExistente.setSubdominio("sub-antigo");

        Clinica clinicaAtualizada = new Clinica();
        clinicaAtualizada.setSubdominio("sub-em-uso");

        when(clinicaRepository.findById(id)).thenReturn(Optional.of(clinicaExistente));
        when(clinicaRepository.existsBySubdominio("sub-em-uso")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> clinicaService.update(id, clinicaAtualizada))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Novo subdomínio já está em uso");

        verify(clinicaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve deletar clínica com sucesso")
    void shouldDeleteClinicaSuccessfully() {
        // Given
        Long id = 1L;
        when(clinicaRepository.existsById(id)).thenReturn(true);

        // When
        clinicaService.delete(id);

        // Then
        verify(clinicaRepository).deleteById(id);
    }

    @Test
    @DisplayName("Deve lançar exceção ao deletar clínica inexistente")
    void shouldThrowExceptionWhenDeletingNonExistentClinica() {
        // Given
        Long id = 1L;
        when(clinicaRepository.existsById(id)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> clinicaService.delete(id))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(clinicaRepository, never()).deleteById(any());
    }
}
