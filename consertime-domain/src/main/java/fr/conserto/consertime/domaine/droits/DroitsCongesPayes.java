package fr.conserto.consertime.domaine.droits;

import java.math.BigDecimal;

/**
 * Droits à congés payés acquis sur une période de référence, à une date donnée.
 *
 * @param acquisBruts      droits acquis, au centième, avant arrondi
 * @param acquis           droits retenus : arrondis à l'entier supérieur une fois la période close (art. L3141-7),
 *                         sinon égaux aux droits bruts (acquisition en cours)
 * @param joursAnciennete  congés d'ancienneté accordés au titre de cette période (période close uniquement)
 * @param periodeClose     vrai si la date de calcul est après la fin de la période ou la sortie du collaborateur
 */
public record DroitsCongesPayes(PeriodeReference periodeReference, ModeDecompte mode, BigDecimal acquisBruts,
                                BigDecimal acquis, int joursAnciennete, boolean periodeClose) {

    public BigDecimal total() {
        return acquis.add(BigDecimal.valueOf(joursAnciennete));
    }
}
