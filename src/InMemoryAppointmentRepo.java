import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryAppointmentRepo implements AppointmentRepository {

    private final LinkedHashMap<UUID, Appointment> byId = new LinkedHashMap<>();

    @Override
    public Appointment save(Appointment appointment) {
        Objects.requireNonNull(appointment, "appointment");
        byId.put(appointment.getId(), appointment);
        return appointment;
    }

    @Override
    public Optional<Appointment> findById(UUID id) {
        Objects.requireNonNull(id, "id");
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public List<Appointment> findAll() {
        return new ArrayList<>(byId.values());
    }

    @Override
    public List<Appointment> findByClientId(UUID clientId) {
        Objects.requireNonNull(clientId, "clientId");
        List<Appointment> out = new ArrayList<>();
        for (Appointment a : byId.values()) {
            if (a.getClientId().equals(clientId)) {
                out.add(a);
            }
        }
        return out;
    }

    @Override
    public List<Appointment> findByDateRange(LocalDate startInclusive, LocalDate endInclusive) {
        Objects.requireNonNull(startInclusive, "startInclusive");
        Objects.requireNonNull(endInclusive, "endInclusive");
        if (endInclusive.isBefore(startInclusive)) {
            return List.of();
        }

        List<Appointment> out = new ArrayList<>();
        for (Appointment a : byId.values()) {
            LocalDate d = a.getDate();
            if ((d.isAfter(startInclusive) || d.isEqual(startInclusive))
                    && (d.isBefore(endInclusive) || d.isEqual(endInclusive))) {
                out.add(a);
            }
        }
        return out;
    }

    @Override
    public boolean deleteById(UUID id) {
        Objects.requireNonNull(id, "id");
        return byId.remove(id) != null;
    }
}

