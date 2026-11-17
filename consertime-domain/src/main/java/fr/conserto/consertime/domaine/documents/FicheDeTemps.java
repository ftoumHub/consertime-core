package fr.conserto.consertime.domaine.documents;

import fr.conserto.consertime.domaine.affaires.Projet;
import fr.conserto.consertime.domaine.calendrier.StatutJour;
import fr.conserto.consertime.domaine.personnel.Collaborateur;
import fr.conserto.consertime.domaine.personnel.Matricule;
import fr.conserto.consertime.domaine.regle.CodeRegle;
import fr.conserto.consertime.domaine.regle.RegleMetierViolee;
import fr.conserto.consertime.domaine.temps.ControleCompletude;
import fr.conserto.consertime.domaine.temps.FeuilleDeTemps;
import fr.conserto.consertime.domaine.temps.Jours;
import fr.conserto.consertime.domaine.temps.Saisie;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Feuille de temps (FDT) mensuelle, remise à Conserto : tous les projets, absences comprises.
 * <p>
 * Règles reprises de {@code ExportFdtHeader}, {@code ExportFdtBody} et {@code ExportFdtFooter} :
 * une colonne par jour du mois (libellé du jour, numéro, grisé si non travaillé), une ligne par projet avec son
 * total, une ligne « Total mois » ; en-tête avec nom, prénom, mois et matricule ; signatures consultant et
 * responsable Conserto.
 * <p>
 * Écart assumé : le legacy tronquait les totaux à l'entier ({@code intValue()}) et perdait les demi-journées.
 */
public record FicheDeTemps(
        String nom,
        String prenom,
        Matricule matricule,
        YearMonth mois,
        LocalDate dateEdition,
        List<Colonne> colonnes,
        List<Ligne> lignes,
        Jours totalMois) {

    public static final String TITRE = "FEUILLE DE TEMPS";

    public record Colonne(LocalDate date, StatutJour statut) {
    }

    public record Ligne(Projet projet, SortedMap<LocalDate, Jours> valeurs, Jours total) {
        public Ligne {
            valeurs = new TreeMap<>(valeurs);
        }

        public Jours valeurLe(LocalDate date) {
            return valeurs.getOrDefault(date, Jours.ZERO);
        }
    }

    public FicheDeTemps {
        colonnes = List.copyOf(colonnes);
        lignes = List.copyOf(lignes);
    }

    /**
     * @param projetsAffiches projets à afficher même sans saisie, en tête et dans cet ordre (le legacy affichait
     *                        les projets cochés dans l'écran d'export) ; les autres projets saisis suivent
     */
    public static FicheDeTemps etablir(FeuilleDeTemps feuille, Collection<Projet> projetsAffiches, boolean exigerCompletude,
                                       Clock clock) {
        if (exigerCompletude) {
            ControleCompletude.Resultat controle = ControleCompletude.verifier(feuille);
            RegleMetierViolee.verifier(controle.estComplet(), CodeRegle.EXPORT_SAISIE_INCOMPLETE,
                    String.join(" ", controle.messages()));
        }
        Map<String, Projet> projets = new LinkedHashMap<>();
        projetsAffiches.forEach(p -> projets.putIfAbsent(p.code(), p));
        feuille.projets().forEach(p -> projets.putIfAbsent(p.code(), p));

        List<Ligne> lignes = projets.values().stream().map(projet -> {
            SortedMap<LocalDate, Jours> valeurs = feuille.saisies().stream()
                    .filter(s -> s.projet().code().equals(projet.code()))
                    .collect(Collectors.toMap(Saisie::date, Saisie::jours, Jours::plus, TreeMap::new));
            Jours total = valeurs.values().stream().reduce(Jours.ZERO, Jours::plus);
            return new Ligne(projet, valeurs, total);
        }).collect(Collectors.toList());

        List<Colonne> colonnes = feuille.periode().jours()
                .map(jour -> new Colonne(jour, feuille.calendrier().statut(jour)))
                .collect(Collectors.toList());

        Collaborateur c = feuille.collaborateur();
        return new FicheDeTemps(c.nom(), c.prenom(), c.matricule(), feuille.mois(), LocalDate.now(clock),
                colonnes, lignes, feuille.total());
    }
}
