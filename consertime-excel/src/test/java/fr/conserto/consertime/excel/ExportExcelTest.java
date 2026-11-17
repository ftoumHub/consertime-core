package fr.conserto.consertime.excel;

import fr.conserto.consertime.domaine.affaires.Affectation;
import fr.conserto.consertime.domaine.affaires.Client;
import fr.conserto.consertime.domaine.affaires.Projet;
import fr.conserto.consertime.domaine.affaires.TypeAbsence;
import fr.conserto.consertime.domaine.calendrier.CalendrierOuvre;
import fr.conserto.consertime.domaine.calendrier.Periode;
import fr.conserto.consertime.domaine.documents.CompteRenduActivite;
import fr.conserto.consertime.domaine.documents.FicheDeTemps;
import fr.conserto.consertime.domaine.documents.NoteDeFrais;
import fr.conserto.consertime.domaine.personnel.Collaborateur;
import fr.conserto.consertime.domaine.temps.FeuilleDeTemps;
import fr.conserto.consertime.domaine.temps.Jours;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Représentation Excel des documents")
class ExportExcelTest {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final Clock EDITION = Clock.fixed(LocalDate.of(2025, 5, 2).atTime(9, 0).atZone(PARIS).toInstant(), PARIS);
    private static final CalendrierOuvre CALENDRIER = CalendrierOuvre.france();
    private static final Projet EXTRANET = Projet.mission("ACME-EXT", "Refonte extranet", new Client("ACME", "ACME Assurances"));

    private final Collaborateur alice = Collaborateur.builder().matricule(1002).nom("Alice", "Martin")
            .entreLe(LocalDate.of(2020, 1, 6)).departement("DIG").build();
    private FeuilleDeTemps avril;

    @BeforeEach
    void avril() {
        avril = new FeuilleDeTemps(alice, YearMonth.of(2025, 4), CALENDRIER);
        Affectation extranet = Affectation.simple(alice.matricule(), EXTRANET);
        Affectation cp = Affectation.simple(alice.matricule(), TypeAbsence.CONGES_PAYES.projet());
        for (LocalDate jour : CALENDRIER.joursOuvres(Periode.mois(YearMonth.of(2025, 4)))) {
            boolean conge = !jour.isBefore(LocalDate.of(2025, 4, 22)) && !jour.isAfter(LocalDate.of(2025, 4, 25));
            avril.saisir(conge ? cp : extranet, jour, Jours.UN);
        }
    }

    private static Workbook lire(byte[] xlsx) throws IOException {
        return new XSSFWorkbook(new ByteArrayInputStream(xlsx));
    }

    private static String texte(Sheet s, int r, int c) {
        return s.getRow(r).getCell(c).getStringCellValue();
    }

    @Test
    @DisplayName("CRA : en-tête, une ligne par jour, jours grisés, total facturé")
    void cra() throws IOException {
        CompteRenduActivite cra = CompteRenduActivite.etablir(avril, true, EDITION);
        try (Workbook wb = lire(ExportExcel.cra(cra))) {
            Sheet s = wb.getSheetAt(0);
            assertThat(s.getSheetName()).isEqualTo("CRA avril 2025");
            assertThat(texte(s, 0, 0)).isEqualTo("COMPTE RENDU D'ACTIVITE");
            assertThat(texte(s, 3, 0)).isEqualTo("Nom : Martin");
            assertThat(texte(s, 4, 0)).isEqualTo("Client : ACME Assurances");
            assertThat(texte(s, 7, 0)).isEqualTo("Mar. 1");
            assertThat(texte(s, 7, 1)).isEqualTo("(1.0) - Refonte extranet");
            // lundi de Pâques, 21 avril : ligne 7 + 20, grisée, sans imputation
            Row paques = s.getRow(27);
            assertThat(paques.getCell(0).getStringCellValue()).isEqualTo("Lun. 21");
            assertThat(paques.getCell(0).getCellStyle().getFillPattern()).isEqualTo(FillPatternType.SOLID_FOREGROUND);
            assertThat(paques.getCell(1).getStringCellValue()).isEqualTo("");
            // congés payés : pas d'imputation sur le CRA client
            assertThat(texte(s, 28, 1)).isEqualTo("");
            Row total = s.getRow(7 + 30 + 1);
            assertThat(total.getCell(0).getStringCellValue()).isEqualTo("Nombre de jours facturés :");
            assertThat(total.getCell(7).getNumericCellValue()).isEqualTo(17.0);
        }
    }

