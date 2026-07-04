package com.vidyapeet.exam;

import com.vidyapeet.common.exception.Exceptions;
import com.vidyapeet.exam.dto.QuestionRequest;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Parses an Excel (.xlsx/.xls) workbook of questions into {@link QuestionRequest}s.
 *
 * <p>Expected columns (row 1 is a header and is skipped):
 * <pre>
 * A: Question | B: Type | C: Option A | D: Option B | E: Option C | F: Option D | G: Correct | H: Marks
 * </pre>
 * Type is one of MCQ, MSQ, TRUE_FALSE, FILL_BLANK (blank defaults to MCQ).
 * The Correct column means: MCQ -> "B"; MSQ -> "A,C"; TRUE_FALSE -> "TRUE"/"FALSE";
 * FILL_BLANK -> accepted answers separated by "|" (e.g. "newton|newtons").
 */
@Component
public class QuestionExcelImporter {

    private static final DataFormatter FORMATTER = new DataFormatter();

    public List<QuestionRequest> parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw Exceptions.badRequest("An Excel file is required.");
        }
        List<QuestionRequest> questions = new ArrayList<>();
        try (InputStream in = file.getInputStream(); Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw Exceptions.badRequest("The workbook has no sheets.");
            }
            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isBlankRow(row)) {
                    continue;
                }
                questions.add(parseRow(row, r + 1));
            }
        } catch (IOException e) {
            throw Exceptions.badRequest("Could not read the Excel file. Ensure it is a valid .xlsx/.xls file.");
        }

        if (questions.isEmpty()) {
            throw Exceptions.badRequest("No question rows were found in the file.");
        }
        return questions;
    }

    private QuestionRequest parseRow(Row row, int rowNumber) {
        String text = cell(row, 0);
        QuestionType type = parseType(cell(row, 1), rowNumber);
        String optionA = cell(row, 2);
        String optionB = cell(row, 3);
        String optionC = cell(row, 4);
        String optionD = cell(row, 5);
        String correct = cell(row, 6);
        int marks = parseMarks(cell(row, 7), rowNumber);

        if (!StringUtils.hasText(text)) {
            throw Exceptions.badRequest("Row " + rowNumber + ": question text is required.");
        }
        if (!StringUtils.hasText(correct)) {
            throw Exceptions.badRequest("Row " + rowNumber + ": the Correct column is required.");
        }

        return switch (type) {
            case MCQ -> {
                requireOptions(optionA, optionB, optionC, optionD, rowNumber);
                yield new QuestionRequest(type, text.trim(),
                        optionA.trim(), optionB.trim(), optionC.trim(), optionD.trim(),
                        parseOption(correct, rowNumber), null, null, null, marks);
            }
            case MSQ -> {
                requireOptions(optionA, optionB, optionC, optionD, rowNumber);
                List<AnswerOption> opts = Arrays.stream(correct.split(","))
                        .map(s -> parseOption(s, rowNumber)).toList();
                yield new QuestionRequest(type, text.trim(),
                        optionA.trim(), optionB.trim(), optionC.trim(), optionD.trim(),
                        null, opts, null, null, marks);
            }
            case TRUE_FALSE -> {
                String c = correct.trim().toUpperCase();
                if (!c.equals("TRUE") && !c.equals("FALSE")) {
                    throw Exceptions.badRequest("Row " + rowNumber + ": Correct must be TRUE or FALSE.");
                }
                yield new QuestionRequest(type, text.trim(), null, null, null, null,
                        null, null, c.equals("TRUE"), null, marks);
            }
            case FILL_BLANK -> {
                List<String> accepted = Arrays.stream(correct.split("\\|"))
                        .map(String::trim).filter(s -> !s.isEmpty()).toList();
                yield new QuestionRequest(type, text.trim(), null, null, null, null,
                        null, null, null, accepted, marks);
            }
        };
    }

    private QuestionType parseType(String raw, int rowNumber) {
        if (!StringUtils.hasText(raw)) {
            return QuestionType.MCQ;
        }
        String t = raw.trim().toUpperCase().replaceAll("[^A-Z]", "");
        return switch (t) {
            case "MCQ" -> QuestionType.MCQ;
            case "MSQ" -> QuestionType.MSQ;
            case "TF", "TRUEFALSE" -> QuestionType.TRUE_FALSE;
            case "FILL", "FILLBLANK", "FILLINTHEBLANK", "FILLINTHEBLANKS" -> QuestionType.FILL_BLANK;
            default -> throw Exceptions.badRequest(
                    "Row " + rowNumber + ": unknown type '" + raw + "'. Use MCQ, MSQ, TRUE_FALSE or FILL_BLANK.");
        };
    }

    private AnswerOption parseOption(String raw, int rowNumber) {
        try {
            return AnswerOption.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw Exceptions.badRequest("Row " + rowNumber + ": correct option must be among A, B, C, D.");
        }
    }

    private void requireOptions(String a, String b, String c, String d, int rowNumber) {
        if (!StringUtils.hasText(a) || !StringUtils.hasText(b) || !StringUtils.hasText(c) || !StringUtils.hasText(d)) {
            throw Exceptions.badRequest("Row " + rowNumber + ": all four options are required for MCQ/MSQ.");
        }
    }

    private int parseMarks(String raw, int rowNumber) {
        int marks;
        try {
            marks = StringUtils.hasText(raw) ? (int) Math.round(Double.parseDouble(raw.trim())) : 1;
        } catch (NumberFormatException e) {
            throw Exceptions.badRequest("Row " + rowNumber + ": marks must be a number.");
        }
        if (marks < 1) {
            throw Exceptions.badRequest("Row " + rowNumber + ": marks must be at least 1.");
        }
        return marks;
    }

    private String cell(Row row, int index) {
        Cell cell = row.getCell(index);
        return cell == null ? "" : FORMATTER.formatCellValue(cell);
    }

    private boolean isBlankRow(Row row) {
        for (int c = 0; c <= 7; c++) {
            if (StringUtils.hasText(cell(row, c))) {
                return false;
            }
        }
        return true;
    }
}
