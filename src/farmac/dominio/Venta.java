package farmac.dominio;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa una venta o transacción realizada en la farmacia (RF13, RF14, RF15).
 * Mantiene composición fuerte con DetalleVenta y soporte de múltiples RecetaMedica.
 */
public class Venta {
    private int id;
    private LocalDateTime fechaHora;
    private double total;
    private String formaPago; // "EFECTIVO", "TARJETA", "TRANSFERENCIA"
    private String estado;    // "COMPLETADA", "CANCELADA"
    private String motivoCancelacion;
    private TurnoCaja turnoCaja;
    private Usuario usuario;
    private List<DetalleVenta> detalles;
    private List<RecetaMedica> recetas; // Soporta múltiples productos controlados (RF14)

    public Venta() {
        this.detalles = new ArrayList<>();
        this.recetas = new ArrayList<>();
        this.fechaHora = LocalDateTime.now();
        this.estado = "COMPLETADA";
    }

    public Venta(int id, TurnoCaja turnoCaja, Usuario usuario, String formaPago) {
        this.id = id;
        this.turnoCaja = turnoCaja;
        this.usuario = usuario;
        this.formaPago = formaPago;
        this.fechaHora = LocalDateTime.now();
        this.estado = "COMPLETADA";
        this.detalles = new ArrayList<>();
        this.recetas = new ArrayList<>();
    }

    public DetalleVenta agregarDetalle(Lote lote, int cantidad, double precioUnitario) {
        DetalleVenta detalle = new DetalleVenta(this.detalles.size() + 1, lote, cantidad, precioUnitario);
        this.detalles.add(detalle);
        calcularTotal();
        return detalle;
    }

    public void agregarReceta(RecetaMedica receta) {
        if (receta != null) {
            this.recetas.add(receta);
        }
    }

    public void asignarReceta(RecetaMedica receta) {
        this.recetas.clear();
        if (receta != null) {
            this.recetas.add(receta);
        }
    }

    public double calcularTotal() {
        double suma = 0.0;
        for (DetalleVenta d : detalles) {
            if (d != null) {
                suma += d.getSubtotal();
            }
        }
        this.total = Math.round(suma * 100.0) / 100.0;
        return this.total;
    }

    /**
     * Cancela la venta reintegrando existencias a los lotes.
     * Valida que la venta no esté previamente cancelada y que el turno siga abierto (RF15).
     */
    public boolean cancelar(String motivo) {
        if ("CANCELADA".equalsIgnoreCase(this.estado)) {
            return false;
        }
        if (this.turnoCaja != null && !this.turnoCaja.estaAbierto()) {
            return false; // No se puede cancelar si el turno ya fue cerrado (RF15)
        }
        this.estado = "CANCELADA";
        this.motivoCancelacion = motivo;
        for (DetalleVenta d : detalles) {
            if (d != null && d.getLote() != null) {
                d.getLote().incrementarStock(d.getCantidad());
            }
        }
        return true;
    }

    // Getters y Setters completos
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public String getFormaPago() { return formaPago; }
    public void setFormaPago(String formaPago) { this.formaPago = formaPago; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getMotivoCancelacion() { return motivoCancelacion; }
    public void setMotivoCancelacion(String motivoCancelacion) { this.motivoCancelacion = motivoCancelacion; }

    public TurnoCaja getTurnoCaja() { return turnoCaja; }
    public void setTurnoCaja(TurnoCaja turnoCaja) { this.turnoCaja = turnoCaja; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public List<DetalleVenta> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleVenta> detalles) {
        this.detalles = detalles;
        calcularTotal();
    }

    public List<RecetaMedica> getRecetas() { return recetas; }
    public void setRecetas(List<RecetaMedica> recetas) {
        this.recetas = recetas != null ? recetas : new ArrayList<>();
    }

    /**
     * Compatibilidad hacia atrás con getReceta()
     */
    public RecetaMedica getReceta() {
        if (recetas != null && !recetas.isEmpty()) {
            return recetas.get(0);
        }
        return null;
    }

    @Override
    public String toString() {
        return "Venta #" + id + " - Total: $" + total + " [" + estado + "]";
    }
}
