"""Exporta los artefactos del trabajo de grado (archivos .xlsx) a Markdown en docs/.

Para qué sirve: los agentes de IA y el editor de código no leen bien los Excel.
Este script copia su contenido a archivos de texto dentro del repositorio para que
se puedan consultar mientras se programa.

Los Excel siguen siendo la fuente: los archivos de docs/ que genera este script
no se editan a mano. Si cambia un Excel, se vuelve a ejecutar el script.

Uso (desde la raíz del repositorio):
    pip install openpyxl
    python scripts/exportar_docs.py [carpeta_documentos]

Por defecto, la carpeta de documentos es ../Documentos (al lado del repositorio).
"""

from __future__ import annotations

import html
import posixpath
import re
import shutil
import sys
import warnings
import zipfile
from datetime import date
from pathlib import Path

import openpyxl

warnings.filterwarnings("ignore", category=UserWarning, module="openpyxl")

RAIZ_REPO = Path(__file__).resolve().parent.parent
DOCS = RAIZ_REPO / "docs"

ESTRATEGICOS = "1. Artefactos proyecto/1.1. Artefactos estratégicos"
TECNICOS = "1. Artefactos proyecto/1.2. Artefactos técnicos"
APOYO = f"{ESTRATEGICOS}/1.1.9. Especificación funcional/1.1.9.5. Artefactos apoyo"

AVISO = (
    "> Archivo generado por `scripts/exportar_docs.py` a partir de `{origen}`. "
    "No lo edites a mano: cambia el Excel y vuelve a ejecutar el script.\n"
)


# --------------------------------------------------------------------------- utilidades


def texto(valor) -> str:
    """Convierte el valor de una celda en texto apto para una tabla Markdown."""
    if valor is None:
        return ""
    if isinstance(valor, float) and valor.is_integer():
        valor = int(valor)
    if isinstance(valor, date):
        valor = valor.strftime("%d/%m/%Y")
    s = str(valor).strip()
    s = s.replace("|", "\\|").replace("\r", "")
    return s.replace("\n", "<br>")


def tabla(encabezados: list[str], filas: list[list]) -> str:
    """Arma una tabla Markdown. Omite las columnas que quedan vacías en todas las filas."""
    columnas = [
        i for i in range(len(encabezados)) if any(texto(f[i]) for f in filas if i < len(f))
    ] or list(range(len(encabezados)))
    lineas = ["| " + " | ".join(texto(encabezados[i]) for i in columnas) + " |"]
    lineas.append("|" + "---|" * len(columnas))
    for f in filas:
        celdas = [texto(f[i]) if i < len(f) else "" for i in columnas]
        lineas.append("| " + " | ".join(celdas) + " |")
    return "\n".join(lineas) + "\n"


def filas_con_datos(ws):
    for fila in ws.iter_rows():
        if any(c.value not in (None, "") for c in fila):
            yield fila


def escribir(ruta: Path, contenido: str) -> None:
    ruta.parent.mkdir(parents=True, exist_ok=True)
    ruta.write_text(contenido.rstrip() + "\n", encoding="utf-8", newline="\n")


def libro(origen: Path, relativo: str):
    return openpyxl.load_workbook(origen / relativo, data_only=True)


def nombre_archivo(nombre: str) -> str:
    s = nombre.lower().strip()
    for a, b in zip("áéíóúñü", "aeiounu"):
        s = s.replace(a, b)
    return re.sub(r"[^a-z0-9]+", "-", s).strip("-")


# --------------------------------------------------------------------------- hojas tabulares


def hoja_tabular(ws, primera_columna: tuple[str, ...] = ("Código", "Identificador")) -> str:
    """Hoja con una fila de encabezados (la que empieza por «Código») y filas de datos."""
    filas = list(ws.iter_rows(values_only=True))
    inicio = next(
        (i for i, f in enumerate(filas) if f and texto(f[0]) in primera_columna), None
    )
    if inicio is None:
        return hoja_generica(ws)
    previo = [texto(v) for f in filas[:inicio] for v in f if texto(v)]
    encabezados = list(filas[inicio])
    datos = [list(f) for f in filas[inicio + 1 :] if any(texto(v) for v in f)]
    partes = [p + "\n" for p in previo]
    partes.append(tabla(encabezados, datos))
    return "\n".join(partes)


