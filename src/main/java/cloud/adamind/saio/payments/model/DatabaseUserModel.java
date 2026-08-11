package cloud.adamind.saio.payments.model;

import lombok.Builder;

@Builder
public record DatabaseUserModel(
        String cedula,
        String correo,
        String fechaCreacion,
        String nombre,
        Integer puntos,
        String rol,
        String telefono,
        String uid
) {
}
