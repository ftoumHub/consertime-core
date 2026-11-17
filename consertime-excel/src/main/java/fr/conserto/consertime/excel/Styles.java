package fr.conserto.consertime.excel;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * Styles partagés par les classeurs, créés une fois par classeur (Excel limite le nombre de styles).
 * Police Calibri 11 comme les exports du legacy ; gris clair pour les week-ends et jours fériés.
 */
final class Styles {

    final CellStyle titre;
    final CellStyle gras;
    final CellStyle texte;
    final CellStyle entete;
    final CellStyle cellule;
    final CellStyle celluleGrise;
    final CellStyle nombre;
    final CellStyle nombreGris;
    final CellStyle nombreGras;
    final CellStyle euro;
    final CellStyle euroGras;

    Styles(Workbook wb) {
        Font normale = police(wb, false, 11);
        Font forte = police(wb, true, 11);
        Font grande = police(wb, true, 16);

        titre = wb.createCellStyle();
        titre.setFont(grande);
        titre.setAlignment(HorizontalAlignment.CENTER);
        titre.setVerticalAlignment(VerticalAlignment.CENTER);

        gras = wb.createCellStyle();
        gras.setFont(forte);

        texte = wb.createCellStyle();
        texte.setFont(normale);

        entete = encadre(wb, forte);
        entete.setAlignment(HorizontalAlignment.CENTER);
        entete.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        entete.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        entete.setWrapText(true);

        cellule = encadre(wb, normale);
        celluleGrise = grise(encadre(wb, normale));

        short unChiffre = wb.createDataFormat().getFormat("0.0");
        nombre = encadre(wb, normale);
        nombre.setDataFormat(unChiffre);
        nombre.setAlignment(HorizontalAlignment.CENTER);
        nombreGris = grise(encadre(wb, normale));
        nombreGris.setDataFormat(unChiffre);
        nombreGris.setAlignment(HorizontalAlignment.CENTER);
        nombreGras = encadre(wb, forte);
        nombreGras.setDataFormat(unChiffre);
        nombreGras.setAlignment(HorizontalAlignment.CENTER);

        short formatEuro = wb.createDataFormat().getFormat("#,##0.00 \"€\"");
        euro = encadre(wb, normale);
        euro.setDataFormat(formatEuro);
        euroGras = encadre(wb, forte);
        euroGras.setDataFormat(formatEuro);
    }

    private static Font police(Workbook wb, boolean gras, int taille) {
        Font f = wb.createFont();
        f.setFontName("Calibri");
        f.setFontHeightInPoints((short) taille);
        f.setBold(gras);
        return f;
    }

    private static CellStyle encadre(Workbook wb, Font font) {
        CellStyle s = wb.createCellStyle();
        s.setFont(font);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        return s;
    }

    private static CellStyle grise(CellStyle s) {
        s.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }
}
