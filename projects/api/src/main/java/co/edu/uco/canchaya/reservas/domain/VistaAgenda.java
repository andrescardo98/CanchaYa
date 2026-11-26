package co.edu.uco.canchaya.reservas.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/** HU29: los días que trae la agenda del escenario. */
public enum VistaAgenda {

	// CA1: solo la fecha elegida
	DIA {
		@Override
		public LocalDate desde(LocalDate fecha) {
			return fecha;
		}

		@Override
		public LocalDate hasta(LocalDate fecha) {
			return fecha;
		}
	},

	// CA2 y P5: de lunes a domingo de la semana de la fecha
	SEMANA {
		@Override
		public LocalDate desde(LocalDate fecha) {
			return fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		}

		@Override
		public LocalDate hasta(LocalDate fecha) {
			return fecha.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
		}
	};

	/** Primer día de la agenda, incluido. */
	public abstract LocalDate desde(LocalDate fecha);

	/** Último día de la agenda, incluido. */
	public abstract LocalDate hasta(LocalDate fecha);

}
