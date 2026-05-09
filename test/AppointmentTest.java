import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class AppointmentTest {

    @Test
    void constructor_rejectsEndNotAfterStart() {
        UUID id = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(1);

        assertThrows(IllegalArgumentException.class, () -> new Appointment(
                id,
                clientId,
                date,
                LocalTime.of(10, 0),
                LocalTime.of(10, 0),
                "Subject",
                null
        ));

        assertThrows(IllegalArgumentException.class, () -> new Appointment(
                id,
                clientId,
                date,
                LocalTime.of(11, 0),
                LocalTime.of(10, 0),
                "Subject",
                null
        ));
    }

    @Test
    void constructor_rejectsBlankSubject() {
        assertThrows(IllegalArgumentException.class, () -> new Appointment(
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                "   ",
                null
        ));
    }

    @Test
    void constructor_trimsSubject_andNormalizesNotes() {
        Appointment a = new Appointment(
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                "  Subject  ",
                "   "
        );
        assertEquals("Subject", a.getSubject());
        assertNull(a.getNotes());
    }
}

