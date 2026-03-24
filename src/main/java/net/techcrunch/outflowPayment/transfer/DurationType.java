package net.techcrunch.outflowPayment.transfer;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

public enum DurationType {

    DAILY {
        @Override
        public LocalDate nextBillingDate(LocalDate from) {
            return from.plusDays(1);
        }
    },

    WEEKLY {
        @Override
        public LocalDate nextBillingDate(LocalDate from) {
            return from.plusWeeks(1);
        }
    },

    MONTHLY {
        @Override
        public LocalDate nextBillingDate(LocalDate from) {
            return from.plusMonths(1);
        }
    },

    YEARLY {
        @Override
        public LocalDate nextBillingDate(LocalDate from) {
            return from.plusYears(1);
        }
    };

    public abstract LocalDate nextBillingDate(LocalDate from);

    public static Optional<DurationType> fromValue(String value) {
        return value == null ? Optional.empty() :
                Arrays.stream(DurationType.values())
                        .filter(t -> t.name().equalsIgnoreCase(value))
                        .findFirst();
    }
}