package fr.conserto.consertime.domaine.temps;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Quantité de temps exprimée en jours (1 = une journée, 0,5 = une demi-journée).
 * <p>
 * Le legacy stockait ces valeurs dans {@code TIMESHEET_ENTRY.HOURS} (float), malgré le nom de la colonne :
 * la validation d'un congé écrit 1.0 ou 0.5 ({@code CongesAdminPage}) et le CRA additionne des jours.
 * Ici : {@link BigDecimal} avec une décimale, pour ne pas perdre les demi-journées comme le faisait
 * l'export FDT ({@code ExportFdtBody.calculerSommeDesTemps} tronquait avec {@code intValue()}).
 */
public record Jours(BigDecimal valeur) implements Comparable<Jours> {

    public static final Jours ZERO = new Jours(BigDecimal.ZERO);
    public static final Jours DEMI = new Jours(new BigDecimal("0.5"));
    public static final Jours UN = new Jours(BigDecimal.ONE);

    private static final BigDecimal DEUX = BigDecimal.valueOf(2);

    public Jours {
        requireNonNull(valeur, "valeur");
        // forme canonique : au moins une décimale (1.0, 0.5), sans zéros inutiles (0.50 -> 0.5)
        BigDecimal canonique = valeur.stripTrailingZeros();
        valeur = canonique.scale() < 1 ? canonique.setScale(1, RoundingMode.UNNECESSARY) : canonique;
    }

    public static Jours of(String valeur) {
        return new Jours(new BigDecimal(valeur.replace(',', '.')));
    }

    public static Jours of(double valeur) {
        return new Jours(BigDecimal.valueOf(valeur));
    }

    public Jours plus(Jours autre) {
        return new Jours(valeur.add(autre.valeur));
    }

    public Jours moins(Jours autre) {
        return new Jours(valeur.subtract(autre.valeur));
    }

    public boolean estMultipleDeDemi() {
        return valeur.multiply(DEUX).stripTrailingZeros().scale() <= 0;
    }

    public boolean estPositif() {
        return valeur.signum() > 0;
    }

    public boolean depasse(Jours autre) {
        return compareTo(autre) > 0;
    }

    @Override
    public int compareTo(Jours autre) {
        return valeur.compareTo(autre.valeur);
    }

    /** Écriture du legacy dans le CRA : « 1.0 », « 0.5 ». */
    @Override
    public String toString() {
        return valeur.toPlainString();
    }
}
