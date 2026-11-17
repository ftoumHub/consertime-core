package fr.conserto.consertime.domaine.droits;

import fr.conserto.consertime.domaine.affaires.TypeAbsence;
import fr.conserto.consertime.domaine.calendrier.CalendrierOuvre;
import fr.conserto.consertime.domaine.calendrier.Periode;
import fr.conserto.consertime.domaine.conges.DemandeConge;
import fr.conserto.consertime.domaine.conges.EtatDemande;
import fr.conserto.consertime.domaine.personnel.Collaborateur;
import fr.conserto.consertime.domaine.temps.Jours;

import fr.conserto.consertime.domaine.conges.PeriodeConge;
import fr.conserto.consertime.domaine.calendrier.DemiJournee;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Calcul des droits à congés payés selon la date d'embauche (règle complétée, absente du legacy).
 * <p>
 * Règles appliquées :
 * <ol>
 *   <li>un mois de la période de référence ouvre droit à 1/12 des droits annuels s'il est entièrement couvert par
 *       la présence du collaborateur, ou si la présence atteint l'équivalent d'un mois (24 jours ouvrables ou
 *       20 jours ouvrés, art. L3141-4) ;</li>
 *   <li>un mois partiel (embauche ou départ en cours de mois) ouvre droit au prorata de ces jours ;</li>
 *   <li>seuls les mois terminés à la date de calcul sont acquis, plus le mois du départ s'il a eu lieu ;</li>
 *   <li>les droits sont plafonnés au total annuel, puis arrondis à l'entier supérieur une fois la période close
 *       (art. L3141-7) ;</li>
 *   <li>les congés d'ancienneté s'apprécient au lendemain de la fin de la période ;</li>
 *   <li>un contrat qui n'acquiert pas de congés payés (stage) a des droits nuls.</li>
 * </ol>
 * Non couverts : absences non assimilées à du travail effectif, fractionnement, report des droits.
 */
public final class CalculDroitsCongesPayes {

    private static final BigDecimal DOUZE = BigDecimal.valueOf(12);

    private final PolitiqueCongesPayes politique;
    private final CalendrierOuvre calendrier;

    public CalculDroitsCongesPayes(PolitiqueCongesPayes politique, CalendrierOuvre calendrier) {
        this.politique = requireNonNull(politique);
        this.calendrier = requireNonNull(calendrier);
    }

    public PolitiqueCongesPayes politique() {
        return politique;
    }

    public PeriodeReference periodeReferenceEnCours(Clock clock) {
        return PeriodeReference.contenant(LocalDate.now(clock), politique.debutPeriodeReference());
    }

    /** Droits acquis sur la période de référence en cours, à la date du jour. */
    public DroitsCongesPayes enCours(Collaborateur collaborateur, Clock clock) {
        return calculer(collaborateur, periodeReferenceEnCours(clock), LocalDate.now(clock));
    }

    /** Droits acquis sur la période de référence précédente : ceux qui se prennent en ce moment. */
    public DroitsCongesPayes aPrendre(Collaborateur collaborateur, Clock clock) {
        PeriodeReference precedente = periodeReferenceEnCours(clock).precedente();
        return calculer(collaborateur, precedente, LocalDate.now(clock));
    }

    public DroitsCongesPayes calculer(Collaborateur collaborateur, PeriodeReference reference, LocalDate aDate) {
        ModeDecompte mode = politique.mode();
        LocalDate sortie = collaborateur.sortie().orElse(null);
        boolean close = !aDate.isBefore(reference.fin()) || (sortie != null && !aDate.isBefore(sortie)
                && reference.periode().contient(sortie));

        if (!collaborateur.typeContrat().acquiertCongesPayes()) {
            return new DroitsCongesPayes(reference, mode, BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2), 0, close);
        }

