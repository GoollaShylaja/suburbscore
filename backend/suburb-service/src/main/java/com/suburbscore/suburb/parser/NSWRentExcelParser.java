package com.suburbscore.suburb.parser;

import com.suburbscore.suburb.enums.PropertyType;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses NSW DCJ (Dept of Communities & Justice) rental bond Excel file.
 * Download from: https://www.facs.nsw.gov.au/resources/statistics/rent-and-sales
 *
 * Expected columns: postcode, bedrooms, dwelling_type, median_rent_weekly
 */
@Slf4j
@Component
public class NSWRentExcelParser {

    @Value("${data.dir:./data}")
    private String dataDir;

    private static final String FILE_NAME = "nsw_rent.xlsx";

    public record RentRecord(String postcode, int bedrooms,
                             PropertyType propertyType, int medianRentWeekly) {}

    public List<RentRecord> parse() {
        Path filePath = Path.of(dataDir, FILE_NAME);
        if (!Files.exists(filePath)) {
            log.warn("NSW rent Excel file not found at {} — skipping rent update", filePath);
            return List.of();
        }

        List<RentRecord> records = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row header  = sheet.getRow(0);

            int postcodeCol  = findColumn(header, "postcode");
            int bedroomsCol  = findColumn(header, "bedrooms");
            int typeCol      = findColumn(header, "dwelling_type");
            int rentCol      = findColumn(header, "median_rent_weekly");

            if (postcodeCol < 0 || bedroomsCol < 0 || typeCol < 0 || rentCol < 0) {
                log.error("NSW rent Excel missing required columns");
                return List.of();
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                try {
                    String postcode  = getCellString(row, postcodeCol).trim();
                    int bedrooms     = (int) row.getCell(bedroomsCol).getNumericCellValue();
                    String typeRaw   = getCellString(row, typeCol).toUpperCase().trim();
                    int rent         = (int) row.getCell(rentCol).getNumericCellValue();

                    PropertyType type = mapDwellingType(typeRaw);
                    if (!postcode.isBlank() && bedrooms >= 0 && rent > 0) {
                        records.add(new RentRecord(postcode, bedrooms, type, rent));
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            log.error("Failed to parse NSW rent Excel: {}", e.getMessage());
            return List.of();
        }

        log.info("NSW rent Excel parsed — {} rent records loaded", records.size());
        return records;
    }

    private int findColumn(Row header, String name) {
        for (Cell cell : header) {
            if (cell.getStringCellValue().equalsIgnoreCase(name)) return cell.getColumnIndex();
        }
        return -1;
    }

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        return cell.getCellType() == CellType.NUMERIC
                ? String.valueOf((int) cell.getNumericCellValue())
                : cell.getStringCellValue();
    }

    private PropertyType mapDwellingType(String raw) {
        if (raw.contains("HOUSE") || raw.contains("DETACHED")) return PropertyType.HOUSE;
        if (raw.contains("APART") || raw.contains("FLAT"))     return PropertyType.APARTMENT;
        if (raw.contains("TOWN"))                               return PropertyType.TOWNHOUSE;
        if (raw.contains("UNIT"))                               return PropertyType.UNIT;
        if (raw.contains("STUDIO"))                             return PropertyType.STUDIO;
        return PropertyType.ANY;
    }
}
