package fr.conserto.consertime.domaine.calendrier;

import java.time.LocalDate;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/** Un jour férié daté, avec son libellé (« Lundi de Pâques »). */
public record JourFerie(LocalDate date, String libelle) implements Comparable<JourFerie> {

    public JourFerie {
        requireNonNull(date, "date");
        requireNonNull(libelle, "libelle");
    }

    @Override
    public int compareTo(JourFerie autre) {
        return date.compareTo(autre.date);
    }
}
