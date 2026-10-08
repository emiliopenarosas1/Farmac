package farmac.dominio;

import java.util.List;

/**
 * Representa un producto o medicamento registrado en el catálogo.
 */
public class Producto {
    private int id;
    private String codigoBarras;
    private String nombre;
    private String categoria;
    private String proveedor;
    private double precioVenta;
    private boolean requiereReceta;
    private int stockMinimo;

    public Producto() {}

    public Producto(int id, String codigoBarras, String nombre, String categoria, String proveedor,
                    double precioVenta, boolean requiereReceta, int stockMinimo) {
        this.id = id;
        this.codigoBarras = codigoBarras;
        this.nombre = nombre;
        this.categoria = categoria;
        this.proveedor = proveedor;
        this.precioVenta = precioVenta;
        this.requiereReceta = requiereReceta;
        this.stockMinimo = stockMinimo;
    }

    public int calcularStockTotal(List<Lote> lotes) {
        if (lotes == null) return 0;
        int total = 0;
        for (Lote lote : lotes) {
            if (lote != null && lote.getProducto() != null && lote.getProducto().getId() == this.id) {
                total += lote.getCantidadActual();
            }
        }
        return total;
    }

    public boolean esBajoStock(List<Lote> lotes) {
        return calcularStockTotal(lotes) <= stockMinimo;
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCodigoBarras() { return codigoBarras; }
    public void setCodigoBarras(String codigoBarras) { this.codigoBarras = codigoBarras; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }

    public double getPrecioVenta() { return precioVenta; }
    public void setPrecioVenta(double precioVenta) { this.precioVenta = precioVenta; }

    public boolean isRequiereReceta() { return requiereReceta; }
    public void setRequiereReceta(boolean requiereReceta) { this.requiereReceta = requiereReceta; }

    public int getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(int stockMinimo) { this.stockMinimo = stockMinimo; }

    @Override
    public String toString() {
        return nombre + " (" + codigoBarras + ") - $" + precioVenta;
    }
}
