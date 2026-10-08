package farmac.dominio;

/**
 * Representa un renglón individual dentro de una venta.
 * Parte de la composición de la clase Venta.
 */
public class DetalleVenta {
    private int id;
    private int cantidad;
    private double precioUnitario;
    private double subtotal;
    private Lote lote;

    public DetalleVenta() {}

    public DetalleVenta(int id, Lote lote, int cantidad, double precioUnitario) {
        this.id = id;
        this.lote = lote;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.subtotal = calcularSubtotal();
    }

    public double calcularSubtotal() {
        return this.cantidad * this.precioUnitario;
    }

    // Getters y Setters completos
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
        this.subtotal = calcularSubtotal();
    }

    public double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(double precioUnitario) {
        this.precioUnitario = precioUnitario;
        this.subtotal = calcularSubtotal();
    }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public Lote getLote() { return lote; }
    public void setLote(Lote lote) { this.lote = lote; }

    @Override
    public String toString() {
        return (lote != null && lote.getProducto() != null ? lote.getProducto().getNombre() : "Item")
                + " x" + cantidad + " = $" + subtotal;
    }
}
