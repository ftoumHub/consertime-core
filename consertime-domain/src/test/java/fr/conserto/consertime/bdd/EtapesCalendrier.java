package fr.conserto.consertime.bdd;

import fr.conserto.consertime.domaine.calendrier.CalendrierFrance;
import fr.conserto.consertime.domaine.calendrier.CalendrierOuvre;
import fr.conserto.consertime.domaine.calendrier.JourFerie;
import fr.conserto.consertime.domaine.calendrier.Periode;
import io.cucumber.java.en.Then;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

/** Étapes de calendrier.feature. */
public class EtapesCalendrier {

    private final CalendrierFrance metropole = CalendrierFrance.metropole();
    private final CalendrierOuvre ouvre = new CalendrierOuvre(metropole);

    @Then("Pâques {int} tombe le {word}")
    public void paquesTombeLe(int annee, String date) {
        assertThat(CalendrierFrance.dimancheDePaques(annee)).isEqualTo(LocalDate.parse(date));
    }

    @Then("le {word} est un jour férié {string}")
    public void estUnJourFerie(String date, String libelle) {
        assertThat(metropole.jourFerie(LocalDate.parse(date)).map(JourFerie::libelle)).contains(libelle);
    }

    @Then("l'année {int} compte {int} jours fériés en métropole")
    public void feriesMetropole(int annee, int nombre) {
        assertThat(metropole.joursFeries(annee)).hasSize(nombre);
    }

    @Then("l'année {int} compte {int} jours fériés en Alsace-Moselle")
    public void feriesAlsaceMoselle(int annee, int nombre) {
        assertThat(CalendrierFrance.alsaceMoselle().joursFeries(annee)).hasSize(nombre);
    }

    @Then("le mois {word} compte {int} jours ouvrés et {int} jours ouvrables")
    public void joursDuMois(String mois, int ouvres, int ouvrables) {
        Periode periode = Periode.mois(YearMonth.parse(mois));
        assertThat(ouvre.nombreJoursOuvres(periode)).isEqualTo(ouvres);
        assertThat(ouvre.nombreJoursOuvrables(periode)).isEqualTo(ouvrables);
    }
}
