package farmac.dominio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Gestor central de inventario, reglas FEFO y control de caducidades de la farmacia.
 * Implementa RF07, RF08, RF09, RF10, RF11, RF18 y RF19.
 */
public class OperacionesInventario {
    private List<Lote> inventario;
    private int diasAlertaCaducidad;

    public static class DespachoLote {
        private Lote lote;
        private int cantidad;

        public DespachoLote(Lote lote, int cantidad) {
            this.lote = lote;
            this.cantidad = cantidad;
        }

        public Lote getLote() { return lote; }
        public int getCantidad() { return cantidad; }
    }

    public OperacionesInventario() {
        this.inventario = new ArrayList<>();
        this.diasAlertaCaducidad = 60; // 60 días por defecto (RF10, RF19)
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
     * con fecha de caducidad más próxima (excluye lotes caducados o sin stock).
     */
    public List<Lote> obtenerLotesFEFO(Producto producto, int cantidadRequerida) {
        List<Lote> candidatos = new ArrayList<>();
        if (producto == null) return candidatos;
        LocalDate hoy = LocalDate.now();

        for (Lote l : inventario) {
            if (l.getProducto() != null && l.getProducto().getId() == producto.getId()
                    && !l.estaVencido(hoy) && l.getCantidadActual() > 0) {
                candidatos.add(l);
            }
        }

        // Ordenar por fecha de caducidad ascendente (más próximo a caducar primero)
        Collections.sort(candidatos, Comparator.comparing(Lote::getFechaCaducidad));
        return candidatos;
    }

    /**
     * Calcula la asignación exacta de cantidades entre múltiples lotes bajo la regla FEFO.
     */
    public List<DespachoLote> calcularDespachoFEFO(Producto producto, int cantidadRequerida) {
        List<DespachoLote> despachos = new ArrayList<>();
        if (cantidadRequerida <= 0) return despachos;

        List<Lote> fefo = obtenerLotesFEFO(producto, cantidadRequerida);
        int restante = cantidadRequerida;

        for (Lote l : fefo) {
            if (restante <= 0) break;
            int tomar = Math.min(l.getCantidadActual(), restante);
            if (tomar > 0) {
                despachos.add(new DespachoLote(l, tomar));
                restante -= tomar;
            }
        }
        return despachos;
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

    /**
     * Devuelve los lotes vencidos o por vencer dentro de un rango de fechas configurable (RF19).
     */
    public List<Lote> lotesVencidosEnRango(LocalDate desde, LocalDate hasta) {
        List<Lote> resultado = new ArrayList<>();
        if (desde == null || hasta == null) return resultado;

        for (Lote l : inventario) {
            LocalDate cad = l.getFechaCaducidad();
            if (cad != null && !cad.isBefore(desde) && !cad.isAfter(hasta)) {
                resultado.add(l);
            }
        }
        return resultado;
    }

    /**
     * Identifica los productos cuyo stock consolidado es menor o igual al mínimo (RF11).
     */
    public List<Producto> productosBajoStock(List<Producto> productos) {
        List<Producto> bajos = new ArrayList<>();
        if (productos == null) return bajos;

        for (Producto p : productos) {
            if (p != null && p.isActivo() && p.esBajoStock(this.inventario)) {
                bajos.add(p);
            }
        }
        return bajos;
    }

    /**
     * Calcula la valorización total del inventario: sumatoria de (cantidadActual * precioCompra) (RF18).
     */
    public double valorTotalInventario() {
        double total = 0.0;
        for (Lote l : inventario) {
            if (l != null && l.getCantidadActual() > 0) {
                total += l.getCantidadActual() * l.getPrecioCompra();
            }
        }
        return Math.round(total * 100.0) / 100.0;
    }

    /**
     * Aplica un ajuste de inventario descontando existencias y generando el movimiento (RF09).
     */
    public MovimientoInventario ajustarStockLote(Lote lote, int cantidadADescontar, String motivo, Usuario usuario) {
        if (lote == null || cantidadADescontar <= 0 || motivo == null || motivo.trim().isEmpty()) {
            return null;
        }
        if (lote.descontarStock(cantidadADescontar)) {
            return new MovimientoInventario(
                    0, lote, "AJUSTE", -cantidadADescontar, motivo,
                    "AJUSTE-" + System.currentTimeMillis(), usuario, LocalDateTime.now()
            );
        }
        return null;
    }

    // Sobrecarga de compatibilidad anterior
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