    @Test
    @DisplayName("FDT : une colonne par jour, une ligne par projet, total du mois")
    void fdt() throws IOException {
        FicheDeTemps fdt = FicheDeTemps.etablir(avril, List.of(), true, EDITION);
        try (Workbook wb = lire(ExportExcel.fdt(fdt))) {
            Sheet s = wb.getSheetAt(0);
            assertThat(texte(s, 0, 0)).isEqualTo("FEUILLE DE TEMPS");
            assertThat(texte(s, 4, 13)).isEqualTo("Matricule : 1002");
            assertThat(texte(s, 6, 2)).isEqualTo("Mar.");
            assertThat(s.getRow(7).getCell(31).getNumericCellValue()).isEqualTo(30.0);
            assertThat(texte(s, 8, 0)).isEqualTo("Refonte extranet");
            assertThat(s.getRow(8).getCell(1).getNumericCellValue()).isEqualTo(17.0);
            assertThat(texte(s, 9, 0)).isEqualTo("Congés payés");
            assertThat(s.getRow(9).getCell(1).getNumericCellValue()).isEqualTo(4.0);
            assertThat(s.getRow(9).getCell(2 + 21).getNumericCellValue()).isEqualTo(1.0);
            assertThat(texte(s, 10, 0)).isEqualTo("Total mois");
            assertThat(s.getRow(10).getCell(1).getNumericCellValue()).isEqualTo(21.0);
        }
    }

    @Test
    @DisplayName("NDF : lignes, totaux en formules SUM avec valeurs calculées, total à verser")
    void ndf() throws IOException {
        NoteDeFrais ndf = NoteDeFrais.ouvrir(alice, YearMonth.of(2025, 4), "ACME Assurances", "Digital Nantes",
                        NoteDeFrais.BaremeKilometrique.de("0.603"), EDITION)
                .ajouter(NoteDeFrais.Ligne.deplacement(LocalDate.of(2025, 4, 3), "Nantes - site client", "42"))
                .ajouter(NoteDeFrais.Ligne.depense(LocalDate.of(2025, 4, 10), "Train Nantes - Paris", "120.00", "10.91"));
        try (Workbook wb = lire(ExportExcel.ndf(ndf))) {
            Sheet s = wb.getSheetAt(0);
            assertThat(texte(s, 0, 0)).isEqualTo("Note de frais");
            assertThat(texte(s, 2, 1)).isEqualTo("1002");
            assertThat(texte(s, 10, 0)).isEqualTo("03/04/2025");
            assertThat(s.getRow(10).getCell(3).getNumericCellValue()).isEqualTo(25.33);
            Row totaux = s.getRow(12);
            assertThat(totaux.getCell(3).getCellType()).isEqualTo(CellType.FORMULA);
            assertThat(totaux.getCell(3).getCellFormula()).isEqualTo("SUM(D11:D12)");
            assertThat(totaux.getCell(3).getNumericCellValue()).isEqualTo(25.33);
            assertThat(s.getRow(14).getCell(4).getCellFormula()).isEqualTo("D13+E13");
            // 25.33 + 120.00 en double ne vaut pas exactement 145.33 : comparaison au centime
            assertThat(Math.round(s.getRow(14).getCell(4).getNumericCellValue() * 100)).isEqualTo(14533L);
        }
    }

    @Test
    void libellesFrancais() {
        assertThat(ExportExcel.libelleJour(LocalDate.of(2025, 5, 29))).isEqualTo("Jeu. 29");
        assertThat(ExportExcel.moisAnnee(YearMonth.of(2025, 8))).isEqualTo("août 2025");
    }
}
