package farmac.dominio;

import java.time.LocalDateTime;

/**
 * Representa un turno de caja abierto por un usuario para transacciones financieras.
 */
public class TurnoCaja {
    private int id;
    private LocalDateTime fechaApertura;
    private double montoInicial;
    private LocalDateTime fechaCierre;
    private double efectivoContado;
    private double montoEsperado;
    private String estado; // "ABIERTO", "CERRADO"
    private Usuario usuario;

    public TurnoCaja() {
        this.estado = "ABIERTO";
        this.fechaApertura = LocalDateTime.now();
    }

    public TurnoCaja(int id, Usuario usuario, double montoInicial) {
        this.id = id;
        this.usuario = usuario;
        this.montoInicial = montoInicial;
        this.fechaApertura = LocalDateTime.now();
        this.estado = "ABIERTO";
    }

    public double cerrarTurno(double efectivoContado, double totalVentasEfectivo) {
        this.fechaCierre = LocalDateTime.now();
        this.efectivoContado = efectivoContado;
        this.montoEsperado = this.montoInicial + totalVentasEfectivo;
        this.estado = "CERRADO";
        return calcularDiferencia();
    }

    public double calcularDiferencia() {
        return this.efectivoContado - this.montoEsperado;
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

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    @Override
    public String toString() {
        return "Turno #" + id + " [" + estado + "] - " + (usuario != null ? usuario.getNombreCompleto() : "N/A");
    }
}
