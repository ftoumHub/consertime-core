package fr.conserto.consertime.domaine.temps;

import fr.conserto.consertime.domaine.calendrier.Periode;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Contrôle préalable à l'export du CRA et de la FDT.
 * <p>
 * Legacy : {@code TimesheetExportCriteriaPanel.checkNbJoursOuvresSaisies}. Les jours attendus sont les jours
 * ouvrés du mois pendant la présence du collaborateur (date d'entrée, date de sortie).
 * <p>
 * Écart assumé : le legacy comparait le nombre de lignes saisies au nombre de jours ouvrés, si bien que deux
 * demi-journées sur un même jour comptaient double et qu'un jour manquant pouvait passer inaperçu. Ici, chaque
 * jour ouvré doit totaliser exactement 1 jour. Le legacy perdait aussi la date d'entrée quand une date de
 * sortie existait ; les deux bornes sont désormais appliquées ensemble.
 */
public final class ControleCompletude {

    private ControleCompletude() {
    }

    public record Resultat(long joursOuvresAttendus, long joursComplets, List<LocalDate> joursIncomplets) {

        public Resultat {
            joursIncomplets = List.copyOf(joursIncomplets);
        }

        public boolean estComplet() {
            return joursIncomplets.isEmpty();
        }

        /** Message affiché par le legacy, repris pour l'interface. */
        public List<String> messages() {
            return List.of("Merci de compléter votre saisie :",
                    "Nombre de jours ouvrés sur la période : " + joursOuvresAttendus,
                    "Nombre de jours saisis : " + joursComplets);
        }
    }

    public static Resultat verifier(FeuilleDeTemps feuille) {
        Optional<Periode> presence = feuille.collaborateur().presenceSur(feuille.periode());
        List<LocalDate> attendus = presence.map(p -> feuille.calendrier().joursOuvres(p)).orElse(List.of());
        List<LocalDate> incomplets = attendus.stream()
                .filter(jour -> feuille.totalDu(jour).compareTo(Jours.UN) != 0)
                .toList();

        return new Resultat(attendus.size(), attendus.size() - incomplets.size(), incomplets);
    }
}
