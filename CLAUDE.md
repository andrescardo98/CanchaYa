# CLAUDE.md

Las instrucciones para agentes de este repositorio están en [AGENTS.md](AGENTS.md).

@AGENTS.md

## Mensajes de commit

Los mensajes de commit siguen [Conventional Commits](https://www.conventionalcommits.org/es/v1.0.0/):

```
<tipo>(<alcance opcional>): <descripción>

[cuerpo opcional]

[pie opcional]
```

- **Tipos**: `feat` (nueva funcionalidad), `fix` (corrección de error), `refactor`, `test`, `docs`, `style`, `perf`, `build`, `ci`, `chore`, `revert`.
- **Alcance**: el módulo o contexto afectado, en minúsculas, por ejemplo `reservas`, `escenarios`, `identidad`, `ubicacion`, `frontend`, `infra`.
- **Descripción**: en español, en imperativo, en minúscula y sin punto final, de máximo 72 caracteres. Por ejemplo: `feat(reservas): validar el límite de 2 horas por día`.
- **Cuerpo**: explica el qué y el porqué. Cita la HU o la decisión cuando aplique (`HU21`, `D10`).
- **Cambios que rompen compatibilidad**: agrega `!` después del tipo o del alcance (`feat(api)!: ...`) y un pie `BREAKING CHANGE: <detalle>`.
