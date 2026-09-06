package cloud.adamind.saio.payments.model;

import cloud.adamind.saio.payments.util.Role;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class DatabaseUserModel {

    String cedula;
    String uid;
    String fechaCreacion;

    @Builder.Default
    String correo = "";

    @Builder.Default
    String rol = String.valueOf(Role.ASISTENTE);

    @Builder.Default
    String telefono = "";

    @Builder.Default
    Integer puntos = 0;

    @Builder.Default
    String nombre = "";

    @Builder.Default
    String boleta = "";


}
