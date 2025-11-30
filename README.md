# 🏥 Sistema de Agendamento Multi-Tenant para Clínicas (SaaS)

## 1. Visão Geral do Produto
Este projeto consiste no desenvolvimento de uma plataforma **SaaS (Software as a Service) Multi-Tenant** voltada para a gestão de agendamentos médicos. O objetivo é fornecer a clínicas de pequeno e médio porte uma solução digital para expor suas agendas, permitir que pacientes marquem consultas online e reduzir o absenteísmo (*no-show*) através de notificações automáticas.

Diferente de um software instalado localmente, esta plataforma permite que múltiplas clínicas utilizem o mesmo software, onde cada uma possui seu ambiente isolado e personalizado via subdomínio (ex: `clinica-vida.sistema.com` e `cardio-center.sistema.com`).
---

## 2. Arquitetura do Sistema

### 2.1 Modelo Multi-Tenant
O sistema adota uma estratégia de **Banco de Dados Compartilhado (Shared Database)** com isolamento lógico.

* **Identificação do Tenant:** O sistema identifica qual clínica está sendo acessada através da URL (Subdomínio).
* **Isolamento de Dados:** Todas as consultas ao banco de dados filtram obrigatoriamente pelo `clinic_id`.
* **Escalabilidade:** Permite a adição de novas clínicas sem necessidade de provisionar nova infraestrutura.

### 2.2 Stack Tecnológica (MVP)
* **Frontend:** React com Next.js.
* **Backend:** Java com Spring Boot.
* **Banco de Dados:** PostgreSQL ou MySQL (Relacional).
* **Mensageria/Jobs:** Redis (para filas de e-mail).

---

## 3. Atores do Sistema

| Ator | Descrição | Permissões Chave |
| :--- | :--- | :--- |
| **Visitante** | Usuário não autenticado acessando o portal da clínica. | Visualizar médicos, especialidades e horários livres. |
| **Paciente** | Usuário final que consome o serviço médico. | Agendar (Particular ou Convênio), visualizar histórico e cancelar consultas. |
| **Recepcionista** | Funcionário da clínica (Operacional). | Gerenciar médicos, inserir tokens de autorização de convênio e gerenciar agenda. |
| **Admin da Clínica** | Gestor da unidade (Gerencial). | Cadastrar planos de saúde, equipe e dados da clínica. |

---

## 4. Funcionalidades por Módulo

### 4.1 Módulo Público (Agendamento)
Focado na conversão e usabilidade para o paciente.

* **Catálogo de Especialidades:** Listagem das áreas médicas atendidas pela clínica.
* **Busca de Médicos:** Listagem de profissionais filtrada por especialidade.
* **Seleção de Modalidade:** Escolha entre atendimento **Particular** ou **Convênio** (selecionando o plano específico).
* **Calendário de Disponibilidade:** Visualização dos slots livres baseados na duração da consulta configurada para cada médico.
* **Fluxo de Agendamento Híbrido:**
    * Permite iniciar a escolha do horário como visitante.
    * Exige Login ou Cadastro rápido apenas no momento de confirmar a reserva.

### 4.2 Módulo do Paciente (Área Logada)
Focado na autogestão.

* **Meus Agendamentos:** Lista de consultas futuras e passadas com detalhe do tipo de pagamento.
* **Cancelamento:** Botão para cancelar consultas futuras (libera o horário na hora).
* **Segurança:** O cancelamento exige autenticação para evitar fraudes ou erros.

### 4.3 Módulo Administrativo (Backoffice da Clínica)
Painel de controle para a equipe interna.

* **Gestão de Planos de Saúde:** Cadastro dos convênios aceitos pela clínica (ex: Unimed, Bradesco).
* **Gestão de Corpo Clínico:** Cadastro de médicos, incluindo a definição do **tempo padrão de consulta** de cada profissional.
* **Validação de Guias:** Interface para o recepcionista inserir o **Token de Autorização** fornecido pelo convênio.
* **Gestão de Grade Horária:** Definição dos blocos de trabalho (ex: Dr. João atende Segundas das 08h às 12h).
* **Bloqueio de Agenda:** Capacidade de bloquear horários manualmente.

