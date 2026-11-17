package fr.conserto.consertime.domaine.affaires;

/**
 * Nature d'un projet. Le legacy ne distinguait que facturable / non facturable ;
 * les projets d'absence étaient reconnus par leur client (CONSERTO).
 */
public enum NatureProjet {
    MISSION,
    INTERNE,
    ABSENCE
}
