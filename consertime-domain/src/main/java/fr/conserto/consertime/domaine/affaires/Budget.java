package fr.conserto.consertime.domaine.affaires;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Budget d'une affectation, en jours (types {@code ASSIGNMENT_DATE}, {@code ASSIGNMENT_TIME_ALLOTTED_FIXED}
 * et {@code ASSIGNMENT_TIME_ALLOTTED_FLEX} du legacy, {@code EhourConstants.java:29-31}).
 */
public sealed interface Budget permits Budget.SansBudget, Budget.Fixe, Budget.Flexible {

    /** Affectation bornée par des dates seulement. */
    record SansBudget() implements Budget {
    }

    /** Budget ferme : on peut l'atteindre mais pas le dépasser. */
    record Fixe(BigDecimal jours) implements Budget {
        public Fixe {
            Objects.requireNonNull(jours, "jours");
        }
    }

    /** Budget avec dépassement autorisé, jusqu'à {@code jours + depassementAutorise}. */
    record Flexible(BigDecimal jours, BigDecimal depassementAutorise) implements Budget {
        public Flexible {
            Objects.requireNonNull(jours, "jours");
            Objects.requireNonNull(depassementAutorise, "depassementAutorise");
        }

        public BigDecimal maximum() {
            return jours.add(depassementAutorise);
        }
    }

    static Budget aucun() {
        return new SansBudget();
    }

    static Budget fixe(String jours) {
        return new Fixe(new BigDecimal(jours));
    }

    static Budget flexible(String jours, String depassementAutorise) {
        return new Flexible(new BigDecimal(jours), new BigDecimal(depassementAutorise));
    }
}
