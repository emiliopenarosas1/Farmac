package farmac.dominio;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa un turno de caja abierto por un usuario para transacciones financieras (RF12, RF16).
 */
public class TurnoCaja {
    private int id;
    private LocalDateTime fechaApertura;
    private double montoInicial;
    private LocalDateTime fechaCierre;
    private double efectivoContado;
    private double montoEsperado;
    private double diferencia;
    private String estado; // "ABIERTO", "CERRADO"
    private Usuario usuario;
    private List<Venta> ventas;

    public TurnoCaja() {
        this.estado = "ABIERTO";
        this.fechaApertura = LocalDateTime.now();
        this.ventas = new ArrayList<>();
    }

    public TurnoCaja(int id, Usuario usuario, double montoInicial) {
        this.id = id;
        this.usuario = usuario;
        this.montoInicial = montoInicial;
        this.fechaApertura = LocalDateTime.now();
        this.estado = "ABIERTO";
        this.ventas = new ArrayList<>();
    }

    public boolean estaAbierto() {
        return "ABIERTO".equalsIgnoreCase(this.estado);
    }

    public void agregarVenta(Venta v) {
        if (v != null) {
            this.ventas.add(v);
        }
    }

    /**
     * Total de ventas completadas (no canceladas) en una forma de pago específica (RF16).
     */
    public double totalPorFormaPago(String formaPago) {
        if (formaPago == null || ventas == null) return 0.0;
        double sum = 0.0;
        for (Venta v : ventas) {
            if (v != null && "COMPLETADA".equalsIgnoreCase(v.getEstado()) && formaPago.equalsIgnoreCase(v.getFormaPago())) {
                sum += v.getTotal();
            }
        }
        return Math.round(sum * 100.0) / 100.0;
    }

    /**
     * Total acumulado de todas las ventas vigentes del turno (RF16).
     */
    public double totalVentasVigentes() {
        if (ventas == null) return 0.0;
        double sum = 0.0;
        for (Venta v : ventas) {
            if (v != null && "COMPLETADA".equalsIgnoreCase(v.getEstado())) {
                sum += v.getTotal();
            }
        }
        return Math.round(sum * 100.0) / 100.0;
    }

    /**
     * Cierra el turno si está abierto, calculando la diferencia contra el efectivo contado.
     */
    public double cerrarTurno(double efectivoContado, double totalVentasEfectivo) {
        if (!estaAbierto()) {
            return this.diferencia;
        }
        this.fechaCierre = LocalDateTime.now();
        this.efectivoContado = efectivoContado;
        this.montoEsperado = Math.round((this.montoInicial + totalVentasEfectivo) * 100.0) / 100.0;
        this.diferencia = Math.round((this.efectivoContado - this.montoEsperado) * 100.0) / 100.0;
        this.estado = "CERRADO";
        return this.diferencia;
    }

    public double calcularDiferencia() {
        this.diferencia = Math.round((this.efectivoContado - this.montoEsperado) * 100.0) / 100.0;
        return this.diferencia;
    }

    // Getters y Setters completos
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDateTime getFechaApertura() { return fechaApertura; }
    public void setFechaApertura(LocalDateTime fechaApertura) { this.fechaApertura = fechaApertura; }

    public double getMontoInicial() { return montoInicial; }
    public void setMontoInicial(double montoInicial) { this.montoInicial = montoInicial; }

    public LocalDateTime getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(LocalDateTime fechaCierre) { this.fechaCierre = fechaCierre; }

    public double getEfectivoContado() { return efectivoContado; }
    public void setEfectivoContado(double efectivoContado) { this.efectivoContado = efectivoContado; }

    public double getMontoEsperado() { return montoEsperado; }
    public void setMontoEsperado(double montoEsperado) { this.montoEsperado = montoEsperado; }

    public double getDiferencia() { return diferencia; }
    public void setDiferencia(double diferencia) { this.diferencia = diferencia; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public List<Venta> getVentas() { return ventas; }
    public void setVentas(List<Venta> ventas) { this.ventas = ventas != null ? ventas : new ArrayList<>(); }

    @Override
    public String toString() {
        return "Turno #" + id + " [" + estado + "] - " + (usuario != null ? usuario.getNombreCompleto() : "N/A");
    }
}
