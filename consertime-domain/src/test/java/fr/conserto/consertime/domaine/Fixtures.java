package fr.conserto.consertime.domaine;

import fr.conserto.consertime.domaine.affaires.Affectation;
import fr.conserto.consertime.domaine.affaires.Budget;
import fr.conserto.consertime.domaine.affaires.Client;
import fr.conserto.consertime.domaine.affaires.Projet;
import fr.conserto.consertime.domaine.calendrier.CalendrierOuvre;
import fr.conserto.consertime.domaine.calendrier.Periode;
import fr.conserto.consertime.domaine.personnel.Collaborateur;
import fr.conserto.consertime.domaine.personnel.Departement;
import fr.conserto.consertime.domaine.personnel.Matricule;
import fr.conserto.consertime.domaine.personnel.Role;
import fr.conserto.consertime.domaine.personnel.TypeContrat;
import fr.conserto.consertime.domaine.regle.CodeRegle;
import fr.conserto.consertime.domaine.regle.RegleMetierViolee;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Jeu de données partagé par les tests : mêmes personnes et projets que le golden master du legacy. */
public final class Fixtures {

    public static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    public static final CalendrierOuvre CALENDRIER = CalendrierOuvre.france();

    public static final Client ACME = new Client("ACME", "ACME Assurances");
    public static final Client BANQUE_OUEST = new Client("BQO", "Banque Ouest");
    public static final Projet EXTRANET = Projet.mission("ACME-EXT", "Refonte extranet", ACME);
    public static final Projet CORE_BANKING = Projet.mission("BQO-CORE", "Migration core banking", BANQUE_OUEST);
    public static final Projet INTERCONTRAT = Projet.interne("INTER", "Intercontrat");
    public static final Projet ECOLE = Projet.interne("ECOLE", "École");

    public static final Collaborateur RACHEL = Collaborateur.builder().matricule(1001).nom("Rachel", "Referent")
            .entreLe(LocalDate.of(2018, 9, 3)).contrat(TypeContrat.MODALITE_1).departement("DIG")
            .roles(Role.REFERENT, Role.CONSULTANT).build();
    public static final Collaborateur ALICE = Collaborateur.builder().matricule(1002).nom("Alice", "Martin")
            .entreLe(LocalDate.of(2020, 1, 6)).contrat(TypeContrat.ETAM).departement("DIG").build();
    public static final Collaborateur BRUNO = Collaborateur.builder().matricule(1003).nom("Bruno", "Petit")
            .entreLe(LocalDate.of(2021, 3, 1)).contrat(TypeContrat.MODALITE_2).departement("DIG").build();
    public static final Collaborateur CHLOE = Collaborateur.builder().matricule(1004).nom("Chloé", "Durand")
            .entreLe(LocalDate.of(2024, 9, 2)).contrat(TypeContrat.ALTERNANT).departement("CLD").build();
    public static final Collaborateur ADMIN = Collaborateur.builder().matricule(1000).nom("Ada", "Admin")
            .entreLe(LocalDate.of(2015, 1, 5)).departement("NA").roles(Role.ADMINISTRATEUR).build();

    public static final Departement DIGITAL = new Departement("DIG", "Digital Nantes", Set.of(RACHEL.matricule()));
    public static final Departement CLOUD = new Departement("CLD", "Cloud Toulouse", Set.of());

    private Fixtures() {
    }

    /** Horloge figée à une date et heure de Paris : « 2025-05-14T09:00 ». */
    public static Clock horloge(String dateHeure) {
        return Clock.fixed(LocalDateTime.parse(dateHeure).atZone(PARIS).toInstant(), PARIS);
    }

    public static Clock horlogeLe(LocalDate date) {
        return Clock.fixed(date.atTime(9, 0).atZone(PARIS).toInstant(), PARIS);
    }

    public static LocalDate d(String iso) {
        return LocalDate.parse(iso);
    }

    public static Affectation affectation(Collaborateur collaborateur, Projet projet) {
        return Affectation.simple(collaborateur.matricule(), projet);
    }

    public static Affectation affectation(Matricule matricule, Projet projet, Periode validite, Budget budget) {
        return new Affectation(projet.code() + "-" + matricule, matricule, projet, validite, budget, BigDecimal.valueOf(550), true);
    }

    /** Vérifie qu'une action enfreint la règle attendue. */
    public static void assertRegle(CodeRegle attendue, ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOfSatisfying(RegleMetierViolee.class,
                e -> assertThat(e.code()).isEqualTo(attendue));
    }
}
