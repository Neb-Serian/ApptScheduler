import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ClientTest {

    @Test
    void constructor_normalizesEmailToLowercaseTrimmed() {
        Client c = new Client(
                UUID.randomUUID(),
                "Hermione",
                "Granger",
                Client.Gender.OTHER,
                LocalDate.of(1990, 1, 1),
                "  Hermione@Example.COM ",
                "408-555-1212"
        );
        assertEquals("hermione@example.com", c.getEmail());
    }

    @Test
    void constructor_rejectsInvalidEmail() {
        assertThrows(IllegalArgumentException.class, () -> new Client(
                UUID.randomUUID(),
                "John",
                "Doe",
                Client.Gender.OTHER,
                LocalDate.of(1990, 1, 1),
                "not-an-email",
                "408-777-7777"
        ));
    }

    @Test
    void constructor_rejectsInvalidPhone() {
        assertThrows(IllegalArgumentException.class, () -> new Client(
                UUID.randomUUID(),
                "Hermione",
                "Granger",
                Client.Gender.OTHER,
                LocalDate.of(1990, 1, 1),
                "hermione@example.com",
                "4067777777"
        ));
    }

    @Test
    void constructor_rejectsFutureDob() {
        assertThrows(IllegalArgumentException.class, () -> new Client(
                UUID.randomUUID(),
                "John",
                "Doe",
                Client.Gender.OTHER,
                LocalDate.now().plusDays(1),
                "john@example.com",
                "408-777-7777"
        ));
    }
}

