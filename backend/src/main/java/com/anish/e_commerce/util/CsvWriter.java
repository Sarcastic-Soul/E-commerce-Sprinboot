package com.anish.e_commerce.util;

import java.util.Arrays;
import java.util.List;

/** Minimal RFC 4180 writer: enough for the admin exports, no extra dependency. */
public final class CsvWriter {

    private final StringBuilder out = new StringBuilder();

    public CsvWriter row(Object... cells) {
        return row(Arrays.asList(cells)); // asList: cells may be null
    }

    public CsvWriter row(List<?> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) out.append(',');
            out.append(escape(cells.get(i)));
        }
        out.append("\r\n");
        return this;
    }

    @Override
    public String toString() {
        return out.toString();
    }

    static String escape(Object value) {
        if (value == null) return "";
        String s = value.toString();
        // Spreadsheet apps run cells starting with these as formulas; a product
        // name like "=HYPERLINK(...)" must come out as text.
        if (!(value instanceof Number) && !s.isEmpty() && "=+-@\t\r".indexOf(s.charAt(0)) >= 0) {
            s = "'" + s;
        }
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