def hoja_generica(ws) -> str:
    """Hoja con forma libre: cada fila con datos se vuelve una línea con sus celdas."""
    lineas = []
    for fila in filas_con_datos(ws):
        celdas = [texto(c.value) for c in fila if texto(c.value)]
        if celdas[0].startswith("<-Volver"):
            continue
        if len(celdas) == 1 and len(celdas[0]) < 80 and not celdas[0].startswith(("•", "*")):
            lineas.append(f"\n**{celdas[0]}**\n")
        else:
            lineas.append("- " + " · ".join(celdas))
    return "\n".join(lineas) + "\n"


def imagenes_por_hoja(archivo: Path) -> dict[str, list[tuple[str, bytes]]]:
    """Devuelve las imágenes pegadas en cada hoja del Excel (diagramas), por nombre de hoja."""

    def destinos(z, rels: str) -> list[str]:
        if rels not in z.namelist():
            return []
        base = posixpath.dirname(posixpath.dirname(rels))
        xml = z.read(rels).decode("utf-8")
        return [posixpath.normpath(posixpath.join(base, t))
                for t in re.findall(r'Target="([^"]+)"', xml)]

    resultado: dict[str, list[tuple[str, bytes]]] = {}
    with zipfile.ZipFile(archivo) as z:
        libro_xml = z.read("xl/workbook.xml").decode("utf-8")
        rels_xml = z.read("xl/_rels/workbook.xml.rels").decode("utf-8")
        objetivo = dict(re.findall(r'<Relationship [^>]*Id="([^"]+)"[^>]*Target="([^"]+)"', rels_xml))
        objetivo |= {i: t for t, i in
                     re.findall(r'<Relationship [^>]*Target="([^"]+)"[^>]*Id="([^"]+)"', rels_xml)}
        for nombre, rid in re.findall(r'<sheet [^>]*name="([^"]+)"[^>]*r:id="([^"]+)"', libro_xml):
            hoja = posixpath.normpath(posixpath.join("xl", objetivo[rid].lstrip("/").removeprefix("xl/")))
            rels_hoja = posixpath.join(posixpath.dirname(hoja), "_rels", posixpath.basename(hoja) + ".rels")
            for dibujo in destinos(z, rels_hoja):
                rels_dibujo = posixpath.join(posixpath.dirname(dibujo), "_rels",
                                             posixpath.basename(dibujo) + ".rels")
                for media in destinos(z, rels_dibujo):
                    if "/media/" in media:
                        nombre_hoja = html.unescape(nombre)
                        resultado.setdefault(nombre_hoja, []).append((media, z.read(media)))
    return resultado


def exportar_libro(origen: Path, relativo: str, destino: Path, titulo: str,
                   omitir: tuple[str, ...] = (), tabulares: tuple[str, ...] = ()) -> None:
    wb = libro(origen, relativo)
    imagenes = imagenes_por_hoja(origen / relativo)
    partes = [f"# {titulo}\n", AVISO.format(origen=relativo)]
    for ws in wb.worksheets:
        if ws.title in omitir or ws.max_row <= 1 and ws.max_column <= 1 and ws.title not in imagenes:
            continue
        cuerpo = hoja_tabular(ws) if ws.title in tabulares else hoja_generica(ws)
        figuras = []
        for n, (media, datos) in enumerate(imagenes.get(ws.title, []), start=1):
            archivo = f"{destino.stem}-{nombre_archivo(ws.title)}-{n}{Path(media).suffix}"
            (destino.parent / "img").mkdir(parents=True, exist_ok=True)
            (destino.parent / "img" / archivo).write_bytes(datos)
            figuras.append(f"![{ws.title}](img/{archivo})\n")
        partes.append(f"## {ws.title}\n\n" + "\n".join(figuras) + cuerpo)
    escribir(destino, "\n".join(partes))


# --------------------------------------------------------------------------- historias de usuario


def exportar_historias(origen: Path) -> None:
    relativo = f"{ESTRATEGICOS}/1.1.7. Tallaje producto/Tallaje producto - CanchaYa.xlsx"
    wb = libro(origen, relativo)
    carpeta = DOCS / "historias"

    backlog = {}
    filas = list(wb["Product Backlog HUs"].iter_rows(values_only=True))
    for f in filas[1:]:
        if f[0] and re.fullmatch(r"HU\d+", str(f[0])):
            backlog[str(f[0])] = f

    indice = [
        "# Historias de usuario\n",
        AVISO.format(origen=relativo),
        "Backlog oficial del Tallaje del producto. Cada HU tiene su archivo con la tarjeta, "
        "la conversación y los criterios de aceptación.\n",
    ]
    filas_indice = []
    for hu, f in sorted(backlog.items(), key=lambda kv: int(kv[0][2:])):
        filas_indice.append([f"[{hu}]({hu}.md)", f[2], f[8], f[9], f[12], f[13]])
    indice.append(tabla(["HU", "Nombre", "Talla", "Puntos", "Prioridad", "Release"], filas_indice))
    escribir(carpeta / "README.md", "\n".join(indice))

    for ws in wb.worksheets:
        if not re.fullmatch(r"HU\d+", ws.title):
            continue
        escribir(carpeta / f"{ws.title}.md", historia(ws, backlog.get(ws.title), relativo))


