package ru.mirea.project.ui;

import java.util.Arrays;
import java.util.List;

final class TablePrinter {
    private TablePrinter() {
    }

    static void print(String[] headers, List<String[]> rows) {
        int[] widths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            widths[i] = headers[i].length();
        }
        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                widths[i] = Math.max(widths[i], row[i].length());
            }
        }

        printRow(headers, widths);
        int totalWidth = Arrays.stream(widths).sum() + 3 * (headers.length - 1);
        System.out.println("-".repeat(totalWidth));
        if (rows.isEmpty()) {
            System.out.println("(записей нет)");
            return;
        }
        rows.forEach(row -> printRow(row, widths));
    }

    private static void printRow(String[] cells, int[] widths) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            line.append(String.format("%-" + widths[i] + "s", cells[i]));
            if (i < cells.length - 1) {
                line.append(" | ");
            }
        }
        System.out.println(line);
    }
}
