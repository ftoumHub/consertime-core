package fr.conserto.consertime.bdd;

import fr.conserto.consertime.domaine.affaires.Affectation;
import fr.conserto.consertime.domaine.affaires.Client;
import fr.conserto.consertime.domaine.affaires.Projet;
import fr.conserto.consertime.domaine.affaires.TypeAbsence;
import fr.conserto.consertime.domaine.calendrier.CalendrierOuvre;
import fr.conserto.consertime.domaine.calendrier.Periode;
import fr.conserto.consertime.domaine.documents.CompteRenduActivite;
import fr.conserto.consertime.domaine.documents.FicheDeTemps;
import fr.conserto.consertime.domaine.personnel.Collaborateur;
import fr.conserto.consertime.domaine.regle.CodeRegle;
import fr.conserto.consertime.domaine.regle.RegleMetierViolee;
import fr.conserto.consertime.domaine.temps.ControleCompletude;
import fr.conserto.consertime.domaine.temps.FeuilleDeTemps;
import fr.conserto.consertime.domaine.temps.Jours;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** Étapes de saisie_et_documents.feature. */
public class EtapesSaisieEtDocuments {

    private static final Client ACME = new Client("ACME", "ACME Assurances");
    private static final Map<String, Projet> PROJETS = Stream.of(
                    Projet.mission("ACME-EXT", "Refonte extranet", ACME),
                    Projet.mission("BQO-CORE", "Migration core banking", new Client("BQO", "Banque Ouest")),
                    Projet.interne("INTER", "Intercontrat"),
                    Projet.interne("ECOLE", "École"),
                    TypeAbsence.CONGES_PAYES.projet(),
                    TypeAbsence.RTT.projet())
            .collect(Collectors.toMap(Projet::nom, p -> p));

    private final CalendrierOuvre calendrier = CalendrierOuvre.france();
    private final Clock horloge = Clock.fixed(LocalDate.of(2025, 5, 2).atTime(9, 0).atZone(ZoneId.of("Europe/Paris")).toInstant(),
            ZoneId.of("Europe/Paris"));
    private Collaborateur collaborateur;
    private FeuilleDeTemps feuille;
    private CompteRenduActivite cra;
    private FicheDeTemps fdt;
    private RegleMetierViolee erreur;

    @Given("la feuille de temps d'Alice Martin pour le mois {word}")
    public void feuilleDAlice(String mois) {
        collaborateur = Collaborateur.builder().matricule(1002).nom("Alice", "Martin").entreLe(LocalDate.of(2020, 1, 6))
                .departement("DIG").build();
        feuille = new FeuilleDeTemps(collaborateur, YearMonth.parse(mois), calendrier);
    }

    @Given("Alice a saisi les périodes suivantes :")
    public void periodesSaisies(DataTable table) {
        for (Map<String, String> ligne : table.asMaps()) {
            Affectation affectation = Affectation.simple(collaborateur.matricule(), projet(ligne.get("projet")));
            Periode periode = Periode.du(LocalDate.parse(ligne.get("du")), LocalDate.parse(ligne.get("au")));
            calendrier.joursOuvres(periode).forEach(jour -> feuille.saisir(affectation, jour, Jours.of(ligne.get("jours"))));
        }
    }

    @When("Alice saisit {word} jour sur {string} le {word}")
    public void saisit(String jours, String projet, String date) {
        try {
            feuille.saisir(Affectation.simple(collaborateur.matricule(), projet(projet)), LocalDate.parse(date), Jours.of(jours));
        } catch (RegleMetierViolee e) {
            erreur = e;
        }
    }

    @Then("la feuille est complète avec {int} jours ouvrés attendus")
    public void complete(int attendus) {
        ControleCompletude.Resultat resultat = ControleCompletude.verifier(feuille);
        assertThat(resultat.estComplet()).isTrue();
        assertThat(resultat.joursOuvresAttendus()).isEqualTo(attendus);
    }

    @When("on établit le compte rendu d'activité")
    public void etablirCra() {
        try {
            cra = CompteRenduActivite.etablir(feuille, true, horloge);
        } catch (RegleMetierViolee e) {
            erreur = e;
        }
    }

    @When("on établit la feuille de temps mensuelle")
    public void etablirFdt() {
        fdt = FicheDeTemps.etablir(feuille, List.of(), true, horloge);
    }

    @Then("le CRA compte {int} jours facturés pour le client {string}")
    public void craJoursFactures(int jours, String client) {
        assertThat(cra.joursFactures()).isEqualTo(Jours.of(String.valueOf(jours)));
        assertThat(cra.client().map(Client::nom)).contains(client);
    }

    @Then("la ligne du {word} est de type {string}")
    public void typeDeLigne(String date, String statut) {
        assertThat(ligne(date).statut().name()).isEqualTo(statut);
    }

    @Then("la ligne du {word} indique {string}")
    public void libelleDeLigne(String date, String libelle) {
        assertThat(ligne(date).libelleImputations()).isEqualTo(libelle);
    }

    @Then("la FDT contient les totaux suivants :")
    public void totauxFdt(DataTable table) {
        List<String> attendus = table.asMaps().stream().map(l -> l.get("projet") + " = " + l.get("total")).collect(Collectors.toList());
        List<String> obtenus = fdt.lignes().stream().map(l -> l.projet().nom() + " = " + l.total()).collect(Collectors.toList());
        assertThat(obtenus).isEqualTo(attendus);
    }

    @Then("le total du mois est de {word} jours")
    public void totalDuMois(String total) {
        assertThat(fdt.totalMois()).isEqualTo(Jours.of(total));
    }

    @Then("l'export est refusé avec {int} jours ouvrés attendus et {int} jours saisis")
    public void exportRefuse(int attendus, int saisis) {
        assertThat(erreur).as("export refusé").isNotNull();
        assertThat(erreur.code()).isEqualTo(CodeRegle.EXPORT_SAISIE_INCOMPLETE);
        ControleCompletude.Resultat resultat = ControleCompletude.verifier(feuille);
        assertThat(resultat.joursOuvresAttendus()).isEqualTo(attendus);
        assertThat(resultat.joursComplets()).isEqualTo(saisis);
    }

    @Then("la saisie est rejetée pour la règle {string}")
    public void saisieRejetee(String code) {
        assertThat(erreur).as("saisie rejetée").isNotNull();
        assertThat(erreur.code().name()).isEqualTo(code);
    }

    @Then("la saisie est acceptée")
    public void saisieAcceptee() {
        assertThat(erreur).as("aucune règle enfreinte").isNull();
    }

    private CompteRenduActivite.Ligne ligne(String date) {
        LocalDate jour = LocalDate.parse(date);
        return cra.lignes().stream().filter(l -> l.date().equals(jour)).findFirst().orElseThrow();
    }

    private static Projet projet(String nom) {
        Projet p = PROJETS.get(nom);
        if (p == null) {
            throw new IllegalArgumentException("Projet inconnu : " + nom);
        }
        return p;
    }
}
