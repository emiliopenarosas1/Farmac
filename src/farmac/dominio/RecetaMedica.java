package farmac.dominio;

import java.time.LocalDate;

/**
 * Representa los datos de una receta médica obligatoria para medicamentos controlados/antibióticos.
 */
public class RecetaMedica {
    private int id;
    private String nombreMedico;
    private String cedulaProfesional;
    private LocalDate fechaExpedicion;
    private String folioReceta;
    private Producto producto;

    public RecetaMedica() {}

    public RecetaMedica(int id, Producto producto, String nombreMedico, String cedulaProfesional,
                        LocalDate fechaExpedicion, String folioReceta) {
        this.id = id;
        this.producto = producto;
        this.nombreMedico = nombreMedico;
        this.cedulaProfesional = cedulaProfesional;
        this.fechaExpedicion = fechaExpedicion;
        this.folioReceta = folioReceta;
    }

    public boolean validarDatos() {
        return nombreMedico != null && !nombreMedico.trim().isEmpty()
                && cedulaProfesional != null && !cedulaProfesional.trim().isEmpty()
                && fechaExpedicion != null
                && folioReceta != null && !folioReceta.trim().isEmpty()
                && producto != null;
    }

    public boolean estaVigente(LocalDate fechaActual) {
        if (fechaExpedicion == null || fechaActual == null) return false;
        return !fechaExpedicion.plusDays(30).isBefore(fechaActual);
    }

    public boolean estaVigente() {
        return estaVigente(LocalDate.now());
    }

    // Getters y Setters completos
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombreMedico() { return nombreMedico; }
    public void setNombreMedico(String nombreMedico) { this.nombreMedico = nombreMedico; }

    public String getCedulaProfesional() { return cedulaProfesional; }
    public void setCedulaProfesional(String cedulaProfesional) { this.cedulaProfesional = cedulaProfesional; }

    public LocalDate getFechaExpedicion() { return fechaExpedicion; }
    public void setFechaExpedicion(LocalDate fechaExpedicion) { this.fechaExpedicion = fechaExpedicion; }

    public String getFolioReceta() { return folioReceta; }
    public void setFolioReceta(String folioReceta) { this.folioReceta = folioReceta; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }

    @Override
    public String toString() {
        return "Receta #" + folioReceta + " - Dr. " + nombreMedico + " (Céd: " + cedulaProfesional + ")";
    }
}
