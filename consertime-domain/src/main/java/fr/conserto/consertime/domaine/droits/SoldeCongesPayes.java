package fr.conserto.consertime.domaine.droits;

import java.math.BigDecimal;

/**
 * Situation des congés payés à prendre : droits de la période de référence close, jours déjà posés
 * (demandes validées dont le premier jour tombe dans la période de prise) et solde.
 */
public record SoldeCongesPayes(DroitsCongesPayes droits, BigDecimal joursPoses, BigDecimal solde) {

    public boolean estDebiteur() {
        return solde.signum() < 0;
    }
}
