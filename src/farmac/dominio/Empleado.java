package farmac.dominio;

/**
 * Clase que representa a un Empleado/Cajero de mostrador.
 * Especialización de Usuario con permisos de cobro y consulta de inventario.
 */
public class Empleado extends Usuario {

    public Empleado() {
        super();
    }

    public Empleado(int id, String nombreUsuario, String nombreCompleto, String password, boolean activo) {
        super(id, nombreUsuario, nombreCompleto, password, activo);
    }

    @Override
    public String obtenerRol() {
        return "EMPLEADO";
    }

    public TurnoCaja iniciarTurno(double montoInicial) {
        return new TurnoCaja(0, this, montoInicial);
    }
}
