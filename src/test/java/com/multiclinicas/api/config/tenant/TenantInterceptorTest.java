package com.multiclinicas.api.config.tenant;

import com.multiclinicas.api.exceptions.ResourceNotFoundException;
import com.multiclinicas.api.repositories.ClinicaRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantInterceptorTest {

    @Mock
    private ClinicaRepository clinicaRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private TenantInterceptor tenantInterceptor;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void preHandle_ShouldReturnTrue_WhenHeaderIsValidAndClinicExists() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("X-Clinic-ID")).thenReturn("1");
        com.multiclinicas.api.models.Clinica clinica = new com.multiclinicas.api.models.Clinica();
        clinica.setId(1L);
        clinica.setAtivo(true);
        when(clinicaRepository.findById(1L)).thenReturn(java.util.Optional.of(clinica));

        boolean result = tenantInterceptor.preHandle(request, response, new Object());

        assertTrue(result);
        assertEquals(1L, TenantContext.getClinicId());
    }

    @Test
    void preHandle_ShouldThrowException_WhenHeaderIsMissing() {
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("X-Clinic-ID")).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> tenantInterceptor.preHandle(request, response, new Object()));
    }

    @Test
    void preHandle_ShouldThrowException_WhenHeaderIsBlank() {
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("X-Clinic-ID")).thenReturn("");

        assertThrows(IllegalArgumentException.class,
                () -> tenantInterceptor.preHandle(request, response, new Object()));
    }

    @Test
    void preHandle_ShouldThrowException_WhenHeaderIsNotANumber() {
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("X-Clinic-ID")).thenReturn("abc");

        assertThrows(IllegalArgumentException.class,
                () -> tenantInterceptor.preHandle(request, response, new Object()));
    }

    @Test
    void preHandle_ShouldThrowException_WhenClinicDoesNotExist() {
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("X-Clinic-ID")).thenReturn("999");
        when(clinicaRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> tenantInterceptor.preHandle(request, response, new Object()));
    }

    @Test
    void preHandle_ShouldReturnFalse_WhenClinicIsInactive() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("X-Clinic-ID")).thenReturn("1");
        com.multiclinicas.api.models.Clinica clinica = new com.multiclinicas.api.models.Clinica();
        clinica.setId(1L);
        clinica.setAtivo(false);
        when(clinicaRepository.findById(1L)).thenReturn(java.util.Optional.of(clinica));

        boolean result = tenantInterceptor.preHandle(request, response, new Object());

        assertFalse(result);
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "Tenant inativo");
    }
}
