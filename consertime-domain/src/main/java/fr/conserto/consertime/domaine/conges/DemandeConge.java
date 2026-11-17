package fr.conserto.consertime.domaine.conges;

import fr.conserto.consertime.domaine.affaires.Projet;
import fr.conserto.consertime.domaine.affaires.TypeAbsence;
import fr.conserto.consertime.domaine.calendrier.CalendrierOuvre;
import fr.conserto.consertime.domaine.personnel.Collaborateur;
import fr.conserto.consertime.domaine.personnel.Departement;
import fr.conserto.consertime.domaine.personnel.Matricule;
import fr.conserto.consertime.domaine.personnel.Role;
import fr.conserto.consertime.domaine.regle.CodeRegle;
import fr.conserto.consertime.domaine.regle.RegleMetierViolee;
import fr.conserto.consertime.domaine.temps.Jours;
import fr.conserto.consertime.domaine.temps.Saisie;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * Demande de congé et son cycle de vie : EN_ATTENTE → VALIDEE | REFUSEE | ANNULEE ; VALIDEE → ANNULEE.
 * <p>
 * Toutes les dates « du jour » viennent d'une {@link Clock}, ce qui rend les règles temporelles testables.
 * Les numéros de règle renvoient à {@code REGLES.md}.
 */
public final class DemandeConge {

    private final String id;
    private final Collaborateur demandeur;
    private final Projet projet;
    private final PeriodeConge periode;
    private final String motif;
    private final Jours nombreDeJours;
    private final LocalDateTime dateDepot;

    private EtatDemande etat = EtatDemande.EN_ATTENTE;
    private Matricule decideur;
    private LocalDateTime dateDecision;
    private String motifRefus;

    private DemandeConge(String id, Collaborateur demandeur, Projet projet, PeriodeConge periode, String motif,
                         Jours nombreDeJours, LocalDateTime dateDepot) {
        this.id = id;
        this.demandeur = demandeur;
        this.projet = projet;
        this.periode = periode;
        this.motif = motif;
        this.nombreDeJours = nombreDeJours;
        this.dateDepot = dateDepot;
    }

    /**
     * Dépôt d'une demande par un collaborateur.
     *
     * @param demandesExistantes demandes déjà connues du collaborateur, pour le contrôle de chevauchement
     */
    public static DemandeConge deposer(Collaborateur demandeur, TypeAbsence type, PeriodeConge periode, String motif,
                                       Collection<DemandeConge> demandesExistantes, CalendrierOuvre calendrier,
                                       Clock clock) {
        return deposer(demandeur, type.projet(), periode, motif, demandesExistantes, calendrier, clock);
    }

    public static DemandeConge deposer(Collaborateur demandeur, Projet projet, PeriodeConge periode, String motif,
                                       Collection<DemandeConge> demandesExistantes, CalendrierOuvre calendrier,
                                       Clock clock) {
        requireNonNull(demandeur, "demandeur");
        RegleMetierViolee.verifier(projet.estAbsence(), CodeRegle.CONGE_PROJET_NON_ABSENCE, projet.code());
        RegleMetierViolee.verifier(demandeur.estPresentLe(periode.debut()) && demandeur.estPresentLe(periode.fin()),
                CodeRegle.CONGE_HORS_PRESENCE, periode.toString());

        Jours nombreDeJours = periode.nombreDeJours(calendrier);
        RegleMetierViolee.verifier(nombreDeJours.estPositif(), CodeRegle.CONGE_SANS_JOUR_OUVRE, periode.toString());

        demandesExistantes.stream()
                .filter(d -> d.demandeur.matricule().equals(demandeur.matricule()))
                .filter(d -> d.etat.estActive())
                .filter(d -> d.periode.chevauche(periode))
                .findFirst()
                .ifPresent(d -> {
                    throw new RegleMetierViolee(CodeRegle.CONGE_CHEVAUCHEMENT, "demande " + d.id + " " + d.periode);
                });

        return new DemandeConge(UUID.randomUUID().toString(), demandeur, projet, periode, motif, nombreDeJours,
                LocalDateTime.now(clock));
    }

    /**
     * Validation par un référent du département du demandeur ou par un administrateur.
     * <p>
     * L'appelant doit intégrer les saisies retournées dans les feuilles de temps. Pour que le statut et les saisies
     * passent ensemble, préférer {@link #valider(Collaborateur, Departement, CalendrierOuvre, Clock, Consumer)}.
     *
     * @return les saisies d'absence à intégrer dans les feuilles de temps (1 ou 0,5 par jour ouvré)
     */
    public List<Saisie> valider(Collaborateur valideur, Departement departementDuDemandeur,
                                CalendrierOuvre calendrier, Clock clock) {
        return valider(valideur, departementDuDemandeur, calendrier, clock, saisies -> { });
    }

