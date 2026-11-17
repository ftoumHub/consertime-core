package fr.conserto.consertime.domaine.temps;

import fr.conserto.consertime.domaine.affaires.Affectation;
import fr.conserto.consertime.domaine.affaires.Projet;
import fr.conserto.consertime.domaine.calendrier.CalendrierOuvre;
import fr.conserto.consertime.domaine.calendrier.Periode;
import fr.conserto.consertime.domaine.personnel.Collaborateur;
import fr.conserto.consertime.domaine.regle.CodeRegle;
import fr.conserto.consertime.domaine.regle.RegleMetierViolee;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toUnmodifiableList;

/**
 * Saisies d'un collaborateur sur un mois. Agrégat qui garantit les règles de saisie :
 * <ul>
 *   <li>SAISIE_HORS_MOIS : la date appartient au mois ;</li>
 *   <li>SAISIE_HORS_PRESENCE : le collaborateur est présent ce jour-là (complément) ;</li>
 *   <li>SAISIE_HORS_AFFECTATION : l'affectation est la sienne et ouverte ce jour-là ;</li>
 *   <li>SAISIE_QUANTITE_INVALIDE : 0,5 ou 1 jour ;</li>
 *   <li>SAISIE_JOUR_DEPASSE : au plus 1 jour par date, tous projets confondus (complément :
 *       le legacy laissait un congé validé s'ajouter à une journée déjà saisie).</li>
 * </ul>
 * Les saisies le week-end ou un jour férié restent possibles, comme dans le legacy ;
 * {@link #saisiesHorsJoursOuvres()} les signale.
 */
public final class FeuilleDeTemps {

    private static final Comparator<Saisie> ORDRE =
            Comparator.comparing(Saisie::date).thenComparing(s -> s.projet().code());

    private final Collaborateur collaborateur;
    private final YearMonth mois;
    private final CalendrierOuvre calendrier;
    private final Map<Cle, Saisie> saisies = new LinkedHashMap<>();

    private record Cle(LocalDate date, String codeProjet) {
    }

    public FeuilleDeTemps(Collaborateur collaborateur, YearMonth mois, CalendrierOuvre calendrier) {
        this.collaborateur = requireNonNull(collaborateur);
        this.mois = requireNonNull(mois);
        this.calendrier = requireNonNull(calendrier);
    }

    /** Ajoute ou remplace la saisie de ce projet pour ce jour. */
    public Saisie saisir(Affectation affectation, LocalDate date, Jours jours, String commentaire) {
        RegleMetierViolee.verifier(affectation.matricule().equals(collaborateur.matricule())
                && affectation.estOuverteLe(date), CodeRegle.SAISIE_HORS_AFFECTATION, affectation.id() + " le " + date);
        return enregistrer(new Saisie(date, affectation.projet(), jours, commentaire));
    }

    public Saisie saisir(Affectation affectation, LocalDate date, Jours jours) {
        return saisir(affectation, date, jours, null);
    }

    /**
     * Intègre des saisies produites par le domaine, par exemple celles d'un congé validé.
     * Tout ou rien : si une saisie enfreint une règle, aucune n'est ajoutée.
     */
    public void integrer(Collection<Saisie> nouvelles) {
        Map<Cle, Saisie> avant = new LinkedHashMap<>(saisies);
        try {
            nouvelles.forEach(this::enregistrer);
        } catch (RegleMetierViolee e) {
            saisies.clear();
            saisies.putAll(avant);
            throw e;
        }
    }

    /** Retire des saisies, par exemple celles d'un congé annulé. */
    public void retirer(Collection<Saisie> aRetirer) {
        aRetirer.forEach(s -> saisies.remove(new Cle(s.date(), s.projet().code())));
    }

    private Saisie enregistrer(Saisie saisie) {
        LocalDate date = saisie.date();
        RegleMetierViolee.verifier(YearMonth.from(date).equals(mois), CodeRegle.SAISIE_HORS_MOIS, date + " hors de " + mois);
        RegleMetierViolee.verifier(collaborateur.estPresentLe(date), CodeRegle.SAISIE_HORS_PRESENCE, date.toString());
        Jours jours = saisie.jours();
        RegleMetierViolee.verifier(jours.estPositif() && !jours.depasse(Jours.UN) && jours.estMultipleDeDemi(),
                CodeRegle.SAISIE_QUANTITE_INVALIDE, jours + " le " + date);

        Cle cle = new Cle(date, saisie.projet().code());
        Jours autres = saisies.values().stream()
                .filter(s -> s.date().equals(date) && !s.projet().code().equals(cle.codeProjet()))
                .map(Saisie::jours)
                .reduce(Jours.ZERO, Jours::plus);
        RegleMetierViolee.verifier(!autres.plus(jours).depasse(Jours.UN), CodeRegle.SAISIE_JOUR_DEPASSE,
                autres.plus(jours) + " jour(s) le " + date);
        saisies.put(cle, saisie);
        return saisie;
    }

    public Collaborateur collaborateur() {
        return collaborateur;
    }

    public YearMonth mois() {
        return mois;
    }

    public Periode periode() {
        return Periode.mois(mois);
    }

    public CalendrierOuvre calendrier() {
        return calendrier;
    }

    /** Saisies triées par date puis par code projet. */
    public List<Saisie> saisies() {
        return saisies.values().stream().sorted(ORDRE).toList();
    }

    public Map<LocalDate, List<Saisie>> parJour() {
        return saisies().stream()
                .collect(Collectors.groupingBy(Saisie::date, TreeMap::new, toList()));
    }

    public Jours totalDu(LocalDate date) {
        return saisies.values().stream()
                .filter(s -> s.date().equals(date))
                .map(Saisie::jours)
                .reduce(Jours.ZERO, Jours::plus);
    }

    public Jours totalPour(Projet projet) {
        return saisies.values().stream().filter(s -> s.projet().code().equals(projet.code()))
                .map(Saisie::jours).reduce(Jours.ZERO, Jours::plus);
    }

    public Jours total() {
        return saisies.values().stream()
                .map(Saisie::jours)
                .reduce(Jours.ZERO, Jours::plus);
    }

    /** Projets saisis dans le mois, dans l'ordre de leur première saisie. */
    public List<Projet> projets() {
        Map<String, Projet> parCode = new LinkedHashMap<>();
        saisies().forEach(s -> parCode.putIfAbsent(s.projet().code(), s.projet()));
        return new ArrayList<>(parCode.values());
    }

    public List<Saisie> saisiesHorsJoursOuvres() {
        return saisies().stream()
                .filter(s -> !calendrier.estOuvre(s.date()))
                .toList();
    }
}
