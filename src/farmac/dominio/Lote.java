package farmac.dominio;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Representa una partida física de un producto con caducidad específica (RF07, RF08).
 * Agregación con Producto y Proveedor.
 */
public class Lote {
    private int id;
    private String numeroLote;
    private LocalDate fechaCaducidad;
    private int cantidadRecibida;
    private int cantidadActual;
    private double precioCompra;
    private Producto producto;
    private Proveedor proveedor;
    private LocalDate fechaRecepcion;
    private String referenciaFactura;

    public Lote() {
        this.fechaRecepcion = LocalDate.now();
    }

    public Lote(int id, String numeroLote, LocalDate fechaCaducidad, int cantidadActual,
                double precioCompra, Producto producto) {
        this.id = id;
        this.numeroLote = numeroLote;
        this.fechaCaducidad = fechaCaducidad;
        this.cantidadRecibida = cantidadActual;
        this.cantidadActual = cantidadActual;
        this.precioCompra = precioCompra;
        this.producto = producto;
        this.fechaRecepcion = LocalDate.now();
    }

    public Lote(int id, String numeroLote, LocalDate fechaCaducidad, int cantidadRecibida, int cantidadActual,
                double precioCompra, Producto producto, Proveedor proveedor, LocalDate fechaRecepcion, String referenciaFactura) {
        this.id = id;
        this.numeroLote = numeroLote;
        this.fechaCaducidad = fechaCaducidad;
        this.cantidadRecibida = cantidadRecibida;
        this.cantidadActual = cantidadActual;
        this.precioCompra = precioCompra;
        this.producto = producto;
        this.proveedor = proveedor;
        this.fechaRecepcion = fechaRecepcion != null ? fechaRecepcion : LocalDate.now();
        this.referenciaFactura = referenciaFactura;
    }

    public boolean descontarStock(int cantidad) {
        if (cantidad <= 0 || this.cantidadActual < cantidad) {
            return false;
        }
        this.cantidadActual -= cantidad;
        return true;
    }

    public void incrementarStock(int cantidad) {
        if (cantidad > 0) {
            this.cantidadActual += cantidad;
        }
    }

    public boolean estaVencido(LocalDate fechaActual) {
        if (fechaCaducidad == null || fechaActual == null) return false;
        return fechaCaducidad.isBefore(fechaActual);
    }

    public long diasParaVencer(LocalDate fechaActual) {
        if (fechaCaducidad == null || fechaActual == null) return 0;
        return ChronoUnit.DAYS.between(fechaActual, fechaCaducidad);
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNumeroLote() { return numeroLote; }
    public void setNumeroLote(String numeroLote) { this.numeroLote = numeroLote; }

    public LocalDate getFechaCaducidad() { return fechaCaducidad; }
    public void setFechaCaducidad(LocalDate fechaCaducidad) { this.fechaCaducidad = fechaCaducidad; }

    public int getCantidadRecibida() { return cantidadRecibida; }
    public void setCantidadRecibida(int cantidadRecibida) { this.cantidadRecibida = cantidadRecibida; }

    public int getCantidadActual() { return cantidadActual; }
    public void setCantidadActual(int cantidadActual) { this.cantidadActual = cantidadActual; }

    public double getPrecioCompra() { return precioCompra; }
    public void setPrecioCompra(double precioCompra) { this.precioCompra = precioCompra; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }

    public Proveedor getProveedor() { return proveedor; }
    public void setProveedor(Proveedor proveedor) { this.proveedor = proveedor; }

    public LocalDate getFechaRecepcion() { return fechaRecepcion; }
    public void setFechaRecepcion(LocalDate fechaRecepcion) { this.fechaRecepcion = fechaRecepcion; }

    public String getReferenciaFactura() { return referenciaFactura; }
    public void setReferenciaFactura(String referenciaFactura) { this.referenciaFactura = referenciaFactura; }

    @Override
    public String toString() {
        return "Lote " + numeroLote + " (Cad: " + fechaCaducidad + ", Cant: " + cantidadActual + ")";
    }
}
