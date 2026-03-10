package com.multiclinicas.api.repositories;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.multiclinicas.api.models.Agendamento;
import com.multiclinicas.api.models.enums.StatusAgendamento;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    List<Agendamento> findAllByClinicaId(Long clinicId);

    List<Agendamento> findByPacienteIdAndClinicaIdOrderByDataConsultaDescHoraInicioDesc(Long pacienteId, Long clinicId);
    
    List<Agendamento> findByDataConsultaAndStatus(LocalDate dataConsulta, StatusAgendamento status);

    // Busca agendamentos de um médico em uma data específica (útil para montar a
    // agenda visualmente depois)
    List<Agendamento> findByMedicoIdAndDataConsultaAndClinicaId(Long medicoId, LocalDate dataConsulta, Long clinicId);

    // Validação de Conflito de Horário
    // Verifica se existe algum agendamento que comece antes do fim do novo E
    // termine depois do início do novo
    @Query("""
                SELECT COUNT(a) > 0 FROM Agendamento a
                WHERE a.clinica.id = :clinicId
                AND a.medico.id = :medicoId
                AND a.dataConsulta = :data
                AND a.status <> 'CANCELADO_CLINICA'
                AND a.status <> 'CANCELADO_PACIENTE'
                AND (
                    (a.horaInicio < :fim AND a.horaFim > :inicio)
                )
            """)
    boolean existsConflict(
            @Param("clinicId") Long clinicId,
            @Param("medicoId") Long medicoId,
            @Param("data") LocalDate data,
            @Param("inicio") LocalTime inicio,
            @Param("fim") LocalTime fim);
}