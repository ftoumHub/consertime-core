package fr.conserto.consertime.domaine.droits;

import java.time.MonthDay;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Paramètres d'acquisition des congés payés.
 *
 * @param mode                    jours ouvrables (légal) ou jours ouvrés
 * @param debutPeriodeReference   début de la période de référence, le 1er juin par défaut (art. R3141-4)
 * @param paliersAnciennete       jours supplémentaires selon l'ancienneté (convention collective)
 */
public record PolitiqueCongesPayes(ModeDecompte mode, MonthDay debutPeriodeReference,
                                   List<PalierAnciennete> paliersAnciennete) {

    /** Jours supplémentaires accordés à partir d'un nombre d'années d'ancienneté. */
    public record PalierAnciennete(int annees, int joursSupplementaires) {
    }

    public PolitiqueCongesPayes {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(debutPeriodeReference, "debutPeriodeReference");
        paliersAnciennete = paliersAnciennete.stream()
                .sorted(Comparator.comparingInt(PalierAnciennete::annees))
                .toList();
    }

    /** Régime légal seul, période de référence du 1er juin au 31 mai. */
    public static PolitiqueCongesPayes codeDuTravail(ModeDecompte mode) {
        return new PolitiqueCongesPayes(mode, MonthDay.of(6, 1), List.of());
    }

    /**
     * Régime légal plus les congés d'ancienneté de la convention Syntec (art. 23) :
     * +1 jour après 5 ans, +2 après 10 ans, +3 après 15 ans, +4 après 20 ans.
     * À confirmer avec la convention et les accords applicables chez Conserto.
     */
    public static PolitiqueCongesPayes syntec(ModeDecompte mode) {
        return new PolitiqueCongesPayes(mode, MonthDay.of(6, 1), List.of(
                new PalierAnciennete(5, 1),
                new PalierAnciennete(10, 2),
                new PalierAnciennete(15, 3),
                new PalierAnciennete(20, 4)));
    }

    public int joursAnciennete(int anneesAnciennete) {
        return paliersAnciennete.stream()
                .filter(p -> anneesAnciennete >= p.annees())
                .mapToInt(PalierAnciennete::joursSupplementaires)
                .max()
                .orElse(0);
    }
}