### 4.4 Módulo de Notificações (Automático)
Serviço de background para garantir o comparecimento.

* **E-mail de Confirmação:** Disparado imediatamente após o sucesso do agendamento.
* **E-mail de Lembrete:** Disparado automaticamente 24 horas antes do horário da consulta.

---

## 5. Modelo de Dados (Entidades Principais e Atributos)
Abaixo estão listadas as entidades do banco de dados.
*Nota: `PK` = Chave Primária, `FK` = Chave Estrangeira.*

### 5.1 Tabela Global
* **Clinicas (Tenants)**
    * `id` (PK): UUID ou Long.
    * `nome_fantasia`: String.
    * `subdominio`: String (Unique). Identificador chave para o multi-tenant.
    * `ativo`: Boolean.
    * `created_at`: Timestamp.

### 5.2 Tabelas por Tenant (Todas possuem `clinic_id`)

* **Planos_Saude (Novidade)**
    * `id` (PK).
    * `clinic_id` (FK).
    * `nome`: String (Ex: "Unimed Premium").
    * `ativo`: Boolean.

* **Usuarios_Admin**
    * `id` (PK).
    * `clinic_id` (FK).
    * `email`: String (Login).
    * `role`: Enum (ADMIN, RECEPCIONISTA).

* **Medicos**
    * `id` (PK).
    * `clinic_id` (FK).
    * `nome`: String.
    * `crm`: String.
    * `duracao_consulta`: Integer (Tempo padrão de atendimento em minutos. Ex: 30).
    * `ativo`: Boolean.

* **Grades_Horario (Configuração de Agenda)**
    * `id` (PK).
    * `medico_id` (FK).
    * `dia_semana`: Integer (0=Dom, 1=Seg, ... 6=Sab).
    * `hora_inicio`: Time (Ex: 08:00).
    * `hora_fim`: Time (Ex: 12:00).
    * *Nota: A duração da consulta foi movida para a tabela Medicos.*

* **Agendamentos (O Core do Sistema)**
    * `id` (PK).
    * `clinic_id` (FK).
    * `paciente_id` (FK).
    * `medico_id` (FK).
    * `data_consulta`: Date.
    * `hora_inicio`: Time.
    * `hora_fim`: Time.
    * `status`: Enum (AGENDADO, REALIZADO, etc).
    * `tipo_pagamento`: Enum (PARTICULAR, CONVENIO).
    * `plano_saude_id`: FK (Obrigatório se tipo for CONVENIO).
    * `token_autorizacao`: String (Preenchido pela recepção após aprovação do convênio).

---

## 6. Regras de Negócio Críticas (MVP)

1.  **Unicidade de Horário:** O sistema não pode permitir dois agendamentos para o mesmo médico no mesmo horário.
2.  **Cálculo de Slots:** Os horários disponíveis são gerados dinamicamente baseados na `hora_inicio` e `hora_fim` da Grade e divididos pela `duracao_consulta` definida no perfil do Médico.
3.  **Fluxo de Convênio:**
    * O paciente deve selecionar um plano de saúde válido daquela clínica ao agendar.
    * Para o atendimento ser efetivado, o recepcionista deve inserir o **Token de Autorização** validado junto à operadora.
4.  **Isolamento de Dados:** Um paciente da Clínica A não pode ver médicos ou planos de saúde da Clínica B.

---

## 7. Guia de Execução da API

### 7.1 Pré-requisitos
* **Java 17** ou superior.
* **Maven** 3.8+.
* **PostgreSQL**.

### 7.2 Configuração do Banco de Dados
1.  Crie um banco de dados chamado `clinicas_db`.
2.  Verifique `application.yaml` (User: `spring`, Pass: `123`).

### 7.3 Como Executar
```bash
mvn spring-boot:run
```

## 📌 Recursos do Projeto

- [Diagrama UML](https://drive.google.com/file/d/1-hSBLckVL-tVWdhuoo6YgG5EgTraQPE-/view?usp=sharing)
- [Documentação Geral do Sistema](https://drive.google.com/file/d/1Q-_Ooygm9UTMOKrSYm10joH6w2TF6Z4-/view?usp=sharing)
- [APIDOG](https://3z8mift3kc.apidog.io/)
