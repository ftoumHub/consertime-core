package fr.conserto.consertime.domaine.documents;

import fr.conserto.consertime.domaine.calendrier.Periode;
import fr.conserto.consertime.domaine.personnel.Collaborateur;
import fr.conserto.consertime.domaine.regle.CodeRegle;
import fr.conserto.consertime.domaine.regle.RegleMetierViolee;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Note de frais (NDF) mensuelle.
 * <p>
 * Structure reprise de {@code ExportNdfSheet} : matricule, nom, date de soumission, mission/client, approbateur,
 * service, période ; colonnes date, description, kilomètres, indemnité kilométrique, montant TTC, TVA, montant HT ;
 * total à verser = indemnités + montants TTC.
 * <p>
 * Le legacy ne produisait qu'un modèle vide (une ligne par jour, montants à saisir dans Excel).
 * Règles complétées : saisie des lignes dans le domaine, indemnité = kilomètres × taux, HT = TTC − TVA,
 * montants positifs, TVA au plus égale au TTC, date dans le mois.
 */
public final class NoteDeFrais {

    public static final String TITRE = "Note de frais";

    /** Taux kilométrique en euros par kilomètre (barème fiscal ou taux interne, selon la politique Conserto). */
    public record BaremeKilometrique(BigDecimal euroParKm) {
        public BaremeKilometrique {
            Objects.requireNonNull(euroParKm, "euroParKm");
        }

        public static BaremeKilometrique de(String euroParKm) {
            return new BaremeKilometrique(new BigDecimal(euroParKm));
        }
    }

    public record Ligne(LocalDate date, String description, BigDecimal kilometres, BigDecimal montantTtc, BigDecimal tva) {

        public Ligne {
            Objects.requireNonNull(date, "date");
            kilometres = Objects.requireNonNullElse(kilometres, BigDecimal.ZERO);
            montantTtc = Objects.requireNonNullElse(montantTtc, BigDecimal.ZERO);
            tva = Objects.requireNonNullElse(tva, BigDecimal.ZERO);
            RegleMetierViolee.verifier(kilometres.signum() >= 0 && montantTtc.signum() >= 0 && tva.signum() >= 0,
                    CodeRegle.NDF_LIGNE_INVALIDE, "montant négatif le " + date);
            RegleMetierViolee.verifier(tva.compareTo(montantTtc) <= 0, CodeRegle.NDF_LIGNE_INVALIDE,
                    "TVA supérieure au TTC le " + date);
        }

        public static Ligne deplacement(LocalDate date, String description, String kilometres) {
            return new Ligne(date, description, new BigDecimal(kilometres), BigDecimal.ZERO, BigDecimal.ZERO);
        }

        public static Ligne depense(LocalDate date, String description, String montantTtc, String tva) {
            return new Ligne(date, description, BigDecimal.ZERO, new BigDecimal(montantTtc), new BigDecimal(tva));
        }

        public BigDecimal indemnite(BaremeKilometrique bareme) {
            return kilometres.multiply(bareme.euroParKm()).setScale(2, RoundingMode.HALF_UP);
        }

        public BigDecimal montantHt() {
            return montantTtc.subtract(tva);
        }
    }

    public record Totaux(BigDecimal kilometres, BigDecimal indemnites, BigDecimal montantTtc, BigDecimal tva,
                         BigDecimal montantHt) {
        public BigDecimal totalAVerser() {
            return indemnites.add(montantTtc);
        }
    }

    private final String matricule;
    private final String nomComplet;
    private final String missionClient;
    private final String service;
    private final YearMonth mois;
    private final LocalDate dateSoumission;
    private final BaremeKilometrique bareme;
    private final List<Ligne> lignes = new ArrayList<>();
    private String approuveePar;

    private NoteDeFrais(Collaborateur collaborateur, YearMonth mois, String missionClient, String service,
                        BaremeKilometrique bareme, LocalDate dateSoumission) {
        this.matricule = collaborateur.matricule().toString();
        this.nomComplet = collaborateur.nomComplet();
        this.missionClient = missionClient;
        this.service = service;
        this.mois = mois;
        this.bareme = bareme;
        this.dateSoumission = dateSoumission;
    }

    public static NoteDeFrais ouvrir(Collaborateur collaborateur, YearMonth mois, String missionClient, String service,
                                     BaremeKilometrique bareme, Clock clock) {
        return new NoteDeFrais(collaborateur, mois, missionClient, service, bareme, LocalDate.now(clock));
    }

    public NoteDeFrais ajouter(Ligne ligne) {
        RegleMetierViolee.verifier(YearMonth.from(ligne.date()).equals(mois), CodeRegle.NDF_LIGNE_INVALIDE,
                ligne.date() + " hors de " + mois);
        lignes.add(ligne);
        return this;
    }

    public NoteDeFrais approuver(Collaborateur approbateur) {
        this.approuveePar = approbateur.nomComplet();
        return this;
    }

    public List<Ligne> lignes() {
        return lignes.stream().sorted(Comparator.comparing(Ligne::date)).toList();
    }

    public Totaux totaux() {
        return new Totaux(
                somme(Ligne::kilometres),
                somme(l -> l.indemnite(bareme)),
                somme(Ligne::montantTtc),
                somme(Ligne::tva),
                somme(Ligne::montantHt));
    }

    private BigDecimal somme(Function<Ligne, BigDecimal> champ) {
        return lignes.stream().map(champ).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public String matricule() {
        return matricule;
    }

    public String nomComplet() {
        return nomComplet;
    }

    public String missionClient() {
        return missionClient;
    }

    public String service() {
        return service;
    }

    public YearMonth mois() {
        return mois;
    }

    public Periode periode() {
        return Periode.mois(mois);
    }

    public LocalDate dateSoumission() {
        return dateSoumission;
    }

    public BaremeKilometrique bareme() {
        return bareme;
    }

    public Optional<String> approuveePar() {
        return Optional.ofNullable(approuveePar);
    }
}