def historia(ws, fila_backlog, relativo: str) -> str:
    titulo = ws["A3"].value or ws.title
    partes = [f"# {titulo}\n", AVISO.format(origen=f"{relativo}, hoja {ws.title}")]
    if fila_backlog:
        partes.append(
            f"**Release:** {fila_backlog[13]} · **Prioridad:** {fila_backlog[12]} · "
            f"**Talla:** {fila_backlog[8]} · **Puntos:** {fila_backlog[9]}\n"
        )
    seccion = None
    criterios = []
    for fila in filas_con_datos(ws):
        a = texto(fila[0].value)
        resto = [texto(c.value) for c in fila[1:] if texto(c.value)]
        if a in ("<-Volver al listado", "Título", titulo):
            continue
        if a == "Tarjeta":
            partes.append("## Tarjeta\n")
        elif a in ("Como", "Necesito", "Para"):
            partes.append(f"- **{a}:** {' '.join(resto)}")
        elif a.startswith("Como ") and not resto:
            partes.append(f"\n> {a}\n")
        elif a == "Tallaje":
            seccion = "tallaje"
        elif a in ("Talla", "Puntos de historia de usuario", "¿Definition of Ready?",
                   "¿Definition of Done?"):
            continue
        elif a in ("Conversación historia", "Conversación aspectos generales y transversales"):
            partes.append(f"\n## {a}\n")
        elif a == "Criterios aceptación":
            seccion = "criterios"
            partes.append("\n## Criterios de aceptación\n")
        elif seccion == "criterios":
            if re.fullmatch(r"CA\d+", a):
                criterios.append((a, resto[0] if resto else ""))
        elif a.startswith("*"):
            partes.append(f"- {a.lstrip('* ').strip()}")
        elif resto:
            partes.append(f"- **{a.rstrip(':')}:** {' '.join(resto)}")
        else:
            partes.append(f"- {a}")
    partes.append(tabla(["Código", "Escenario"], [list(c) for c in criterios]))
    return "\n".join(partes)


# --------------------------------------------------------------------------- modelo de dominio


def exportar_dominio(origen: Path) -> None:
    relativo = (f"{ESTRATEGICOS}/1.1.4. Modelo de dominio/1.1.3.2. Modelo de dominio enriquecido/"
                "Modelo_de_dominio_enriquecido_CanchaYa.xlsx")
    wb = libro(origen, relativo)
    carpeta = DOCS / "dominio"
    lista = wb["Listado Objetos de Dominio"]
    objetos = [list(f) for f in lista.iter_rows(min_row=2, values_only=True) if f[0]]
    indice = [
        "# Modelo de dominio enriquecido\n",
        AVISO.format(origen=relativo),
        "Referencia oficial del dominio: atributos, combinaciones únicas, responsabilidades y "
        "políticas (reglas de negocio) de cada objeto. Toda entidad lleva `id` UUID generado "
        "en el servidor y los atributos van en camelCase.\n",
        tabla(["Objeto", "Descripción"],
              [[f"[{o[0]}]({nombre_archivo(o[0])}.md)", o[1]] for o in objetos]),
    ]
    escribir(carpeta / "README.md", "\n".join(indice))
    for o in objetos:
        if o[0] in wb.sheetnames:
            escribir(carpeta / f"{nombre_archivo(o[0])}.md",
                     objeto_dominio(wb[o[0]], f"{relativo}, hoja {o[0]}"))


