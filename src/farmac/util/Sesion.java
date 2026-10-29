package farmac.util;

import farmac.dao.TurnoCajaDAO;
import farmac.dominio.TurnoCaja;
import farmac.dominio.Usuario;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestor de sesión global y estado compartido de la aplicación (A3).
 * Mantiene el usuario activo y el turno de caja abierto para todas las pantallas.
 */
public class Sesion {

    private static Usuario usuarioActivo;
    private static TurnoCaja turnoActivo;
    private static final List<Runnable> listenersTurno = new ArrayList<>();

    public static synchronized Usuario getUsuarioActivo() {
        return usuarioActivo;
    }

    public static synchronized void setUsuarioActivo(Usuario u) {
        usuarioActivo = u;
    }

    public static synchronized TurnoCaja getTurnoActivo() {
        if (turnoActivo == null) {
            refrescarTurno();
        }
        return turnoActivo;
    }

    public static synchronized void setTurnoActivo(TurnoCaja turno) {
        turnoActivo = turno;
        notificarCambioTurno();
    }

    public static synchronized void refrescarTurno() {
        try {
            TurnoCajaDAO dao = new TurnoCajaDAO();
            turnoActivo = dao.obtenerTurnoAbierto();
        } catch (SQLException e) {
            // Sin conexión no debe conservarse un turno potencialmente obsoleto.
            turnoActivo = null;
            System.err.println("Error al refrescar turno de caja: " + e.getMessage());
        }
        notificarCambioTurno();
    }

    public static synchronized boolean hayTurnoAbierto() {
        TurnoCaja t = getTurnoActivo();
        return t != null && t.estaAbierto();
    }

    public static synchronized boolean esEncargado() {
        return usuarioActivo != null && "ENCARGADO".equalsIgnoreCase(usuarioActivo.obtenerRol());
    }

    public static synchronized void cerrarSesion() {
        usuarioActivo = null;
        turnoActivo = null;
        // Evita que pantallas de una sesión anterior sigan recibiendo notificaciones.
        listenersTurno.clear();
    }

    public static synchronized void agregarListenerTurno(Runnable r) {
        if (r != null && !listenersTurno.contains(r)) {
            listenersTurno.add(r);
        }
    }

    public static synchronized void removerListenerTurno(Runnable r) {
        listenersTurno.remove(r);
    }

    private static synchronized void notificarCambioTurno() {
        for (Runnable r : listenersTurno) {
            try {
                r.run();
            } catch (Exception e) {
                System.err.println("Error en listener de turno: " + e.getMessage());
            }
        }
    }
}
