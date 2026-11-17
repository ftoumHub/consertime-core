package fr.conserto.consertime.domaine.conges;

import fr.conserto.consertime.domaine.calendrier.CalendrierOuvre;
import fr.conserto.consertime.domaine.calendrier.DemiJournee;
import fr.conserto.consertime.domaine.calendrier.Periode;
import fr.conserto.consertime.domaine.regle.CodeRegle;
import fr.conserto.consertime.domaine.regle.RegleMetierViolee;
import fr.conserto.consertime.domaine.temps.Jours;

import java.time.LocalDate;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

/**
 * Période d'absence à la demi-journée près.
 *
 * @param debut  premier jour d'absence
 * @param debutA {@code MATIN} : absent dès le matin ; {@code APRES_MIDI} : absent à partir de midi
 * @param fin    dernier jour d'absence
 * @param finA   {@code MATIN} : de retour l'après-midi ; {@code APRES_MIDI} : absent toute la journée
 *
 *               <p>Règle de décompte (legacy {@code DemandeCongeFormPanel.updateDateDemande}) : chaque jour ouvré compte
 *               1 jour ; un début l'après-midi ou une fin le matin retire 0,5 au jour concerné.
 *               <p>Écart assumé : le legacy parcourait la période fin exclue (bug de {@code createDateSequence}),
 *               oubliant le dernier jour dès qu'une demande durait plus d'une journée. Une semaine du lundi au vendredi
 *               valait 4 jours au lieu de 5.
 */
public record PeriodeConge(LocalDate debut, DemiJournee debutA, LocalDate fin, DemiJournee finA) {

    public PeriodeConge {
        requireNonNull(debut, "debut");
        requireNonNull(debutA, "debutA");
        requireNonNull(fin, "fin");
        requireNonNull(finA, "finA");

        RegleMetierViolee.verifier(
                demiJourneeDebut(debut, debutA) <= demiJourneeFin(fin, finA),
                CodeRegle.CONGE_DATES_INVALIDES,
                debut + " " + debutA.libelle() + " -> " + fin + " " + finA.libelle());
    }

    /**
     * Journées entières, du matin du premier jour au soir du dernier.
     */
    public static PeriodeConge journeesEntieres(LocalDate debut, LocalDate fin) {
        return new PeriodeConge(debut, DemiJournee.MATIN, fin, DemiJournee.APRES_MIDI);
    }

    public static PeriodeConge journee(LocalDate jour) {
        return journeesEntieres(jour, jour);
    }

    public static PeriodeConge demiJournee(LocalDate jour, DemiJournee demi) {
        return new PeriodeConge(jour, demi, jour, demi);
    }

    public Periode jours() {
        return new Periode(debut, fin);
    }

    /**
     * Valeur de chaque jour ouvré d'absence (1 ou 0,5), dans l'ordre chronologique.
     */
    public SortedMap<LocalDate, Jours> decompte(CalendrierOuvre calendrier) {
        return decompteSelon(calendrier::estOuvre);
    }

    /**
     * Décompte sur les jours retenus par le prédicat : jours ouvrés (cas général) ou jours ouvrables
     * (décompte légal des congés payés, samedi compris).
     */
    public SortedMap<LocalDate, Jours> decompteSelon(Predicate<LocalDate> jourDecompte) {
        SortedMap<LocalDate, Jours> parJour = new TreeMap<>();
        jours().jours()
                .filter(jourDecompte)
                .forEach(jour -> {
                    Jours valeur = Jours.UN;
                    if (jour.equals(debut) && debutA == DemiJournee.APRES_MIDI) {
                        valeur = valeur.moins(Jours.DEMI);
                    }
                    if (jour.equals(fin) && finA == DemiJournee.MATIN) {
                        valeur = valeur.moins(Jours.DEMI);
                    }
                    if (valeur.estPositif()) {
                        parJour.put(jour, valeur);
                    }
                });
        return parJour;
    }

    public Jours nombreDeJours(CalendrierOuvre calendrier) {
        return decompte(calendrier)
                .values()
                .stream()
                .reduce(Jours.ZERO, Jours::plus);
    }

    /**
     * Vrai si les deux périodes partagent au moins une demi-journée (requêtes {@code findDemandesForDates*} du legacy).
     */
    public boolean chevauche(PeriodeConge autre) {
        return demiJourneeDebut(debut, debutA) <= demiJourneeFin(autre.fin, autre.finA)
                && demiJourneeDebut(autre.debut, autre.debutA) <= demiJourneeFin(fin, finA);
    }

    private static long demiJourneeDebut(LocalDate jour, DemiJournee demi) {
        return jour.toEpochDay() * 2 + (demi == DemiJournee.APRES_MIDI ? 1 : 0);
    }

    private static long demiJourneeFin(LocalDate jour, DemiJournee demi) {
        return jour.toEpochDay() * 2 + (demi == DemiJournee.MATIN ? 0 : 1);
    }

    @Override
    public String toString() {
        return "du " + debut + " (" + debutA.libelle() + ") au " + fin + " (" + finA.libelle() + ")";
    }
}