def objeto_dominio(ws, relativo: str) -> str:
    filas = [[c.value for c in f] for f in ws.iter_rows()]
    nombre, descripcion = filas[1][1], filas[2][1]
    partes = [f"# {nombre}\n", AVISO.format(origen=relativo), f"{texto(descripcion)}\n"]

    def buscar(etiqueta, desde=0):
        return next((i for i in range(desde, len(filas)) if texto(filas[i][0]) == etiqueta), None)

    # Atributos
    i_attr = buscar("Atributo")
    if i_attr is not None:
        enc = filas[i_attr]
        i_desc = next(j for j, v in enumerate(enc) if texto(v) == "Descripción")
        datos = []
        j = i_attr + 1
        while j < len(filas) and texto(filas[j][0]):
            datos.append(filas[j])
            j += 1
        partes.append("## Atributos\n")
        partes.append(tabla(enc[: i_desc + 1], [d[: i_desc + 1] for d in datos]))
        usos = [k for k in range(i_desc + 1, len(enc)) if texto(enc[k])]
        if usos:
            partes.append("\n### Uso de cada atributo por responsabilidad\n")
            partes.append(tabla([enc[0]] + [enc[k] for k in usos],
                                [[d[0]] + [d[k] for k in usos] for d in datos]))

    # Combinaciones únicas
    i_comb = buscar("Nombre combinación")
    if i_comb is not None:
        datos = []
        j = i_comb + 1
        while j < len(filas) and texto(filas[j][0]):
            datos.append(filas[j][:3])
            j += 1
        if datos:
            partes.append("\n## Combinaciones únicas\n")
            partes.append(tabla(filas[i_comb][:3], datos))

    # Responsabilidades
    i_resp = buscar("Responsabilidad")
    if i_resp is not None:
        enc, sub = filas[i_resp], filas[i_resp + 1]
        col = {texto(v): k for k, v in enumerate(enc) if texto(v)}
        c_desc = col.get("Descripción")
        c_ent = col.get("Entradas")
        c_sal = col.get("Salida")
        c_pol = col.get("Políticas (Reglas de Negocio)")
        c_exc = col.get("Excepción")
        partes.append("\n## Responsabilidades\n")
        actual = None
        for f in filas[i_resp + 2 :]:
            if not any(texto(v) for v in f):
                continue
            if texto(f[0]):
                if actual:
                    partes.append(responsabilidad(actual))
                actual = {"nombre": texto(f[0]), "desc": texto(f[c_desc]) if c_desc else "",
                          "entradas": [], "salida": "", "politicas": []}
            if actual is None:
                continue
            if c_ent is not None and texto(f[c_ent]):
                actual["entradas"].append(
                    f"`{texto(f[c_ent])}` ({texto(f[c_ent + 1])}): {texto(f[c_ent + 2])}")
            if c_sal is not None and texto(f[c_sal]):
                actual["salida"] = f"{texto(f[c_sal])}: {texto(f[c_sal + 1])}"
            if c_pol is not None and texto(f[c_pol]):
                actual["politicas"].append([f[c_pol], f[c_pol + 1],
                                            f[c_exc] if c_exc else "",
                                            f[c_exc + 1] if c_exc else ""])
        if actual:
            partes.append(responsabilidad(actual))
    return "\n".join(partes)


def responsabilidad(r: dict) -> str:
    partes = [f"\n### {r['nombre']}\n", f"{r['desc']}\n"]
    if r["entradas"]:
        partes.append("**Entradas:**\n\n" + "\n".join(f"- {e}" for e in r["entradas"]) + "\n")
    if r["salida"]:
        partes.append(f"**Salida:** {r['salida']}\n")
    if r["politicas"]:
        partes.append(tabla(["Política", "Descripción", "Excepción", "¿Qué hago?"],
                            r["politicas"]))
    return "\n".join(partes)


# --------------------------------------------------------------------------- principal


