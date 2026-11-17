package fr.conserto.consertime.bdd;

import fr.conserto.consertime.domaine.affaires.TypeAbsence;
import fr.conserto.consertime.domaine.calendrier.CalendrierOuvre;
import fr.conserto.consertime.domaine.calendrier.DemiJournee;
import fr.conserto.consertime.domaine.conges.DemandeConge;
import fr.conserto.consertime.domaine.conges.Notifications;
import fr.conserto.consertime.domaine.conges.PeriodeConge;
import fr.conserto.consertime.domaine.personnel.Collaborateur;
import fr.conserto.consertime.domaine.personnel.Departement;
import fr.conserto.consertime.domaine.personnel.Role;
import fr.conserto.consertime.domaine.regle.RegleMetierViolee;
import fr.conserto.consertime.domaine.temps.Jours;
import fr.conserto.consertime.domaine.temps.Saisie;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** Étapes de conges.feature. Une instance par scénario. */
public class EtapesConges {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    private final CalendrierOuvre calendrier = CalendrierOuvre.france();
    private final Map<String, Collaborateur> collaborateurs = new LinkedHashMap<>();
    private final Map<String, Departement> departements = new LinkedHashMap<>();
    private final List<DemandeConge> demandes = new ArrayList<>();
    private Clock horloge = Clock.system(PARIS);
    private DemandeConge derniere;
    private List<Saisie> saisiesProduites = List.of();
    private RuntimeException erreur;

    @Given("les collaborateurs suivants :")
    public void lesCollaborateursSuivants(DataTable table) {
        for (Map<String, String> ligne : table.asMaps()) {
            Collaborateur.Builder builder = Collaborateur.builder()
                    .matricule(Integer.parseInt(ligne.get("matricule")))
                    .nom(ligne.get("prénom"), ligne.get("nom"))
                    .departement(ligne.get("département"))
                    .entreLe(LocalDate.parse(ligne.get("entrée")));
            Arrays.stream(ligne.get("rôles").split(",")).map(String::trim).map(Role::valueOf).forEach(builder::roles);
            collaborateurs.put(ligne.get("prénom"), builder.build());
        }
    }

    @Given("le département {string} a pour référente {word}")
    public void departementAvecReferent(String code, String prenom) {
        departements.put(code, new Departement(code, code, Set.of(collaborateur(prenom).matricule())));
    }

    @Given("le département {string} n'a pas de référent")
    public void departementSansReferent(String code) {
        departements.put(code, new Departement(code, code, new HashSet<>()));
    }

    @Given("nous sommes le {word} à {word}")
    public void nousSommesLe(String date, String heure) {
        horloge = Clock.fixed(LocalDateTime.parse(date + "T" + heure).atZone(PARIS).toInstant(), PARIS);
    }

    @Given("{word} a demandé un congé {string} du {word} {word} au {word} {word}")
    public void aDemande(String prenom, String type, String debut, String depuis, String fin, String jusqua) {
        derniere = deposer(prenom, type, debut, depuis, fin, jusqua);
    }

    @When("{word} demande un congé {string} du {word} {word} au {word} {word}")
    public void demande(String prenom, String type, String debut, String depuis, String fin, String jusqua) {
        try {
            derniere = deposer(prenom, type, debut, depuis, fin, jusqua);
        } catch (RegleMetierViolee e) {
            erreur = e;
        }
    }

    @Given("{word} a validé la demande")
    public void aValide(String prenom) {
        saisiesProduites = derniere.valider(collaborateur(prenom), departementDe(derniere), calendrier, horloge);
    }

    @When("{word} valide la demande")
    public void valide(String prenom) {
        tenter(() -> saisiesProduites = derniere.valider(collaborateur(prenom), departementDe(derniere), calendrier, horloge));
    }

    @When("{word} refuse la demande")
    public void refuse(String prenom) {
        tenter(() -> derniere.refuser(collaborateur(prenom), departementDe(derniere), "refus", horloge));
    }

    @When("{word} annule la demande")
    public void annule(String prenom) {
        tenter(() -> saisiesProduites = derniere.annuler(collaborateur(prenom), calendrier, horloge));
    }

