package co.edu.uco.canchaya;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import java.util.ArrayList;
import java.util.List;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Reglas de arquitectura que se revisan en cada compilación (arquetipo de solución,
 * hoja «Estructura del backend»). Si alguien —una persona o la IA— rompe un límite
 * entre módulos, esta prueba falla y el cambio no pasa.
 *
 * <p>Las reglas se escriben desde ahora, aunque los módulos todavía no tengan clases
 * ({@code allowEmptyShould(true)}): así protegen el código desde la primera HU.
 */
@AnalyzeClasses(packages = "co.edu.uco.canchaya", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {

	private static final String RAIZ = "co.edu.uco.canchaya.";

	// --- El dominio no conoce la tecnología ---------------------------------------

	@ArchTest
	static final ArchRule elDominioNoDependeDeSpringNiDeLaBaseDeDatos = noClasses()
			.that().resideInAPackage("..domain..")
			.should().dependOnClassesThat().resideInAnyPackage(
					"org.springframework..", "jakarta.persistence..", "org.hibernate..", "java.sql..")
			.because("el dominio solo tiene reglas de negocio; la tecnología va en los adaptadores")
			.allowEmptyShould(true);

	// --- Qué módulo puede usar a cuál («Puede usar a») ----------------------------

	@ArchTest
	static final ArchRule identidadNoUsaOtrosModulos =
			soloPuedeUsar("identidad");

	@ArchTest
	static final ArchRule ubicacionNoUsaOtrosModulos =
			soloPuedeUsar("ubicacion");

	@ArchTest
	static final ArchRule escenariosSoloUsaIdentidadYUbicacion =
			soloPuedeUsar("escenarios", "identidad", "ubicacion");

	@ArchTest
	static final ArchRule reservasSoloUsaIdentidadYEscenarios =
			soloPuedeUsar("reservas", "identidad", "escenarios");

	@ArchTest
	static final ArchRule reputacionSoloUsaIdentidadReservasYEscenarios =
			soloPuedeUsar("reputacion", "identidad", "reservas", "escenarios");

	@ArchTest
	static final ArchRule sinDependenciasEnCirculo = slices()
			.matching(RAIZ + "(*)..")
			.should().beFreeOfCycles()
			.allowEmptyShould(true);

	/**
	 * Las clases del módulo {@code modulo} no pueden usar ningún otro módulo de negocio,
	 * salvo los permitidos. Transversal lo pueden usar todos.
	 */
	private static ArchRule soloPuedeUsar(String modulo, String... permitidos) {
		var todos = new ArrayList<>(
				List.of("identidad", "ubicacion", "escenarios", "reservas", "reputacion"));
		todos.remove(modulo);
		todos.removeAll(List.of(permitidos));
		String[] prohibidos = todos.stream().map(m -> RAIZ + m + "..").toArray(String[]::new);
		return noClasses()
				.that().resideInAPackage(RAIZ + modulo + "..")
				.should().dependOnClassesThat().resideInAnyPackage(prohibidos)
				.because("el arquetipo solo permite que " + modulo + " use a "
						+ (permitidos.length == 0 ? "ningún otro módulo" : String.join(", ", permitidos)))
				.allowEmptyShould(true);
	}

}