        BigDecimal moisAcquis = BigDecimal.ZERO;
        for (YearMonth mois : reference.mois()) {
            if (estAcquis(mois, sortie, aDate)) {
                moisAcquis = moisAcquis.add(fractionDuMois(collaborateur, mois));
            }
        }

        BigDecimal bruts = mode.joursParAn().multiply(moisAcquis).divide(DOUZE, MathContext.DECIMAL64)
                .min(mode.joursParAn())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal retenus = close ? bruts.setScale(0, RoundingMode.CEILING).setScale(2) : bruts;
        int anciennete = close && retenus.signum() > 0
                ? politique.joursAnciennete(collaborateur.anciennete(reference.fin().plusDays(1)).getYears())
                : 0;
        return new DroitsCongesPayes(reference, mode, bruts, retenus, anciennete, close);
    }

    private static boolean estAcquis(YearMonth mois, LocalDate sortie, LocalDate aDate) {
        boolean moisTermine = !aDate.isBefore(mois.atEndOfMonth());
        boolean moisDuDepart = sortie != null && YearMonth.from(sortie).equals(mois) && !aDate.isBefore(sortie);
        return moisTermine || moisDuDepart;
    }

    /** Part du mois acquise : 1 si mois complet ou équivalent atteint, sinon prorata des jours de présence. */
    BigDecimal fractionDuMois(Collaborateur collaborateur, YearMonth mois) {
        Periode periodeMois = Periode.mois(mois);
        Optional<Periode> presence = collaborateur.presenceSur(periodeMois);
        if (presence.isEmpty()) {
            return BigDecimal.ZERO;
        }
        if (presence.get().equals(periodeMois)) {
            return BigDecimal.ONE;
        }
        long joursPresents = politique.mode() == ModeDecompte.OUVRABLES
                ? calendrier.nombreJoursOuvrables(presence.get())
                : calendrier.nombreJoursOuvres(presence.get());
        BigDecimal equivalent = BigDecimal.valueOf(politique.mode().joursEquivalentsAUnMois());
        return BigDecimal.valueOf(joursPresents).divide(equivalent, MathContext.DECIMAL64).min(BigDecimal.ONE);
    }

    /** Solde des droits à prendre à la date du jour. */
    public SoldeCongesPayes solde(Collaborateur collaborateur, Collection<DemandeConge> demandes, Clock clock) {
        DroitsCongesPayes droits = aPrendre(collaborateur, clock);
        Periode prise = droits.periodeReference().periodeDePrise();
        Jours poses = demandes.stream()
                .filter(d -> d.demandeur().matricule().equals(collaborateur.matricule()))
                .filter(d -> d.etat() == EtatDemande.VALIDEE)
                .filter(d -> d.type().map(TypeAbsence::imputeSurDroitsCongesPayes).orElse(false))
                .filter(d -> prise.contient(d.periode().debut()))
                .map(d -> politique.mode() == ModeDecompte.OUVRABLES
                        ? joursOuvrablesDecomptes(d.periode())
                        : d.nombreDeJours())
                .reduce(Jours.ZERO, Jours::plus);
        return new SoldeCongesPayes(droits, poses.valeur(), droits.total().subtract(poses.valeur()));
    }

    /**
     * Décompte en jours ouvrables : chaque jour ouvrable d'absence, y compris ceux qui précèdent la reprise
     * (un congé du lundi au vendredi, reprise le lundi, coûte 6 jours ouvrables : le samedi est décompté).
     */
    private Jours joursOuvrablesDecomptes(PeriodeConge periode) {
        Jours total = periode.decompteSelon(calendrier::estOuvrable).values().stream().reduce(Jours.ZERO, Jours::plus);
        if (periode.finA() == DemiJournee.APRES_MIDI) {
            LocalDate jour = periode.fin().plusDays(1);
            while (!calendrier.estOuvre(jour)) {
                if (calendrier.estOuvrable(jour)) {
                    total = total.plus(Jours.UN);
                }
                jour = jour.plusDays(1);
            }
        }
        return total;
    }
}
