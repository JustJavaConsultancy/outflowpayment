package net.techcrunch.outflowPayment.transfer;

import java.time.LocalDate;

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
}