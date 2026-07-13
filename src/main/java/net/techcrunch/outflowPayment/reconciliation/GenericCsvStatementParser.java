package net.techcrunch.outflowPayment.reconciliation;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class GenericCsvStatementParser {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Africa/Lagos");

    public StatementParseResult parse(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw new IllegalArgumentException("Statement file is empty");
            }
            List<String> headers = splitCsv(headerLine).stream().map(this::normalize).toList();
            List<ParsedStatementRow> rows = new ArrayList<>();
            List<RejectedStatementRow> rejectedRows = new ArrayList<>();
            String line;
            int rowNumber = 1;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank()) {
                    continue;
                }
                try {
                    List<String> values = splitCsv(line);
                    Map<String, Object> raw = rawPayload(headers, values);
                    BigDecimal amount = amount(value(raw, "amount", "transactionamount", "paidamount", "settledamount"));
                    String transactionReference = value(raw, "transactionreference", "paymentreference", "reference", "ref");
                    String externalReference = value(raw, "externalreference", "providerreference", "sessionid", "rrn", "bankreference");
                    if (amount == null || isBlank(transactionReference, externalReference)) {
                        throw new IllegalArgumentException("Row " + rowNumber + " must include amount and a reference");
                    }
                    rows.add(new ParsedStatementRow(
                            rowNumber,
                            transactionReference,
                            externalReference,
                            amount,
                            firstNonBlank(value(raw, "currency"), "NGN"),
                            direction(value(raw, "direction", "type", "entrytype")),
                            date(value(raw, "transactiondate", "createdat", "paidat", "date")),
                            date(value(raw, "valuedate", "settlementdate")),
                            value(raw, "status", "providerstatus", "paymentstatus"),
                            value(raw, "narration", "description", "remarks"),
                            raw
                    ));
                } catch (RuntimeException rowException) {
                    rejectedRows.add(new RejectedStatementRow(
                            rowNumber,
                            line,
                            rowException.getMessage(),
                            Map.of("rawLine", line)
                    ));
                }
            }
            return new StatementParseResult(rows, rejectedRows);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to parse statement CSV: " + exception.getMessage(), exception);
        }
    }

    private List<String> splitCsv(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                quoted = !quoted;
            } else if (character == ',' && !quoted) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }
        values.add(current.toString().trim());
        return values;
    }

    private Map<String, Object> rawPayload(List<String> headers, List<String> values) {
        Map<String, Object> raw = new LinkedHashMap<>();
        for (int index = 0; index < headers.size(); index++) {
            raw.put(headers.get(index), index < values.size() ? strip(values.get(index)) : "");
        }
        return raw;
    }

    private String normalize(String value) {
        return strip(value).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private String strip(String value) {
        if (value == null) {
            return "";
        }
        String stripped = value.trim();
        if (stripped.length() >= 2 && stripped.startsWith("\"") && stripped.endsWith("\"")) {
            return stripped.substring(1, stripped.length() - 1).trim();
        }
        return stripped;
    }

    private String value(Map<String, Object> raw, String... keys) {
        for (String key : keys) {
            Object value = raw.get(key);
            if (value != null && !value.toString().isBlank()) {
                return value.toString().trim();
            }
        }
        return null;
    }

    private BigDecimal amount(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return new BigDecimal(value.replace(",", "").replace("?", "").trim());
    }

    private ReconciliationStatementDirection direction(String value) {
        if (value == null) {
            return ReconciliationStatementDirection.UNKNOWN;
        }
        String normalized = value.toUpperCase(Locale.ROOT);
        if (normalized.contains("CR") || normalized.contains("CREDIT") || normalized.contains("INFLOW")) {
            return ReconciliationStatementDirection.CREDIT;
        }
        if (normalized.contains("DR") || normalized.contains("DEBIT") || normalized.contains("OUTFLOW")) {
            return ReconciliationStatementDirection.DEBIT;
        }
        return ReconciliationStatementDirection.UNKNOWN;
    }

    private OffsetDateTime date(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_OFFSET_DATE_TIME,
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy")
        );
        for (DateTimeFormatter formatter : formatters) {
            try {
                if (formatter == DateTimeFormatter.ISO_OFFSET_DATE_TIME) {
                    return OffsetDateTime.parse(value, formatter);
                }
                if (formatter == DateTimeFormatter.ISO_LOCAL_DATE_TIME) {
                    return LocalDateTime.parse(value, formatter).atZone(BUSINESS_ZONE).toOffsetDateTime();
                }
                return LocalDate.parse(value, formatter).atStartOfDay(BUSINESS_ZONE).toOffsetDateTime();
            } catch (RuntimeException ignored) {
                // Try the next supported format.
            }
        }
        throw new IllegalArgumentException("Unsupported date format: " + value);
    }

    private boolean isBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String firstNonBlank(String first, String fallback) {
        return first == null || first.isBlank() ? fallback : first;
    }
}
