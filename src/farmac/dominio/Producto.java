package farmac.dominio;

import java.time.LocalDate;
import java.util.List;

/**
 * Representa un producto o medicamento registrado en el catálogo (RF03, RF04, RF06).
 */
public class Producto {
    private int id;
    private String codigoBarras;
    private String nombre;
    private String categoria;
    private String proveedor; // Nombre o razón social para visualización rápida
    private Proveedor proveedorObj; // Relación con entidad Proveedor (RF07, RF20)
    private double precioVenta;
    private boolean requiereReceta;
    private int stockMinimo;
    private boolean activo; // Baja lógica (RF04)

    public Producto() {
        this.activo = true;
    }

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
        this.activo = true;
    }

    public Producto(int id, String codigoBarras, String nombre, String categoria, Proveedor proveedorObj,
                    double precioVenta, boolean requiereReceta, int stockMinimo, boolean activo) {
        this.id = id;
        this.codigoBarras = codigoBarras;
        this.nombre = nombre;
        this.categoria = categoria;
        this.proveedorObj = proveedorObj;
        this.proveedor = proveedorObj != null ? proveedorObj.getRazonSocial() : "";
        this.precioVenta = precioVenta;
        this.requiereReceta = requiereReceta;
        this.stockMinimo = stockMinimo;
        this.activo = activo;
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

    /**
     * Devuelve la fecha de caducidad más próxima entre los lotes vigentes (RF06).
     */
    public LocalDate obtenerCaducidadProxima(List<Lote> lotes) {
        if (lotes == null) return null;
        LocalDate hoy = LocalDate.now();
        LocalDate masProxima = null;
        for (Lote l : lotes) {
            if (l != null && l.getProducto() != null && l.getProducto().getId() == this.id
                    && l.getCantidadActual() > 0 && !l.estaVencido(hoy)) {
                if (masProxima == null || l.getFechaCaducidad().isBefore(masProxima)) {
                    masProxima = l.getFechaCaducidad();
                }
            }
        }
        return masProxima;
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

    public String getProveedor() {
        if (proveedorObj != null) return proveedorObj.getRazonSocial();
        return proveedor;
    }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }

    public Proveedor getProveedorObj() { return proveedorObj; }
    public void setProveedorObj(Proveedor proveedorObj) {
        this.proveedorObj = proveedorObj;
        if (proveedorObj != null) {
            this.proveedor = proveedorObj.getRazonSocial();
        }
    }

    public double getPrecioVenta() { return precioVenta; }
    public void setPrecioVenta(double precioVenta) { this.precioVenta = precioVenta; }

    public boolean isRequiereReceta() { return requiereReceta; }
    public void setRequiereReceta(boolean requiereReceta) { this.requiereReceta = requiereReceta; }

    public int getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(int stockMinimo) { this.stockMinimo = stockMinimo; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    @Override
    public String toString() {
        return nombre + " (" + codigoBarras + ") - $" + precioVenta;
    }
}
