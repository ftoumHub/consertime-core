package fr.conserto.consertime.domaine.affaires;

import java.util.Objects;

/** Client (table {@code CUSTOMER}). Conserto est elle-même un client : elle porte les projets internes et d'absence. */
public record Client(String code, String nom) {

    /** Client technique du legacy, recherché en dur par {@code CongeSelectionPanel} (« CONSERTO » / « CONS »). */
    public static final Client CONSERTO = new Client("CONS", "CONSERTO");

    public Client {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(nom, "nom");
    }
}
