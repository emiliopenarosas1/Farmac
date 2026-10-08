package farmac.dominio;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa una venta o transacción realizada en la farmacia.
 * Mantiene composición fuerte con DetalleVenta.
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
    private RecetaMedica receta;

    public Venta() {
        this.detalles = new ArrayList<>();
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
    }

    public DetalleVenta agregarDetalle(Lote lote, int cantidad, double precioUnitario) {
        DetalleVenta detalle = new DetalleVenta(this.detalles.size() + 1, lote, cantidad, precioUnitario);
        this.detalles.add(detalle);
        calcularTotal();
        return detalle;
    }

    public void asignarReceta(RecetaMedica receta) {
        this.receta = receta;
    }

    public double calcularTotal() {
        double suma = 0.0;
        for (DetalleVenta d : detalles) {
            if (d != null) {
                suma += d.getSubtotal();
            }
        }
        this.total = suma;
        return this.total;
    }

    public boolean cancelar(String motivo) {
        if (this.estado != null && this.estado.equals("CANCELADA")) {
            return false;
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

    public RecetaMedica getReceta() { return receta; }
    public void setReceta(RecetaMedica receta) { this.receta = receta; }

    @Override
    public String toString() {
        return "Venta #" + id + " - Total: $" + total + " [" + estado + "]";
    }
}
