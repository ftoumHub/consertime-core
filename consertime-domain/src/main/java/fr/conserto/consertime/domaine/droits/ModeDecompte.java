package fr.conserto.consertime.domaine.droits;

import java.math.BigDecimal;

/**
 * Unité de décompte des congés payés.
 * <ul>
 *   <li>{@link #OUVRABLES} : régime légal, 2,5 jours par mois, 30 jours par an, du lundi au samedi
 *       (Code du travail, art. L3141-3) ;</li>
 *   <li>{@link #OUVRES} : équivalent courant en entreprise, 25 jours par an, du lundi au vendredi.</li>
 * </ul>
 *
 * @see <a href="https://www.legifrance.gouv.fr/codes/article_lc/LEGIARTI000033020517">Article L3141-3</a>
 */
public enum ModeDecompte {
    OUVRABLES(30, 24),
    OUVRES(25, 20);

    private final BigDecimal joursParAn;
    private final int joursEquivalentsAUnMois;

    ModeDecompte(int joursParAn, int joursEquivalentsAUnMois) {
        this.joursParAn = BigDecimal.valueOf(joursParAn);
        this.joursEquivalentsAUnMois = joursEquivalentsAUnMois;
    }

    /** Droits acquis pour une période de référence complète. */
    public BigDecimal joursParAn() {
        return joursParAn;
    }

    /**
     * Nombre de jours de travail assimilé à un mois de travail effectif : 24 jours ouvrables
     * (art. L3141-4), ou leur équivalent de 20 jours ouvrés.
     */
    public int joursEquivalentsAUnMois() {
        return joursEquivalentsAUnMois;
    }
}
