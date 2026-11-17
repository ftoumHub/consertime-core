package fr.conserto.consertime.excel;

import fr.conserto.consertime.domaine.documents.CompteRenduActivite;
import fr.conserto.consertime.domaine.documents.FicheDeTemps;
import fr.conserto.consertime.domaine.documents.NoteDeFrais;
import fr.conserto.consertime.domaine.temps.Jours;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Représentation Excel (.xlsx) des documents du domaine. Adaptateur sans règle de gestion : tous les
 * calculs (jours facturés, totaux, indemnités) viennent du domaine ; ce module ne fait que les placer.
 * <p>
 * Mise en page inspirée des exports du legacy ({@code ExportCra*}, {@code ExportFdt*}, {@code ExportNdfSheet}).
 */
public final class ExportExcel {

    static final Locale FR = Locale.FRANCE;
    private static final DateTimeFormatter JJ_MM_AAAA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ExportExcel() {
    }

    // ------------------------------------------------------------------ CRA

    /** Feuille « CRA » : une ligne par jour (A : jour, B à G : imputations, H : jours facturés). */
    public static byte[] cra(CompteRenduActivite cra) {
        try (Workbook wb = new XSSFWorkbook()) {
            Styles st = new Styles(wb);
            Sheet s = wb.createSheet("CRA " + moisAnnee(cra.mois()));
            s.setColumnWidth(0, 14 * 256);
            for (int c = 1; c <= 6; c++) {
                s.setColumnWidth(c, 14 * 256);
            }
            s.setColumnWidth(7, 12 * 256);

            fusion(s, 0, 0, 0, 7, CompteRenduActivite.TITRE, st.titre);
            texte(s, 1, 0, "Date : " + cra.dateEdition().format(JJ_MM_AAAA), st.texte);
            fusion(s, 3, 3, 0, 3, "Nom : " + cra.nom(), st.cellule);
            fusion(s, 3, 3, 4, 7, "Prénom : " + cra.prenom(), st.cellule);
            fusion(s, 4, 4, 0, 3, "Client : " + cra.client().map(c -> c.nom()).orElse(""), st.cellule);
            fusion(s, 4, 4, 4, 7, "Mois/Année : " + moisAnnee(cra.mois()), st.cellule);

            int r = 6;
            texte(s, r, 0, "Jour", st.entete);
            fusion(s, r, r, 1, 6, "Imputation Projet", st.entete);
            texte(s, r, 7, "Jours", st.entete);
            for (CompteRenduActivite.Ligne ligne : cra.lignes()) {
                r++;
                boolean gris = ligne.statut().estNonTravaille();
                texte(s, r, 0, libelleJour(ligne.date()), gris ? st.celluleGrise : st.cellule);
                fusion(s, r, r, 1, 6, ligne.libelleImputations(), gris ? st.celluleGrise : st.cellule);
                nombreOuVide(s, r, 7, ligne.total(), gris ? st.nombreGris : st.nombre);
            }
            r += 2;
            texte(s, r, 0, "Nombre de jours facturés :", st.gras);
            nombre(s, r, 7, cra.joursFactures().valeur(), st.nombreGras);

            signatures(s, r + 2, st, 0, 3, "Consultant", 4, 7, "Responsable Client");
            return enOctets(wb);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ------------------------------------------------------------------ FDT

    /** Feuille « FDT » : A : projet, B : total, puis une colonne par jour du mois à partir de C. */
    public static byte[] fdt(FicheDeTemps fdt) {
        try (Workbook wb = new XSSFWorkbook()) {
            Styles st = new Styles(wb);
            Sheet s = wb.createSheet("FDT " + moisAnnee(fdt.mois()));
            int premiereColJour = 2;
            int derniereCol = premiereColJour + fdt.colonnes().size() - 1;
            s.setColumnWidth(0, 28 * 256);
            s.setColumnWidth(1, 9 * 256);
            for (int c = premiereColJour; c <= derniereCol; c++) {
                s.setColumnWidth(c, 5 * 256);
            }

            fusion(s, 0, 0, 0, derniereCol, FicheDeTemps.TITRE, st.titre);
            texte(s, 1, 0, "Date : " + fdt.dateEdition().format(JJ_MM_AAAA), st.texte);
            fusion(s, 3, 3, 0, 12, "Nom : " + fdt.nom(), st.cellule);
            fusion(s, 3, 3, 13, derniereCol, "Prénom : " + fdt.prenom(), st.cellule);
            fusion(s, 4, 4, 0, 12, "Mois/Année : " + moisAnnee(fdt.mois()), st.cellule);
            fusion(s, 4, 4, 13, derniereCol, "Matricule : " + fdt.matricule(), st.cellule);

            int rJour = 6;
            int rNumero = 7;
            texte(s, rNumero, 1, "Totaux", st.entete);
            for (int i = 0; i < fdt.colonnes().size(); i++) {
                FicheDeTemps.Colonne col = fdt.colonnes().get(i);
                boolean gris = col.statut().estNonTravaille();
                texte(s, rJour, premiereColJour + i, jourCourt(col.date()), gris ? st.celluleGrise : st.entete);
                nombreEntier(s, rNumero, premiereColJour + i, col.date().getDayOfMonth(), gris ? st.celluleGrise : st.entete);
            }

            int r = rNumero;
            for (FicheDeTemps.Ligne ligne : fdt.lignes()) {
                r++;
                texte(s, r, 0, ligne.projet().nom(), st.cellule);
                nombre(s, r, 1, ligne.total().valeur(), st.nombreGras);
                for (int i = 0; i < fdt.colonnes().size(); i++) {
                    FicheDeTemps.Colonne col = fdt.colonnes().get(i);
                    nombreOuVide(s, r, premiereColJour + i, ligne.valeurLe(col.date()),
                            col.statut().estNonTravaille() ? st.nombreGris : st.nombre);
                }
            }
            r++;
            texte(s, r, 0, "Total mois", st.entete);
            nombre(s, r, 1, fdt.totalMois().valeur(), st.nombreGras);

            s.createFreezePane(premiereColJour, rNumero + 1);
            signatures(s, r + 2, st, 0, 12, "Consultant", 13, derniereCol, "Responsable Conserto");
            return enOctets(wb);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ------------------------------------------------------------------ NDF

    /**
     * Feuille « NDF ». Les montants viennent du domaine ; les totaux sont des formules SUM, comme dans le legacy,
     * pour que la note reste modifiable dans Excel. Les valeurs calculées sont enregistrées dans le fichier.
     */
    public static byte[] ndf(NoteDeFrais ndf) {
        try (Workbook wb = new XSSFWorkbook()) {
            Styles st = new Styles(wb);
            Sheet s = wb.createSheet("NDF " + moisAnnee(ndf.mois()));
            int[] largeurs = {12, 36, 12, 16, 14, 12, 14};
            for (int c = 0; c < largeurs.length; c++) {
                s.setColumnWidth(c, largeurs[c] * 256);
            }

            fusion(s, 0, 0, 0, 6, NoteDeFrais.TITRE, st.titre);
            entete(s, 2, "Matricule", ndf.matricule(), st);
            entete(s, 3, "Nom", ndf.nomComplet(), st);
            texte(s, 3, 4, "Date de soumission", st.gras);
            texte(s, 3, 5, ndf.dateSoumission().format(JJ_MM_AAAA), st.texte);
            entete(s, 4, "Mission/client", ndf.missionClient(), st);
            texte(s, 4, 4, "Approuvée par", st.gras);
            texte(s, 4, 5, ndf.approuveePar().orElse(""), st.texte);
            entete(s, 5, "Service", ndf.service(), st);
            entete(s, 6, "Période", "Du " + ndf.periode().debut().format(JJ_MM_AAAA) + " au "
                    + ndf.periode().fin().format(JJ_MM_AAAA), st);
            entete(s, 7, "Taux kilométrique", ndf.bareme().euroParKm().toPlainString() + " €/km", st);

            int rEntete = 9;
            String[] colonnes = {"Date", "Description des frais", "Nombre de kilomètres",
                    "Montant de l'indemnité déplacement", "Montant TTC", "TVA", "Montant HT"};
            for (int c = 0; c < colonnes.length; c++) {
                texte(s, rEntete, c, colonnes[c], st.entete);
            }

            List<NoteDeFrais.Ligne> lignes = ndf.lignes();
            int r = rEntete;
            for (NoteDeFrais.Ligne l : lignes) {
                r++;
                texte(s, r, 0, l.date().format(JJ_MM_AAAA), st.cellule);
                texte(s, r, 1, l.description(), st.cellule);
                nombre(s, r, 2, l.kilometres(), st.nombre);
                nombre(s, r, 3, l.indemnite(ndf.bareme()), st.euro);
                nombre(s, r, 4, l.montantTtc(), st.euro);
                nombre(s, r, 5, l.tva(), st.euro);
                nombre(s, r, 6, l.montantHt(), st.euro);
            }
            // numéros de ligne Excel (base 1) de la première et de la dernière ligne de frais
            int premiere = rEntete + 2;
            int derniere = r + 1;
            int rTotaux = r + 1;
            texte(s, rTotaux, 1, "Totaux :", st.gras);
            for (int c = 2; c <= 6; c++) {
                String col = CellReference.convertNumToColString(c);
                formule(s, rTotaux, c, lignes.isEmpty() ? "0" : "SUM(" + col + premiere + ":" + col + derniere + ")",
                        c == 2 ? st.nombreGras : st.euroGras);
            }
            int ligneExcelTotaux = rTotaux + 1;
            texte(s, rTotaux + 2, 1, "Total à verser", st.gras);
            formule(s, rTotaux + 2, 4, "D" + ligneExcelTotaux + "+E" + ligneExcelTotaux, st.euroGras);

            wb.getCreationHelper().createFormulaEvaluator().evaluateAll();
            return enOctets(wb);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ------------------------------------------------------------------ outillage

    /** « Mar. 1 », comme {@code WebUtils.formatDate("E", date)} capitalisé dans le legacy. */
    static String libelleJour(LocalDate date) {
        return jourCourt(date) + " " + date.getDayOfMonth();
    }

    static String jourCourt(LocalDate date) {
        String court = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, FR);
        return Character.toUpperCase(court.charAt(0)) + court.substring(1);
    }

    static String moisAnnee(YearMonth mois) {
        return mois.getMonth().getDisplayName(TextStyle.FULL, FR) + " " + mois.getYear();
    }

    private static void entete(Sheet s, int r, String libelle, String valeur, Styles st) {
        texte(s, r, 0, libelle, st.gras);
        texte(s, r, 1, valeur, st.texte);
    }

    private static void signatures(Sheet s, int r, Styles st, int c1, int c2, String gauche, int c3, int c4, String droite) {
        texte(s, r, c1, "Signature", st.gras);
        fusion(s, r + 1, r + 1, c1, c2, gauche, st.entete);
        fusion(s, r + 1, r + 1, c3, c4, droite, st.entete);
        fusion(s, r + 2, r + 5, c1, c2, "", st.cellule);
        fusion(s, r + 2, r + 5, c3, c4, "", st.cellule);
    }

    private static Row ligne(Sheet s, int r) {
        Row row = s.getRow(r);
        return row != null ? row : s.createRow(r);
    }

    private static Cell cellule(Sheet s, int r, int c, CellStyle style) {
        Cell cell = ligne(s, r).createCell(c);
        cell.setCellStyle(style);
        return cell;
    }

    private static void texte(Sheet s, int r, int c, String valeur, CellStyle style) {
        cellule(s, r, c, style).setCellValue(valeur);
    }

    private static void nombre(Sheet s, int r, int c, BigDecimal valeur, CellStyle style) {
        cellule(s, r, c, style).setCellValue(valeur.doubleValue());
    }

    private static void nombreEntier(Sheet s, int r, int c, int valeur, CellStyle style) {
        cellule(s, r, c, style).setCellValue(valeur);
    }

    private static void nombreOuVide(Sheet s, int r, int c, Jours valeur, CellStyle style) {
        Cell cell = cellule(s, r, c, style);
        if (valeur.estPositif()) {
            cell.setCellValue(valeur.valeur().doubleValue());
        }
    }

    private static void formule(Sheet s, int r, int c, String formule, CellStyle style) {
        cellule(s, r, c, style).setCellFormula(formule);
    }

    /** Fusionne la plage et pose le style sur chaque cellule, pour que les bordures suivent toute la plage. */
    private static void fusion(Sheet s, int r1, int r2, int c1, int c2, String valeur, CellStyle style) {
        for (int r = r1; r <= r2; r++) {
            for (int c = c1; c <= c2; c++) {
                cellule(s, r, c, style);
            }
        }
        ligne(s, r1).getCell(c1).setCellValue(valeur);
        if (r1 != r2 || c1 != c2) {
            s.addMergedRegion(new CellRangeAddress(r1, r2, c1, c2));
        }
    }

    private static byte[] enOctets(Workbook wb) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        return out.toByteArray();
    }
}
