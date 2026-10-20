package farmac.dominio;

import java.time.LocalDateTime;

/**
 * Registra cada cambio en las existencias de un lote (RF08, RF09).
 * Tipos: RECEPCION, VENTA, AJUSTE, DEVOLUCION, CANCELACION.
 */
public class MovimientoInventario {
    private int id;
    private Lote lote;
    private String tipo;
    private int cantidad; // Con signo (+ entrada, - salida) o magnitud
    private String motivo;
    private String referenciaId; // Número de ticket, remisión o factura
    private Usuario usuario;
    private LocalDateTime fechaHora;

    public MovimientoInventario() {
        this.fechaHora = LocalDateTime.now();
    }

    public MovimientoInventario(int id, Lote lote, String tipo, int cantidad, String motivo,
                                String referenciaId, Usuario usuario, LocalDateTime fechaHora) {
        this.id = id;
        this.lote = lote;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.motivo = motivo;
        this.referenciaId = referenciaId;
        this.usuario = usuario;
        this.fechaHora = fechaHora != null ? fechaHora : LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Lote getLote() { return lote; }
    public void setLote(Lote lote) { this.lote = lote; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public String getReferenciaId() { return referenciaId; }
    public void setReferenciaId(String referenciaId) { this.referenciaId = referenciaId; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }

    @Override
    public String toString() {
        return "[" + tipo + "] " + cantidad + " pzas - " + motivo + " (" + fechaHora + ")";
    }
}
