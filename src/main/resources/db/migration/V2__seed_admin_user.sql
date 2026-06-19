-- =============================================================
-- V2__seed_admin_user.sql
-- TallerAPI - Usuario administrador inicial
--
-- Inserta un usuario ADMIN precargado para poder autenticarse desde
-- el primer arranque, antes de que exista cualquier flujo de registro
-- (que se implementará en la Fase 2).
--
-- Credenciales de portfolio/desarrollo (NO usar en producción real):
--   username: admin
--   password: Admin123!
--
-- El valor de password_hash es el resultado de aplicar BCrypt (coste
-- 10) a "Admin123!". Ver fase1-notas-tecnicas.md para el detalle de
-- cómo se generó este hash y cómo gestionar esta contraseña en un
-- entorno real (variables de entorno / secrets manager, no hardcoded).
-- =============================================================

INSERT INTO usuarios (username, password_hash, rol)
VALUES (
           'admin',
           '$2b$10$oRA./J9eJr/uDjdB1BzSaO06P3bZcu3TkriawJIUw1RXg.oNgnvrS',
           'ADMIN'
       );