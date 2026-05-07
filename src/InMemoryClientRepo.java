import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryClientRepo implements ClientRepository {

    private final LinkedHashMap<UUID, Client> clientsById = new LinkedHashMap<>();

    @Override
    public Client save(Client client) {
        Objects.requireNonNull(client, "client");
        clientsById.put(client.getId(), client);
        return client;
    }

    @Override
    public Optional<Client> findById(UUID id) {
        Objects.requireNonNull(id, "id");
        return Optional.ofNullable(clientsById.get(id));
    }

    @Override
    public List<Client> findAll() {
        return new ArrayList<>(clientsById.values());
    }

    @Override
    public boolean deleteById(UUID id) {
        Objects.requireNonNull(id, "id");
        return clientsById.remove(id) != null;
    }
}

