import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public final class AppointmentService {

    private final AppointmentRepository repository;

    public AppointmentService(AppointmentRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public Appointment add(Appointment appointment) {
        Objects.requireNonNull(appointment, "appointment");
        ensureNoConflict(appointment, null);
        repository.save(appointment);
        return appointment;
    }

    public Appointment update(Appointment appointment) {
        Objects.requireNonNull(appointment, "appointment");
        ensureNoConflict(appointment, appointment.getId());
        repository.save(appointment);
        return appointment;
    }

    public boolean delete(UUID id) {
        Objects.requireNonNull(id, "id");
        return repository.deleteById(id);
    }

    public Optional<Appointment> findById(UUID id) {
        Objects.requireNonNull(id, "id");
        return repository.findById(id);
    }

    public List<Appointment> getAll() {
        return sorted(repository.findAll());
    }

    public List<Appointment> getForClient(UUID clientId) {
        Objects.requireNonNull(clientId, "clientId");
        return sorted(repository.findByClientId(clientId));
    }

    public List<Appointment> getForWeek(LocalDate anyDayInWeek) {
        Objects.requireNonNull(anyDayInWeek, "anyDayInWeek");
        LocalDate start = startOfWeek(anyDayInWeek);
        LocalDate end = start.plusDays(6);
        return sorted(repository.findByDateRange(start, end));
    }

    /**
     * Appointments in the given calendar week, but only on or after {@code todayInclusive}
     * (so past days in the same week are omitted).
     */
    public List<Appointment> getUpcomingForWeek(LocalDate anyDayInWeek, LocalDate todayInclusive) {
        Objects.requireNonNull(anyDayInWeek, "anyDayInWeek");
        Objects.requireNonNull(todayInclusive, "todayInclusive");
        LocalDate weekStart = startOfWeek(anyDayInWeek);
        LocalDate weekEnd = weekStart.plusDays(6);
        LocalDate effStart = weekStart.isBefore(todayInclusive) ? todayInclusive : weekStart;
        return sorted(filterFromDate(repository.findByDateRange(effStart, weekEnd), todayInclusive));
    }

    /**
     * All appointments on or after today (inclusive).
     */
    public List<Appointment> getFromTodayOnwards(LocalDate todayInclusive) {
        Objects.requireNonNull(todayInclusive, "todayInclusive");
        LocalDate farEnd = todayInclusive.plusYears(50);
        return sorted(filterFromDate(repository.findByDateRange(todayInclusive, farEnd), todayInclusive));
    }

    public static LocalDate startOfWeek(LocalDate d) {
        DayOfWeek dow = d.getDayOfWeek();
        int shift = (dow.getValue() + 6) % 7; // Monday=0
        return d.minusDays(shift);
    }

    private static List<Appointment> sorted(List<Appointment> in) {
        in.sort(Comparator.comparing(Appointment::getDate).thenComparing(Appointment::getStartTime));
        return in;
    }

    private static List<Appointment> filterFromDate(List<Appointment> in, LocalDate minDateInclusive) {
        return in.stream()
                .filter(a -> !a.getDate().isBefore(minDateInclusive))
                .collect(Collectors.toList());
    }

    private void ensureNoConflict(Appointment candidate, UUID ignoreAppointmentId) {
        // No past appointments (date in past, or earlier today).
        LocalDate today = LocalDate.now();
        if (candidate.getDate().isBefore(today)) {
            throw new IllegalArgumentException("Appointment date cannot be in the past");
        }
        if (candidate.getDate().isEqual(today) && candidate.getStartTime().isBefore(LocalTime.now())) {
            throw new IllegalArgumentException("Appointment start time cannot be in the past");
        }

        // Conflict checking: no overlapping appointments on the same date, for any client.
        List<Appointment> existing = repository.findAll();
        for (Appointment other : existing) {
            if (ignoreAppointmentId != null && ignoreAppointmentId.equals(other.getId())) {
                continue;
            }
            if (!other.getDate().equals(candidate.getDate())) {
                continue;
            }
            if (overlaps(candidate.getStartTime(), candidate.getEndTime(), other.getStartTime(), other.getEndTime())) {
                throw new IllegalArgumentException(
                        "Appointment time conflicts with an existing appointment (" +
                                other.getStartTime() + "-" + other.getEndTime() + ")"
                );
            }
        }
    }

    private static boolean overlaps(LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) {
        return aStart.isBefore(bEnd) && aEnd.isAfter(bStart);
    }
}

