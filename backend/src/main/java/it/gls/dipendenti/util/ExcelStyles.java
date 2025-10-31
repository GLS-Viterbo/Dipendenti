package it.gls.dipendenti.util;

import org.apache.poi.ss.usermodel.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class per gestire gli stili delle celle Excel
 */
public class ExcelStyles {

    private final Map<String, CellStyle> styleCache = new HashMap<>();

    /**
     * Stile per testo in grassetto
     */
    public CellStyle getBoldStyle(Workbook workbook) {
        return styleCache.computeIfAbsent("bold", k -> {
            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            font.setFontName("Courier New");
            font.setFontHeightInPoints((short) 11);
            style.setFont(font);
            style.setAlignment(HorizontalAlignment.CENTER);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            return style;
        });
    }

    /**
     * Stile per testo centrato con font Courier
     */
    public CellStyle getCenterStyle(Workbook workbook) {
        return styleCache.computeIfAbsent("center", k -> {
            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setFontName("Courier New");
            font.setFontHeightInPoints((short) 11);
            style.setFont(font);
            style.setAlignment(HorizontalAlignment.CENTER);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            return style;
        });
    }

    /**
     * Stile per numeri decimali (es. ore lavorate)
     */
    public CellStyle getDecimalStyle(Workbook workbook) {
        return styleCache.computeIfAbsent("decimal", k -> {
            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setFontName("Courier New");
            font.setFontHeightInPoints((short) 11);

            DataFormat format = workbook.createDataFormat();
            style.setDataFormat(format.getFormat("0.0"));

            style.setFont(font);
            style.setAlignment(HorizontalAlignment.CENTER);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            return style;
        });
    }

    /**
     * Stile per valori positivi (verde chiaro)
     */
    public CellStyle getPositiveStyle(Workbook workbook) {
        return styleCache.computeIfAbsent("positive", k -> {
            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setFontName("Courier New");
            font.setFontHeightInPoints((short) 11);
            font.setColor(IndexedColors.DARK_GREEN.getIndex());

            DataFormat format = workbook.createDataFormat();
            style.setDataFormat(format.getFormat("0.0"));

            style.setFont(font);
            style.setAlignment(HorizontalAlignment.CENTER);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            return style;
        });
    }

    /**
     * Stile per valori negativi (rosso chiaro)
     */
    public CellStyle getNegativeStyle(Workbook workbook) {
        return styleCache.computeIfAbsent("negative", k -> {
            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setFontName("Courier New");
            font.setFontHeightInPoints((short) 11);
            font.setColor(IndexedColors.DARK_RED.getIndex());

            DataFormat format = workbook.createDataFormat();
            style.setDataFormat(format.getFormat("0.0"));

            style.setFont(font);
            style.setAlignment(HorizontalAlignment.CENTER);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            style.setFillForegroundColor(IndexedColors.ROSE.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            return style;
        });
    }

    /**
     * Stile per header delle tabelle (grassetto + sfondo grigio)
     */
    public CellStyle getHeaderStyle(Workbook workbook) {
        return styleCache.computeIfAbsent("header", k -> {
            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            font.setFontName("Courier New");
            font.setFontHeightInPoints((short) 11);
            font.setColor(IndexedColors.WHITE.getIndex());

            style.setFont(font);
            style.setAlignment(HorizontalAlignment.CENTER);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            style.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Bordi
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);

            return style;
        });
    }

    /**
     * Stile per date
     */
    public CellStyle getDateStyle(Workbook workbook) {
        return styleCache.computeIfAbsent("date", k -> {
            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setFontName("Courier New");
            font.setFontHeightInPoints((short) 11);

            DataFormat format = workbook.createDataFormat();
            style.setDataFormat(format.getFormat("dd/mm/yyyy"));

            style.setFont(font);
            style.setAlignment(HorizontalAlignment.CENTER);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            return style;
        });
    }

    /**
     * Stile per orari
     */
    public CellStyle getTimeStyle(Workbook workbook) {
        return styleCache.computeIfAbsent("time", k -> {
            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setFontName("Courier New");
            font.setFontHeightInPoints((short) 11);

            DataFormat format = workbook.createDataFormat();
            style.setDataFormat(format.getFormat("hh:mm"));

            style.setFont(font);
            style.setAlignment(HorizontalAlignment.CENTER);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            return style;
        });
    }

    /**
     * Stile per celle di warning/attenzione (giallo)
     */
    public CellStyle getWarningStyle(Workbook workbook) {
        return styleCache.computeIfAbsent("warning", k -> {
            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setFontName("Courier New");
            font.setFontHeightInPoints((short) 11);
            font.setBold(true);

            style.setFont(font);
            style.setAlignment(HorizontalAlignment.CENTER);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            return style;
        });
    }

    /**
     * Stile per celle di errore (rosso scuro)
     */
    public CellStyle getErrorStyle(Workbook workbook) {
        return styleCache.computeIfAbsent("error", k -> {
            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setFontName("Courier New");
            font.setFontHeightInPoints((short) 11);
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());

            style.setFont(font);
            style.setAlignment(HorizontalAlignment.CENTER);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            style.setFillForegroundColor(IndexedColors.RED.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            return style;
        });
    }

    /**
     * Pulisce la cache degli stili
     */
    public void clearCache() {
        styleCache.clear();
    }
}