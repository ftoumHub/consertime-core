package fr.conserto.consertime.domaine.calendrier;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Jours fériés légaux en France, calculés avec {@code java.time}.
 * <p>
 * Remplace {@code DateUtil.isJourFerie} et la classe tierce {@code BigDate} du legacy (calcul de Pâques).
 * La liste métropolitaine est celle du legacy (11 jours, {@code DateUtil.java:66-121}).
 * Compléments :
 * <ul>
 *   <li>zone Alsace-Moselle : Vendredi saint et Saint-Étienne (26 décembre) ;</li>
 *   <li>option « lundi de Pentecôte travaillé » pour les entreprises qui en font leur journée de solidarité.</li>
 * </ul>
 */
public final class CalendrierFrance implements CalendrierJoursFeries {

    public enum Zone { METROPOLE, ALSACE_MOSELLE }

    private final Zone zone;
    private final boolean lundiDePentecoteChome;
    private final Map<Integer, List<JourFerie>> cache = new ConcurrentHashMap<>();

    public CalendrierFrance(Zone zone, boolean lundiDePentecoteChome) {
        this.zone = zone;
        this.lundiDePentecoteChome = lundiDePentecoteChome;
    }

    /** Calendrier du legacy : métropole, lundi de Pentecôte férié. */
    public static CalendrierFrance metropole() {
        return new CalendrierFrance(Zone.METROPOLE, true);
    }

    public static CalendrierFrance alsaceMoselle() {
        return new CalendrierFrance(Zone.ALSACE_MOSELLE, true);
    }

    @Override
    public List<JourFerie> joursFeries(int annee) {
        return cache.computeIfAbsent(annee, this::calculer);
    }

    private List<JourFerie> calculer(int annee) {
        LocalDate paques = dimancheDePaques(annee);
        List<JourFerie> jours = new ArrayList<>();
        jours.add(new JourFerie(LocalDate.of(annee, Month.JANUARY, 1), "Jour de l'an"));
        jours.add(new JourFerie(paques.plusDays(1), "Lundi de Pâques"));
        jours.add(new JourFerie(LocalDate.of(annee, Month.MAY, 1), "Fête du Travail"));
        jours.add(new JourFerie(LocalDate.of(annee, Month.MAY, 8), "Victoire 1945"));
        jours.add(new JourFerie(paques.plusDays(39), "Ascension"));
        if (lundiDePentecoteChome) {
            jours.add(new JourFerie(paques.plusDays(50), "Lundi de Pentecôte"));
        }
        jours.add(new JourFerie(LocalDate.of(annee, Month.JULY, 14), "Fête nationale"));
        jours.add(new JourFerie(LocalDate.of(annee, Month.AUGUST, 15), "Assomption"));
        jours.add(new JourFerie(LocalDate.of(annee, Month.NOVEMBER, 1), "Toussaint"));
        jours.add(new JourFerie(LocalDate.of(annee, Month.NOVEMBER, 11), "Armistice 1918"));
        jours.add(new JourFerie(LocalDate.of(annee, Month.DECEMBER, 25), "Noël"));
        if (zone == Zone.ALSACE_MOSELLE) {
            jours.add(new JourFerie(paques.minusDays(2), "Vendredi saint"));
            jours.add(new JourFerie(LocalDate.of(annee, Month.DECEMBER, 26), "Saint-Étienne"));
        }
        Collections.sort(jours);
        return List.copyOf(jours);
    }

    /**
     * Dimanche de Pâques du calendrier grégorien (algorithme de Meeus, Jones et Butcher).
     * Valable pour toute année grégorienne (à partir de 1583).
     */
    public static LocalDate dimancheDePaques(int annee) {
        int a = annee % 19;
        int b = annee / 100;
        int c = annee % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int mois = (h + l - 7 * m + 114) / 31;
        int jour = ((h + l - 7 * m + 114) % 31) + 1;

        return LocalDate.of(annee, mois, jour);
    }
}
