package farmac.dominio;

/**
 * Clase que representa al Encargado/Administrador de la farmacia.
 * Especialización de Usuario con permisos de administración, ajustes de stock y cortes.
 */
public class Encargado extends Usuario {

    public Encargado() {
        super();
    }

    public Encargado(int id, String nombreUsuario, String nombreCompleto, String password, boolean activo) {
        super(id, nombreUsuario, nombreCompleto, password, activo);
    }

    @Override
    public String obtenerRol() {
        return "ENCARGADO";
    }

    public boolean autorizarAjuste(Lote lote, int cantidad, String motivo) {
        if (lote == null || motivo == null || motivo.trim().isEmpty()) {
            return false;
        }
        return lote.descontarStock(cantidad);
    }

    public double realizarCorte(TurnoCaja turno, double efectivoContado, double ventasEfectivo) {
        if (turno == null) return 0.0;
        return turno.cerrarTurno(efectivoContado, ventasEfectivo);
    }
}
