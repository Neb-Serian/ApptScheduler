import java.util.List;
import java.util.Objects;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ClientService {

    private final ClientRepository repository;

    public ClientService(ClientRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public Client addClient(
            String firstName,
            String lastName,
            Client.Gender gender,
            LocalDate dateOfBirth,
            String email,
            String phone
    ) {
        Client client = Client.createNew(
                firstName,
                lastName,
                gender,
                dateOfBirth,
                email,
                phone
        );
        repository.save(client);
        return client;
    }

    public List<Client> getAllClients() {
        return repository.findAll();
    }

    public List<Client> searchClients(String query) {
        String q = normalizeQuery(query);
        if (q == null) {
            return getAllClients();
        }
        return repository.findAll().stream()
                .filter(c -> matches(c, q))
                .collect(Collectors.toList());
    }

    public Client updateClient(
            UUID id,
            String firstName,
            String lastName,
            Client.Gender gender,
            LocalDate dateOfBirth,
            String email,
            String phone
    ) {
        Objects.requireNonNull(id, "id");
        Client updated = new Client(
                id,
                firstName,
                lastName,
                gender,
                dateOfBirth,
                email,
                phone
        );
        repository.save(updated);
        return updated;
    }

    public Optional<Client> findById(UUID id) {
        Objects.requireNonNull(id, "id");
        return repository.findById(id);
    }

    public boolean deleteClient(UUID id) {
        Objects.requireNonNull(id, "id");
        return repository.deleteById(id);
    }

    private static String normalizeQuery(String query) {
        if (query == null) {
            return null;
        }
        String t = query.trim().toLowerCase();
        return t.isEmpty() ? null : t;
    }

    private static boolean matches(Client c, String q) {
        return c.getFirstName().toLowerCase().contains(q)
                || c.getLastName().toLowerCase().contains(q)
                || c.getDisplayName().toLowerCase().contains(q)
                || c.getEmail().toLowerCase().contains(q)
                || c.getPhone().toLowerCase().contains(q);
    }
}

