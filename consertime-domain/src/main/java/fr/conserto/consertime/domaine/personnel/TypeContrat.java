package fr.conserto.consertime.domaine.personnel;

import java.math.BigDecimal;
import java.util.Arrays;

/**
 * Types de contrat de ConserTime (table {@code TYPE_CONTRAT}, script {@code maj2-TypeContrat.sql}).
 * <p>
 * Complément : {@link #acquiertCongesPayes()}. Un stagiaire n'acquiert pas de congés payés au sens du
 * Code du travail (ses congés relèvent de la convention de stage).
 */
public enum TypeContrat {
    ETAM("ETAM", "35", true),
    MODALITE_1("MODALITE1", "37", true),
    MODALITE_2("MODALITE2", "38.5", true),
    ALTERNANT("ALTERNANT", "35", true),
    STAGIAIRE("STAGIAIRE", "35", false);

    private final String codeLegacy;
    private final BigDecimal heuresHebdomadaires;
    private final boolean acquiertCongesPayes;

    TypeContrat(String codeLegacy, String heuresHebdomadaires, boolean acquiertCongesPayes) {
        this.codeLegacy = codeLegacy;
        this.heuresHebdomadaires = new BigDecimal(heuresHebdomadaires);
        this.acquiertCongesPayes = acquiertCongesPayes;
    }

    public String codeLegacy() {
        return codeLegacy;
    }

    public BigDecimal heuresHebdomadaires() {
        return heuresHebdomadaires;
    }

    public boolean acquiertCongesPayes() {
        return acquiertCongesPayes;
    }

    public static TypeContrat depuisCodeLegacy(String code) {
        return Arrays.stream(values())
                .filter(t -> t.codeLegacy.equalsIgnoreCase(code.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Type de contrat inconnu : " + code));
    }
}
