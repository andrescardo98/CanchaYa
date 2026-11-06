# Documentación del proyecto

Esta carpeta trae al repositorio, en texto, los artefactos de análisis y diseño del trabajo de grado. Así se pueden consultar mientras se programa, tanto tú como los agentes de IA, sin abrir los Excel.

**Los Excel siguen siendo la fuente.** Todo lo que hay aquí, salvo este archivo, lo genera `scripts/exportar_docs.py`. No lo edites a mano: cambia el Excel y vuelve a ejecutar el script (ver [Cómo actualizar](#cómo-actualizar)).

## Qué consultar y cuándo

| Necesito… | Archivo |
|---|---|
| Saber qué regla manda cuando dos documentos no coinciden | [decisiones.md](decisiones.md) (D01–D36, fuente de verdad) |
| Implementar una historia de usuario | [historias/HUnn.md](historias/README.md): tarjeta, conversación y criterios de aceptación |
| Atributos, validaciones, combinaciones únicas y políticas de una entidad | [dominio/](dominio/README.md) (modelo de dominio enriquecido) |
| El texto de un mensaje o de un correo | [especificacion/mensajes.md](especificacion/mensajes.md) (códigos `ERR/EXI/INF/CNF-<OBJ>-nn` y `COR-*`) |
| Un valor configurable (plazos, límites, formatos) | [especificacion/parametros.md](especificacion/parametros.md) (`PAR-*`) |
| Reglas que aplican a todas las HU | [especificacion/aspectos-generales.md](especificacion/aspectos-generales.md) (`AG-01` a `AG-23`) |
| Ver la arquitectura elegida en un diagrama | [arquitectura/alternativa-seleccionada.md](arquitectura/alternativa-seleccionada.md) (ALT-01, D30) |
| Cómo se organiza el backend por dentro y qué módulo puede usar a cuál | [arquitectura/arquetipo.md](arquitectura/arquetipo.md), hoja «Estructura del backend» |
| Hosting, base de datos, correo, variables de entorno | [arquitectura/plataforma.md](arquitectura/plataforma.md), hoja «Configuración del entorno» |
| Qué significa un código `ESC-*`, `RT-*`, `RNG-*` o `AC-*` | [arquitectura/](arquitectura/): escenarios de calidad, restricciones y tácticas |
| Clases, paquetes, atributos y métodos | [diseno/clases.md](diseno/clases.md) y [diseno/clases.mmd](diseno/clases.mmd) |
| Componentes e interfaces entre módulos | [diseno/componentes.md](diseno/componentes.md) |
| Tablas, columnas y restricciones de la base de datos | [diseno/entidad-relacion.md](diseno/entidad-relacion.md) y [diseno/entidad-relacion.mmd](diseno/entidad-relacion.mmd) |

**Diagramas.** Cada archivo muestra arriba de su hoja el diagrama que tiene pegado el Excel; las imágenes quedan en la carpeta `img/` de cada sección. El modelo de dominio muestra `Modelo de dominio Ampliado.drawio.png`. Del diagrama de clases y del modelo entidad relación también hay versión en texto (`.mmd`, Mermaid), que es la que mejor lee la IA.

Las pantallas (prototipos en PNG) siguen en la carpeta de documentos, en `1.1.9.5. Artefactos apoyo/Prototipos`. Se traen al repositorio cuando empiece el frontend.

## Cómo actualizar

Desde la raíz del repositorio:

```bash
pip install openpyxl          # solo la primera vez
python scripts/exportar_docs.py
```

Por defecto lee la carpeta `../Documentos` (la que está al lado del repositorio). Si está en otro lugar, pásala como argumento: `python scripts/exportar_docs.py "C:/ruta/Documentos"`.

Después revisa con `git diff docs/` qué cambió y súbelo en un commit `docs: actualizar la documentación exportada`.
