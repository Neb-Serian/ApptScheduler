import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ClientServiceTest {

    @Test
    void updateClient_updatesStoredClientFields() {
        ClientService service = new ClientService(new InMemoryClientRepo());

        Client created = service.addClient(
                "John",
                "Doe",
                Client.Gender.FEMALE,
                LocalDate.of(1980, 12, 9),
                "john@example.com",
                "408-555-1212"
        );

        Client updated = service.updateClient(
                created.getId(),
                "John",
                "Doe-Smith",
                Client.Gender.FEMALE,
                LocalDate.of(1980, 12, 9),
                "JOHN@EXAMPLE.COM",
                "408-555-1212"
        );

        Client fetched = service.findById(created.getId()).orElseThrow();
        assertEquals(created.getId(), fetched.getId());
        assertEquals("Doe-Smith", fetched.getLastName());
        assertEquals("john@example.com", fetched.getEmail());
        assertEquals(updated.getEmail(), fetched.getEmail());
    }

    @Test
    void searchClients_returnsAllWhenBlankQuery() {
        ClientService service = new ClientService(new InMemoryClientRepo());
        service.addClient("John", "Doe", Client.Gender.OTHER, LocalDate.of(1990, 1, 1), "john@example.com", "408-555-1212");
        service.addClient("Hermione", "Granger", Client.Gender.FEMALE, LocalDate.of(1980, 12, 9), "hermione@example.com", "650-555-1212");

        assertEquals(2, service.searchClients("").size());
        assertEquals(2, service.searchClients("   ").size());
        assertEquals(2, service.searchClients(null).size());
    }

    @Test
    void searchClients_matchesByNameEmailOrPhone_caseInsensitive() {
        ClientService service = new ClientService(new InMemoryClientRepo());
        service.addClient("John", "Doe", Client.Gender.OTHER, LocalDate.of(1990, 1, 1), "john@example.com", "408-777-7777");
        service.addClient("Hermione", "Granger", Client.Gender.FEMALE, LocalDate.of(1980, 12, 9), "hermione@example.com", "650-555-1212");

        assertEquals(1, service.searchClients("john").size());
        assertEquals(1, service.searchClients("DOE").size());
        assertEquals(1, service.searchClients("JOHN@EXAMPLE.COM").size());
        assertEquals(1, service.searchClients("650-555").size());
    }

    @Test
    void deleteClient_deletesExistingClient() {
        ClientService service = new ClientService(new InMemoryClientRepo());

        Client created = service.addClient(
                "Hermione",
                "Granger",
                Client.Gender.FEMALE,
                LocalDate.of(1980, 12, 9),
                "hermione@example.com",
                "408-555-1212"
        );

        assertTrue(service.deleteClient(created.getId()));
        assertTrue(service.findById(created.getId()).isEmpty());
    }

    @Test
    void deleteClient_returnsFalseWhenMissing() {
        ClientService service = new ClientService(new InMemoryClientRepo());
        assertFalse(service.deleteClient(UUID.randomUUID()));
    }
}

