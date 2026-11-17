package fr.conserto.consertime.bdd;

import fr.conserto.consertime.domaine.calendrier.CalendrierOuvre;
import fr.conserto.consertime.domaine.droits.CalculDroitsCongesPayes;
import fr.conserto.consertime.domaine.droits.DroitsCongesPayes;
import fr.conserto.consertime.domaine.droits.ModeDecompte;
import fr.conserto.consertime.domaine.droits.PeriodeReference;
import fr.conserto.consertime.domaine.droits.PolitiqueCongesPayes;
import fr.conserto.consertime.domaine.personnel.Collaborateur;
import fr.conserto.consertime.domaine.personnel.TypeContrat;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/** Étapes de droits_conges_payes.feature. */
public class EtapesDroitsConges {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    private Collaborateur collaborateur;
    private PolitiqueCongesPayes politique;
    private Clock horloge = Clock.system(PARIS);
    private DroitsCongesPayes droits;

    @Given("un collaborateur embauché le {word}")
    public void collaborateurEmbauche(String date) {
        collaborateur = Collaborateur.builder().matricule(5000).nom("Camille", "Test").entreLe(LocalDate.parse(date)).build();
    }

    @Given("un stagiaire embauché le {word}")
    public void stagiaireEmbauche(String date) {
        collaborateur = Collaborateur.builder().matricule(5001).nom("Sam", "Stage").entreLe(LocalDate.parse(date))
                .contrat(TypeContrat.STAGIAIRE).build();
    }

    @Given("un décompte en jours {string}")
    public void decompte(String mode) {
        politique = PolitiqueCongesPayes.codeDuTravail(mode(mode));
    }

    @Given("un décompte en jours {string} avec les congés d'ancienneté Syntec")
    public void decompteSyntec(String mode) {
        politique = PolitiqueCongesPayes.syntec(mode(mode));
    }

    @Given("nous sommes le {word}")
    public void nousSommesLe(String date) {
        horloge = Clock.fixed(LocalDate.parse(date).atTime(12, 0).atZone(PARIS).toInstant(), PARIS);
    }

    @When("on calcule ses droits au {word}")
    public void calculAu(String date) {
        LocalDate le = LocalDate.parse(date);
        PeriodeReference reference = PeriodeReference.contenant(le.minusDays(1), politique.debutPeriodeReference());
        droits = calcul().calculer(collaborateur, reference, le);
    }

    @When("on calcule ses droits à la date du jour")
    public void calculALaDateDuJour() {
        droits = calcul().enCours(collaborateur, horloge);
    }

    @Then("il a acquis {word} jours avant arrondi")
    public void acquisBruts(String jours) {
        assertThat(droits.acquisBruts()).isEqualByComparingTo(jours);
    }

    @Then("ses droits retenus sont de {int} jours")
    public void droitsRetenus(int jours) {
        assertThat(droits.acquis()).isEqualByComparingTo(String.valueOf(jours));
    }

    @Then("ses droits totaux sont de {int} jours")
    public void droitsTotaux(int jours) {
        assertThat(droits.total()).isEqualByComparingTo(String.valueOf(jours));
    }

    @Then("la période de référence n'est pas close")
    public void periodeNonClose() {
        assertThat(droits.periodeClose()).isFalse();
    }

    private CalculDroitsCongesPayes calcul() {
        return new CalculDroitsCongesPayes(politique, CalendrierOuvre.france());
    }

    private static ModeDecompte mode(String libelle) {
        switch (libelle) {
            case "ouvrables":
                return ModeDecompte.OUVRABLES;
            case "ouvrés":
                return ModeDecompte.OUVRES;
            default:
                throw new IllegalArgumentException("mode « ouvrables » ou « ouvrés » attendu : " + libelle);
        }
    }
}
