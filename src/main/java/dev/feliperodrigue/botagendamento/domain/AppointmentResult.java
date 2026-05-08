package dev.feliperodrigue.botagendamento.domain;

import java.util.List;

public record AppointmentResult(
        boolean available,
        List<String> slots,
        String errorMessage
) {

    public static AppointmentResult noSlots() {
        return new AppointmentResult(false, List.of(), null);
    }

    public static AppointmentResult withSlots(List<String> slots) {
        return new AppointmentResult(true, slots, null);
    }

    public static AppointmentResult error(String message) {
        return new AppointmentResult(false, List.of(), message);
    }

    public boolean hasError() {
        return errorMessage != null;
    }
}
