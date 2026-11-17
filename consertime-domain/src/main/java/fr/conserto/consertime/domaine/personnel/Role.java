package fr.conserto.consertime.domaine.personnel;

/** Rôles applicatifs (constantes {@code UserRole.ROLE_*} du legacy). */
public enum Role {
    CONSULTANT("ROLE_CONSULTANT"),
    CHEF_DE_PROJET("ROLE_PROJECTMANAGER"),
    REFERENT("ROLE_REFERENT"),
    ADMINISTRATEUR("ROLE_ADMIN"),
    REPORTING("ROLE_REPORT");

    private final String codeLegacy;

    Role(String codeLegacy) {
        this.codeLegacy = codeLegacy;
    }

    public String codeLegacy() {
        return codeLegacy;
    }
}
