package fr.conserto.consertime.domaine.regle;

import java.util.Objects;

/**
 * Levée quand une opération du domaine enfreint une règle de gestion.
 * Le {@link CodeRegle} permet aux adaptateurs (API, interface) de traduire l'erreur sans analyser le message.
 */
public final class RegleMetierViolee extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final CodeRegle code;

    public RegleMetierViolee(CodeRegle code) {
        this(code, code.libelle());
    }

    public RegleMetierViolee(CodeRegle code, String detail) {
        super(code.name() + " : " + Objects.requireNonNull(detail));
        this.code = code;
    }

    public CodeRegle code() {
        return code;
    }

    /** Lève l'exception si la condition est fausse. */
    public static void verifier(boolean condition, CodeRegle code) {
        if (!condition) {
            throw new RegleMetierViolee(code);
        }
    }

    /** Lève l'exception, avec un détail, si la condition est fausse. */
    public static void verifier(boolean condition, CodeRegle code, String detail) {
        if (!condition) {
            throw new RegleMetierViolee(code, detail);
        }
    }
}
