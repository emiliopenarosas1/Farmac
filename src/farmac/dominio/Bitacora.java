package farmac.dominio;

import java.time.LocalDateTime;

/**
 * Registro de auditoría y bitácora del sistema (A7).
 * Registra login, ajustes de inventario, cancelaciones de venta y cortes de caja.
 */
public class Bitacora {
    private int id;
    private Usuario usuario;
    private String accion;
    private String descripcion;
    private LocalDateTime fechaHora;
    private String ip;

    public Bitacora() {
        this.fechaHora = LocalDateTime.now();
    }

    public Bitacora(int id, Usuario usuario, String accion, String descripcion, LocalDateTime fechaHora, String ip) {
        this.id = id;
        this.usuario = usuario;
        this.accion = accion;
        this.descripcion = descripcion;
        this.fechaHora = fechaHora != null ? fechaHora : LocalDateTime.now();
        this.ip = ip;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    @Override
    public String toString() {
        return "[" + fechaHora + "] " + accion + ": " + descripcion + " (Usuario: " + (usuario != null ? usuario.getNombreUsuario() : "N/A") + ")";
    }
}
