package farmac.dominio;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Gestor central de inventario, reglas FEFO y control de caducidades de la farmacia.
 */
public class OperacionesInventario {
    private List<Lote> inventario;
    private int diasAlertaCaducidad;

    public OperacionesInventario() {
        this.inventario = new ArrayList<>();
        this.diasAlertaCaducidad = 60; // 60 días por defecto
    }

    public OperacionesInventario(int diasAlertaCaducidad) {
        this.inventario = new ArrayList<>();
        this.diasAlertaCaducidad = diasAlertaCaducidad;
    }

    public void registrarEntrada(Lote nuevoLote) {
        if (nuevoLote != null) {
            this.inventario.add(nuevoLote);
        }
    }

    /**
     * Aplica el principio FEFO (First Expired, First Out) seleccionando los lotes vigentes
     * con fecha de caducidad más próxima.
     */
    public List<Lote> obtenerLotesFEFO(Producto producto, int cantidadRequerida) {
        List<Lote> candidatos = new ArrayList<>();
        LocalDate hoy = LocalDate.now();

        for (Lote l : inventario) {
            if (l.getProducto() != null && l.getProducto().getId() == producto.getId()
                    && !l.estaVencido(hoy) && l.getCantidadActual() > 0) {
                candidatos.add(l);
            }
        }

        // Ordenar por fecha de caducidad ascendente
        Collections.sort(candidatos, Comparator.comparing(Lote::getFechaCaducidad));
        return candidatos;
    }

    /**
     * Devuelve los lotes vencidos o próximos a vencer según el umbral configurado.
     */
    public List<Lote> obtenerLotesEnAlerta() {
        List<Lote> alertas = new ArrayList<>();
        LocalDate hoy = LocalDate.now();

        for (Lote l : inventario) {
            if (l.estaVencido(hoy) || l.diasParaVencer(hoy) <= diasAlertaCaducidad) {
                alertas.add(l);
            }
        }
        return alertas;
    }

    public boolean ajustarStockLote(Lote lote, int nuevaCantidad, String motivo) {
        if (lote == null || nuevaCantidad < 0 || motivo == null || motivo.trim().isEmpty()) {
            return false;
        }
        lote.setCantidadActual(nuevaCantidad);
        return true;
    }

    // Getters y Setters
    public List<Lote> getInventario() { return inventario; }
    public void setInventario(List<Lote> inventario) { this.inventario = inventario; }

    public int getDiasAlertaCaducidad() { return diasAlertaCaducidad; }
    public void setDiasAlertaCaducidad(int diasAlertaCaducidad) { this.diasAlertaCaducidad = diasAlertaCaducidad; }
}
