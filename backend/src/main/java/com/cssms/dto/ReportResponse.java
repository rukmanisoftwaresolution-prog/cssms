package com.cssms.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ReportResponse(
        String title,
        String subtitle,
        List<Column> columns,
        List<Map<String, Object>> rows,
        List<Summary> summary) {

    public enum Type { TEXT, NUMBER, CURRENCY, DATE }

    public record Column(String key, String label, Type type) {
    }

    public record Summary(String label, Object value, Type type) {
    }

    public static Builder builder(String title, String subtitle) {
        return new Builder(title, subtitle);
    }

    /** Fluent builder: declare columns once, then add rows positionally. */
    public static final class Builder {
        private final String title;
        private final String subtitle;
        private final List<Column> columns = new ArrayList<>();
        private final List<Map<String, Object>> rows = new ArrayList<>();
        private final List<Summary> summary = new ArrayList<>();

        private Builder(String title, String subtitle) {
            this.title = title;
            this.subtitle = subtitle;
        }

        public Builder column(String key, String label, Type type) {
            columns.add(new Column(key, label, type));
            return this;
        }

        public Builder row(Object... values) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 0; i < columns.size(); i++) {
                row.put(columns.get(i).key(), i < values.length ? values[i] : null);
            }
            rows.add(row);
            return this;
        }

        public Builder summary(String label, Object value, Type type) {
            summary.add(new Summary(label, value, type));
            return this;
        }

        public ReportResponse build() {
            return new ReportResponse(title, subtitle, List.copyOf(columns), List.copyOf(rows), List.copyOf(summary));
        }
    }
}