def main() -> None:
    origen = Path(sys.argv[1]) if len(sys.argv) > 1 else RAIZ_REPO.parent / "Documentos"
    if not (origen / "decisiones.md").exists():
        sys.exit(f"No encuentro decisiones.md en {origen}. Indica la carpeta de documentos.")

    # Se borra lo generado antes para que no queden archivos viejos. docs/README.md se conserva.
    DOCS.mkdir(exist_ok=True)
    for carpeta in ("historias", "dominio", "especificacion", "arquitectura", "diseno"):
        shutil.rmtree(DOCS / carpeta, ignore_errors=True)
    shutil.copyfile(origen / "decisiones.md", DOCS / "decisiones.md")

    exportar_historias(origen)
    exportar_dominio(origen)

    exportar_libro(origen, f"{APOYO}/Catálogo de mensajes - CanchaYa.xlsx",
                   DOCS / "especificacion" / "mensajes.md", "Catálogo de mensajes",
                   tabulares=("Mensajes", "Plantillas de correo"))
    exportar_libro(origen, f"{APOYO}/Catálogo de parámetros - CanchaYa.xlsx",
                   DOCS / "especificacion" / "parametros.md", "Catálogo de parámetros",
                   tabulares=("Parámetros",))
    exportar_libro(origen, f"{APOYO}/Aspectos generales - CanchaYa.xlsx",
                   DOCS / "especificacion" / "aspectos-generales.md", "Aspectos generales",
                   tabulares=("Aspectos generales",))

    arq = f"{TECNICOS}/1. Diseño arquitectónico"
    drivers = f"{arq}/1. Drivers arquitectónicos"
    for relativo, archivo, titulo in [
        (f"{drivers}/1. Atributos de calidad/2. Escenarios de calidad/"
         "Escenarios de calidad - CanchaYa.xlsx", "escenarios-calidad.md", "Escenarios de calidad"),
        (f"{drivers}/3. Restricciones técnicas/Restricciones técnicas - CanchaYa.xlsx",
         "restricciones-tecnicas.md", "Restricciones técnicas"),
        (f"{drivers}/4. Restricciones de negocio/Restricciones de negocio - CanchaYa.xlsx",
         "restricciones-negocio.md", "Restricciones de negocio"),
        (f"{arq}/3. Tácticas y estrategias/Tácticas y estrategias - CanchaYa.xlsx",
         "tacticas.md", "Tácticas y estrategias"),
        (f"{arq}/4. Alternativas de solución/3. Alternativa seleccionada/"
         "Alternativa seleccionada - CanchaYa.xlsx",
         "alternativa-seleccionada.md", "Alternativa seleccionada (ALT-01)"),
        (f"{arq}/5. Arquetipo de solución/Arquetipo de solución - CanchaYa.xlsx",
         "arquetipo.md", "Arquetipo de solución"),
        (f"{arq}/6. Plataforma tecnológica/Plataforma tecnológica - CanchaYa.xlsx",
         "plataforma.md", "Plataforma tecnológica"),
    ]:
        exportar_libro(origen, relativo, DOCS / "arquitectura" / archivo, titulo,
                       omitir=("Valores",))

    det = f"{TECNICOS}/2. Diseño detallado"
    exportar_libro(origen, f"{det}/1. Diagrama de clases/Diagrama de clases - CanchaYa.xlsx",
                   DOCS / "diseno" / "clases.md", "Diagrama de clases",
                   omitir=("Valores",), tabulares=())
    exportar_libro(origen, f"{det}/2. Diagrama de componentes/Diagrama de componentes - CanchaYa.xlsx",
                   DOCS / "diseno" / "componentes.md", "Diagrama de componentes")
    exportar_libro(origen, f"{det}/6. Modelo entidad relación/Modelo entidad relación - CanchaYa.xlsx",
                   DOCS / "diseno" / "entidad-relacion.md", "Modelo entidad relación",
                   omitir=("Valores",))
    # Diagramas en texto (Mermaid): la IA los lee mejor que la imagen.
    for fuente, destino in [
        (f"{det}/1. Diagrama de clases/Diagrama de clases - CanchaYa.mmd", "clases.mmd"),
        (f"{det}/6. Modelo entidad relación/Modelo entidad relación - CanchaYa.mmd", "entidad-relacion.mmd"),
    ]:
        if (origen / fuente).exists():
            shutil.copyfile(origen / fuente, DOCS / "diseno" / destino)

    # Diagrama del modelo de dominio: está como imagen suelta, no dentro del Excel.
    imagen = f"{ESTRATEGICOS}/1.1.4. Modelo de dominio/Modelo de dominio Ampliado.drawio.png"
    if (origen / imagen).exists():
        (DOCS / "dominio" / "img").mkdir(exist_ok=True)
        shutil.copyfile(origen / imagen, DOCS / "dominio" / "img" / "modelo-de-dominio.png")
        indice = DOCS / "dominio" / "README.md"
        texto_indice = indice.read_text(encoding="utf-8")
        indice.write_text(texto_indice.replace(
            "\n| Objeto |",
            f"\n![Modelo de dominio](img/modelo-de-dominio.png)\n\n_Imagen: `{imagen}`._\n\n| Objeto |", 1),
            encoding="utf-8", newline="\n")

    print(f"Documentos exportados en {DOCS}")


if __name__ == "__main__":
    main()