    /**
     * Validation atomique : les saisies sont d'abord remises à {@code enregistrement} (typiquement
     * {@code FeuilleDeTemps::integrer}) ; si celui-ci lève une exception, la demande reste en attente.
     * Le legacy passait la demande à VALIDE puis avalait l'erreur d'imputation.
     */
    public List<Saisie> valider(Collaborateur valideur, Departement departementDuDemandeur,
                                CalendrierOuvre calendrier, Clock clock, Consumer<List<Saisie>> enregistrement) {
        verifierDecision(valideur, departementDuDemandeur);
        List<Saisie> saisies = saisies(calendrier);
        enregistrement.accept(saisies);
        etat = EtatDemande.VALIDEE;
        decideur = valideur.matricule();
        dateDecision = LocalDateTime.now(clock);
        return saisies;
    }

    public void refuser(Collaborateur valideur, Departement departementDuDemandeur, String motifDuRefus, Clock clock) {
        verifierDecision(valideur, departementDuDemandeur);
        etat = EtatDemande.REFUSEE;
        decideur = valideur.matricule();
        dateDecision = LocalDateTime.now(clock);
        motifRefus = motifDuRefus;
    }

    /**
     * Annulation par le demandeur (ou un administrateur). Une demande en attente s'annule à tout moment ;
     * une demande validée seulement avant son premier jour.
     *
     * @return les saisies d'absence à retirer des feuilles de temps (vide si la demande n'était pas validée)
     */
    public List<Saisie> annuler(Collaborateur auteur, CalendrierOuvre calendrier, Clock clock) {
        RegleMetierViolee.verifier(auteur.matricule().equals(demandeur.matricule()) || auteur.aLeRole(Role.ADMINISTRATEUR),
                CodeRegle.CONGE_ANNULATION_NON_AUTORISEE);
        RegleMetierViolee.verifier(etat.estActive(), CodeRegle.CONGE_TRANSITION_INTERDITE, "annulation d'une demande " + etat.libelle());
        boolean etaitValidee = etat == EtatDemande.VALIDEE;
        if (etaitValidee) {
            RegleMetierViolee.verifier(LocalDate.now(clock).isBefore(periode.debut()), CodeRegle.CONGE_ANNULATION_TARDIVE,
                    "congé commencé le " + periode.debut());
        }
        etat = EtatDemande.ANNULEE;
        dateDecision = LocalDateTime.now(clock);
        return etaitValidee ? saisies(calendrier) : List.of();
    }

    private void verifierDecision(Collaborateur valideur, Departement departementDuDemandeur) {
        RegleMetierViolee.verifier(etat == EtatDemande.EN_ATTENTE, CodeRegle.CONGE_TRANSITION_INTERDITE,
                "demande " + etat.libelle());
        RegleMetierViolee.verifier(!valideur.matricule().equals(demandeur.matricule()), CodeRegle.CONGE_AUTO_DECISION);
        RegleMetierViolee.verifier(departementDuDemandeur.code().equals(demandeur.codeDepartement()),
                CodeRegle.CONGE_DECIDEUR_NON_HABILITE, "département fourni : " + departementDuDemandeur.code());
        boolean referentDuDepartement = valideur.aLeRole(Role.REFERENT)
                && departementDuDemandeur.aPourReferent(valideur.matricule());
        RegleMetierViolee.verifier(referentDuDepartement || valideur.aLeRole(Role.ADMINISTRATEUR),
                CodeRegle.CONGE_DECIDEUR_NON_HABILITE, valideur.nomComplet());
    }

    private List<Saisie> saisies(CalendrierOuvre calendrier) {
        return periode.decompte(calendrier).entrySet().stream()
                .map(e -> new Saisie(e.getKey(), projet, e.getValue(), motif))
                .toList();
    }

    /** Une référente ne voit que les demandes de son département ; un administrateur les voit toutes (écart SEC-015). */
    public boolean estVisiblePar(Collaborateur lecteur, Departement departementDuDemandeur) {
        return lecteur.matricule().equals(demandeur.matricule())
                || lecteur.aLeRole(Role.ADMINISTRATEUR)
                || (lecteur.aLeRole(Role.REFERENT) && departementDuDemandeur.code().equals(demandeur.codeDepartement())
                && departementDuDemandeur.aPourReferent(lecteur.matricule()));
    }

    public String id() {
        return id;
    }

    public Collaborateur demandeur() {
        return demandeur;
    }

    public Projet projet() {
        return projet;
    }

    public Optional<TypeAbsence> type() {
        return projet.typeAbsence();
    }

    public PeriodeConge periode() {
        return periode;
    }

    public String motif() {
        return motif;
    }

    /** Nombre de jours ouvrés calculé au dépôt (colonne {@code NB_JOURS}). */
    public Jours nombreDeJours() {
        return nombreDeJours;
    }

    public LocalDateTime dateDepot() {
        return dateDepot;
    }

    public EtatDemande etat() {
        return etat;
    }

    public Optional<Matricule> decideur() {
        return Optional.ofNullable(decideur);
    }

    public Optional<LocalDateTime> dateDecision() {
        return Optional.ofNullable(dateDecision);
    }

    public Optional<String> motifRefus() {
        return Optional.ofNullable(motifRefus);
    }

    @Override
    public String toString() {
        return "DemandeConge[" + demandeur.nomComplet() + ", " + projet.code() + ", " + periode + ", " + etat + "]";
    }
}
