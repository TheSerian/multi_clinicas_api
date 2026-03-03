-- Migration V3: Adiciona suporte a SUPER_ADMIN e permissão para clinic_id nulo

-- 1. Alterar a tabela para permitir que clinic_id seja NULO (para o SUPER_ADMIN)
ALTER TABLE usuarios_admin ALTER COLUMN clinic_id DROP NOT NULL;

-- 2. Inserir o usuário padrão SUPER_ADMIN
-- O password abaixo é o hash bcrypt para "123456"
INSERT INTO usuarios_admin (clinic_id, nome, email, senha_hash, role)
VALUES (NULL, 'Super Administrador', 'admin@multiclinicas.com', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjIQiqf1VG', 'SUPER_ADMIN');
