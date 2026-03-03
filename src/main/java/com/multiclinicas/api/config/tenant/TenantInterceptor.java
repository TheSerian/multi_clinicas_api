package com.multiclinicas.api.config.tenant;

import com.multiclinicas.api.exceptions.ResourceNotFoundException;
import com.multiclinicas.api.repositories.ClinicaRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
@RequiredArgsConstructor
public class TenantInterceptor implements HandlerInterceptor {

    private final ClinicaRepository clinicaRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // Ignora requisições de pre-flight do CORS
        if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
            return true;
        }

        String clinicIdHeader = request.getHeader("X-Clinic-ID");

        // 1. Valida se o header está presente
        if (clinicIdHeader == null || clinicIdHeader.isBlank()) {
            throw new IllegalArgumentException("Header X-Clinic-ID é obrigatório");
        }

        try {
            Long clinicId = Long.parseLong(clinicIdHeader);

            // 2. Valida se a clínica existe e está ativa
            java.util.Optional<com.multiclinicas.api.models.Clinica> clinicaOpt = clinicaRepository.findById(clinicId);
            if (clinicaOpt.isEmpty()) {
                throw new ResourceNotFoundException("Clínica informada no cabeçalho não encontrada: " + clinicId);
            }

            if (!clinicaOpt.get().getAtivo()) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Tenant inativo");
                return false;
            }

            // 3. Define no contexto para uso global
            TenantContext.setClinicId(clinicId);

            return true;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Header X-Clinic-ID deve ser um número válido");
        }
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
            ModelAndView modelAndView) {
        // Limpa o contexto ao final da requisição para evitar memory leaks em threads
        // reutilizadas
        TenantContext.clear();
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
            Exception ex) {
        TenantContext.clear();
    }
}
