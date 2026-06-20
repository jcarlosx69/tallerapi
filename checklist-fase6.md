# ✅ Checklist final — Fase 6 (Documentación y cierre)

Basada en la sección 5.3 de la especificación del proyecto. Repásala completa antes de dar TallerAPI por cerrado e incluirlo en el CV.

- [ ] El repositorio es público y el README se entiende sin contexto previo (alguien ajeno al proyecto puede saber qué hace y por qué existe en menos de un minuto)
- [ ] Existe al menos una URL activa o un vídeo corto que demuestre que la API funciona desplegada
- [ ] El pipeline de CI/CD aparece en la pestaña "Actions" de GitHub con histórico de ejecuciones en verde
- [ ] Los 94 tests pasan y puedes indicar, aunque sea de forma aproximada, el porcentaje de cobertura
- [ ] No hay credenciales, claves ni tokens visibles en el historial de commits (revisa también commits antiguos, no solo el estado actual del código)
- [ ] El README menciona explícitamente las palabras clave: **Java 21, Spring Boot 3, Docker, AWS, JWT, CI/CD, tests** — para superar filtros ATS por palabras clave

---

### 💡 Notas rápidas

- Para revisar credenciales en el historial completo (no solo el HEAD actual), una búsqueda con `git log -p` o una herramienta como `gitleaks` / `trufflehog` es más fiable que una revisión manual.
- Si el entorno de AWS está apagado para ahorrar créditos en el momento de aplicar a una oferta, considera grabar un vídeo corto (1-2 min) mostrando Swagger UI en funcionamiento como alternativa a la URL en vivo.
- Una vez marcados todos los puntos, actualiza la frase resumen del CV con el grado de avance real del proyecto.
