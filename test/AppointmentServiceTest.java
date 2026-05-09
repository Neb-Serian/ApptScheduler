import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class AppointmentServiceTest {

    @Test
    void add_andUpdate_persistsUpdatedAppointmentFields() {
        AppointmentService service = new AppointmentService(new InMemoryAppointmentRepo());
        LocalDate date = LocalDate.now().plusDays(1);
        UUID clientId = UUID.randomUUID();

        Appointment created = Appointment.createNew(clientId, date, LocalTime.of(9, 0), LocalTime.of(10, 0), "Initial", null);
        service.add(created);

        Appointment updated = new Appointment(
                created.getId(),
                created.getClientId(),
                created.getDate(),
                LocalTime.of(11, 0),
                LocalTime.of(12, 0),
                "Updated subject",
                "Notes"
        );
        service.update(updated);

        Appointment fetched = service.findById(created.getId()).orElseThrow();
        assertEquals(LocalTime.of(11, 0), fetched.getStartTime());
        assertEquals(LocalTime.of(12, 0), fetched.getEndTime());
        assertEquals("Updated subject", fetched.getSubject());
        assertEquals("Notes", fetched.getNotes());
    }

    @Test
    void add_rejectsOverlappingAppointments_sameDateDifferentClients() {
        AppointmentService service = new AppointmentService(new InMemoryAppointmentRepo());
        LocalDate date = LocalDate.now().plusDays(1);

        UUID c1 = UUID.randomUUID();
        UUID c2 = UUID.randomUUID();

        service.add(Appointment.createNew(c1, date, LocalTime.of(10, 0), LocalTime.of(11, 0), "A", null));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.add(Appointment.createNew(c2, date, LocalTime.of(10, 30), LocalTime.of(11, 30), "B", null))
        );
        assertTrue(ex.getMessage().toLowerCase().contains("conflict"));
    }

    @Test
    void add_allowsBackToBackAppointments() {
        AppointmentService service = new AppointmentService(new InMemoryAppointmentRepo());
        LocalDate date = LocalDate.now().plusDays(1);

        UUID c1 = UUID.randomUUID();
        UUID c2 = UUID.randomUUID();

        service.add(Appointment.createNew(c1, date, LocalTime.of(9, 0), LocalTime.of(10, 0), "A", null));
        assertDoesNotThrow(() ->
                service.add(Appointment.createNew(c2, date, LocalTime.of(10, 0), LocalTime.of(11, 0), "B", null))
        );
    }

    @Test
    void update_rejectsConflicts_ignoresSameAppointmentId() {
        AppointmentService service = new AppointmentService(new InMemoryAppointmentRepo());
        LocalDate date = LocalDate.now().plusDays(1);
        UUID c1 = UUID.randomUUID();
        UUID c2 = UUID.randomUUID();

        Appointment a1 = Appointment.createNew(c1, date, LocalTime.of(9, 0), LocalTime.of(10, 0), "A", null);
        Appointment a2 = Appointment.createNew(c2, date, LocalTime.of(10, 0), LocalTime.of(11, 0), "B", null);
        service.add(a1);
        service.add(a2);

        assertDoesNotThrow(() -> service.update(new Appointment(
                a1.getId(), a1.getClientId(), a1.getDate(), a1.getStartTime(), a1.getEndTime(), a1.getSubject(), a1.getNotes()
        )));

        assertThrows(IllegalArgumentException.class, () -> service.update(new Appointment(
                a1.getId(), a1.getClientId(), date, LocalTime.of(10, 30), LocalTime.of(11, 30), "A2", null
        )));
    }

    @Test
    void add_rejectsPastDate() {
        AppointmentService service = new AppointmentService(new InMemoryAppointmentRepo());
        LocalDate past = LocalDate.now().minusDays(1);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.add(Appointment.createNew(UUID.randomUUID(), past, LocalTime.of(10, 0), LocalTime.of(11, 0), "A", null))
        );
        assertTrue(ex.getMessage().toLowerCase().contains("past"));
    }

    @Test
    void add_rejectsStartTimeEarlierToday() {
        AppointmentService service = new AppointmentService(new InMemoryAppointmentRepo());
        LocalDate today = LocalDate.now();

        LocalTime start = LocalTime.now().minusMinutes(5);
        LocalTime end = start.plusMinutes(30);
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.add(Appointment.createNew(UUID.randomUUID(), today, start, end, "A", null))
        );
        assertTrue(ex.getMessage().toLowerCase().contains("past"));
    }
}

