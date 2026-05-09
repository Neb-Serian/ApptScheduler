import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class InMemoryAppointmentRepoTest {

    @Test
    void findByDateRange_includesEndpoints() {
        InMemoryAppointmentRepo repo = new InMemoryAppointmentRepo();
        UUID clientId = UUID.randomUUID();

        LocalDate d1 = LocalDate.now().plusDays(1);
        LocalDate d2 = d1.plusDays(1);
        LocalDate d3 = d1.plusDays(2);

        Appointment a1 = Appointment.createNew(clientId, d1, LocalTime.of(9, 0), LocalTime.of(10, 0), "A1", null);
        Appointment a2 = Appointment.createNew(clientId, d2, LocalTime.of(9, 0), LocalTime.of(10, 0), "A2", null);
        Appointment a3 = Appointment.createNew(clientId, d3, LocalTime.of(9, 0), LocalTime.of(10, 0), "A3", null);
        repo.save(a1);
        repo.save(a2);
        repo.save(a3);

        List<Appointment> got = repo.findByDateRange(d1, d2);
        assertEquals(2, got.size());
        assertTrue(got.stream().anyMatch(a -> a.getId().equals(a1.getId())));
        assertTrue(got.stream().anyMatch(a -> a.getId().equals(a2.getId())));
    }

    @Test
    void findByDateRange_returnsEmptyWhenEndBeforeStart() {
        InMemoryAppointmentRepo repo = new InMemoryAppointmentRepo();
        LocalDate d1 = LocalDate.now().plusDays(1);
        LocalDate d0 = d1.minusDays(1);
        assertTrue(repo.findByDateRange(d1, d0).isEmpty());
    }

    @Test
    void deleteById_returnsTrueWhenDeleted_falseWhenMissing() {
        InMemoryAppointmentRepo repo = new InMemoryAppointmentRepo();
        Appointment a = Appointment.createNew(UUID.randomUUID(), LocalDate.now().plusDays(1),
                LocalTime.of(9, 0), LocalTime.of(10, 0), "A", null);
        repo.save(a);

        assertTrue(repo.deleteById(a.getId()));
        assertFalse(repo.deleteById(a.getId()));
    }
}

