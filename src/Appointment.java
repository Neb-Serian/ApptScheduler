import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

public final class Appointment {

    private final UUID id;
    private final UUID clientId;
    private final LocalDate date;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final String subject;
    private final String notes;

    public Appointment(UUID id, UUID clientId, LocalDate date, LocalTime startTime, LocalTime endTime, String subject, String notes) {
        this.id = Objects.requireNonNull(id, "id");
        this.clientId = Objects.requireNonNull(clientId, "clientId");
        this.date = Objects.requireNonNull(date, "date");
        this.startTime = Objects.requireNonNull(startTime, "startTime");
        this.endTime = Objects.requireNonNull(endTime, "endTime");
        this.subject = requireNonBlank(subject, "subject");
        this.notes = normalizeOptional(notes);

        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
    }

    public static Appointment createNew(UUID clientId, LocalDate date, LocalTime startTime, LocalTime endTime, String subject, String notes) {
        return new Appointment(UUID.randomUUID(), clientId, date, startTime, endTime, subject, notes);
    }

    public UUID getId() {
        return id;
    }

    public UUID getClientId() {
        return clientId;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public String getSubject() {
        return subject;
    }

    public String getNotes() {
        return notes;
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

