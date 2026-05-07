import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository {
    Appointment save(Appointment appointment);

    Optional<Appointment> findById(UUID id);

    List<Appointment> findAll();

    List<Appointment> findByClientId(UUID clientId);

    List<Appointment> findByDateRange(LocalDate startInclusive, LocalDate endInclusive);

    boolean deleteById(UUID id);
}

