package farmac.dominio;

/**
 * Representa una empresa proveedora de medicamentos y material de curación (RF07, RF20).
 */
public class Proveedor {
    private int id;
    private String razonSocial;
    private String contacto;
    private String telefono;
    private boolean activo;

    public Proveedor() {
        this.activo = true;
    }

    public Proveedor(int id, String razonSocial, String contacto, String telefono, boolean activo) {
        this.id = id;
        this.razonSocial = razonSocial;
        this.contacto = contacto;
        this.telefono = telefono;
        this.activo = activo;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }

    public String getContacto() { return contacto; }
    public void setContacto(String contacto) { this.contacto = contacto; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    @Override
    public String toString() {
        return razonSocial != null ? razonSocial : "Proveedor #" + id;
    }
}
