import java.util.Objects;
import java.util.UUID;
import java.time.LocalDate;

/**
 * Domain model for a client.
 *
 * Encapsulation: fields are private and validated at construction time.
 */
public final class Client {

    public enum Gender {
        MALE,
        FEMALE,
        OTHER
    }

    private final UUID id;
    private final String firstName;
    private final String lastName;
    private final Gender gender;
    private final LocalDate dateOfBirth;
    private final String email;
    private final String phone;

    public Client(
            UUID id,
            String firstName,
            String lastName,
            Gender gender,
            LocalDate dateOfBirth,
            String email,
            String phone
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.firstName = requireNonBlank(firstName, "firstName");
        this.lastName = requireNonBlank(lastName, "lastName");
        this.gender = Objects.requireNonNull(gender, "gender");
        this.dateOfBirth = Objects.requireNonNull(dateOfBirth, "dateOfBirth");
        this.email = normalizeEmail(requireNonBlank(email, "email"));
        this.phone = requireNonBlank(phone, "phone");

        if (this.dateOfBirth.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("dateOfBirth cannot be in the future");
        }
        if (!looksLikeEmail(this.email)) {
            throw new IllegalArgumentException("email must look like an email address (example: name@example.com)");
        }
        if (!looksLikeUsPhone(this.phone)) {
            throw new IllegalArgumentException("phone must be ###-###-####");
        }
    }

    public static Client createNew(
            String firstName,
            String lastName,
            Gender gender,
            LocalDate dateOfBirth,
            String email,
            String phone
    ) {
        return new Client(
                UUID.randomUUID(),
                firstName,
                lastName,
                gender,
                dateOfBirth,
                email,
                phone
        );
    }

    public UUID getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Gender getGender() {
        return gender;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getDisplayName() {
        return firstName + " " + lastName;
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static boolean looksLikeEmail(String value) {
        int at = value.indexOf('@');
        if (at <= 0 || at != value.lastIndexOf('@')) {
            return false;
        }
        int dot = value.indexOf('.', at + 2);
        return dot > at + 1 && dot < value.length() - 1;
    }

    private static String normalizeEmail(String value) {
        return value.trim().toLowerCase();
    }

    private static boolean looksLikeUsPhone(String value) {
        // Strict on purpose: forces consistent formatting and simplifies later search/display.
        return value.matches("\\d{3}-\\d{3}-\\d{4}");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Client client)) return false;
        return id.equals(client.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Client{" +
                "id=" + id +
                ", name='" + getDisplayName() + '\'' +
                '}';
    }
}

