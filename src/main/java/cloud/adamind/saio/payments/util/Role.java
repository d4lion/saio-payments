package cloud.adamind.saio.payments.util;

public enum Role {
    ADMIN("admin"),
    ASISTENTE("asistente"),
    COORDINADOR("coordinador");

    private final String role;

    Role(String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }
}