    @Then("la demande compte {word} jours")
    public void laDemandeCompte(String jours) {
        assertThat(erreur).as("aucune règle enfreinte").isNull();
        assertThat(derniere.nombreDeJours()).isEqualTo(Jours.of(jours));
    }

    @Then("la demande est {string}")
    public void laDemandeEst(String etat) {
        assertThat(derniere.etat().libelle()).isEqualTo(etat);
    }

    @Then("l'opération est rejetée pour la règle {string}")
    public void rejetee(String code) {
        assertThat(erreur).as("une règle devait être enfreinte").isNotNull();
        assertThat(((RegleMetierViolee) erreur).code().name()).isEqualTo(code);
    }

    @Then("les saisies d'absence suivantes sont produites :")
    public void saisiesProduites(DataTable table) {
        List<String> attendues = table.asMaps().stream()
                .map(l -> l.get("date") + " " + Jours.of(l.get("jours")) + " " + l.get("projet"))
                .collect(Collectors.toList());
        List<String> obtenues = saisiesProduites.stream()
                .map(s -> s.date() + " " + s.jours() + " " + s.projet().code())
                .collect(Collectors.toList());
        assertThat(obtenues).isEqualTo(attendues);
    }

    @Then("le nombre de saisies à retirer est {int}")
    public void nombreDeSaisiesARetirer(int nombre) {
        assertThat(saisiesProduites).hasSize(nombre);
    }

    @Then("les destinataires de la demande sont {string}")
    public void destinataires(String prenoms) {
        Notifications.Destinataires dest = Notifications.nouvelleDemande(derniere, departementDe(derniere), collaborateurs.values());
        assertThat(dest.destinataires().stream().map(Collaborateur::prenom).collect(Collectors.joining(", "))).isEqualTo(prenoms);
    }

    @Then("la configuration est signalée incomplète")
    public void configurationIncomplete() {
        assertThat(Notifications.nouvelleDemande(derniere, departementDe(derniere), collaborateurs.values())
                .configurationIncomplete()).isTrue();
    }

    @Then("la demande est à relancer après {int} jours ouvrés")
    public void aRelancer(int delai) {
        assertThat(Notifications.aRelancer(demandes, delai, calendrier, horloge)).containsExactly(derniere);
    }

    @Then("la demande n'est pas à relancer après {int} jours ouvrés")
    public void pasARelancer(int delai) {
        assertThat(Notifications.aRelancer(demandes, delai, calendrier, horloge)).isEmpty();
    }

    // ------------------------------------------------------------------ outillage

    private DemandeConge deposer(String prenom, String type, String debut, String depuis, String fin, String jusqua) {
        PeriodeConge periode = new PeriodeConge(LocalDate.parse(debut), borneDebut(depuis), LocalDate.parse(fin), borneFin(jusqua));
        TypeAbsence absence = TypeAbsence.depuisCodeProjet(type).orElseThrow();
        DemandeConge demande = DemandeConge.deposer(collaborateur(prenom), absence, periode, "", demandes, calendrier, horloge);
        demandes.add(demande);
        return demande;
    }

    private void tenter(Runnable action) {
        try {
            action.run();
        } catch (RegleMetierViolee e) {
            erreur = e;
        }
    }

    private static DemiJournee borneDebut(String depuis) {
        switch (depuis) {
            case "matin":
                return DemiJournee.MATIN;
            case "après-midi":
                return DemiJournee.APRES_MIDI;
            default:
                throw new IllegalArgumentException("début attendu « matin » ou « après-midi » : " + depuis);
        }
    }

    private static DemiJournee borneFin(String jusqua) {
        switch (jusqua) {
            case "midi":
                return DemiJournee.MATIN;
            case "soir":
                return DemiJournee.APRES_MIDI;
            default:
                throw new IllegalArgumentException("fin attendue « midi » ou « soir » : " + jusqua);
        }
    }

    private Collaborateur collaborateur(String prenom) {
        Collaborateur c = collaborateurs.get(prenom);
        if (c == null) {
            throw new IllegalArgumentException("Collaborateur inconnu : " + prenom);
        }
        return c;
    }

    private Departement departementDe(DemandeConge demande) {
        return departements.get(demande.demandeur().codeDepartement());
    }
}
