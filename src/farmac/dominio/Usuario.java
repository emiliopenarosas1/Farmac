package farmac.dominio;

/**
 * Clase base abstracta que representa a un usuario general del sistema Farmac.
 */
public abstract class Usuario {
    protected int id;
    protected String nombreUsuario;
    protected String nombreCompleto;
    protected String password;
    protected boolean activo;

    public Usuario() {
        this.activo = true;
    }

    public Usuario(int id, String nombreUsuario, String nombreCompleto, String password, boolean activo) {
        this.id = id;
        this.nombreUsuario = nombreUsuario;
        this.nombreCompleto = nombreCompleto;
        this.password = password;
        this.activo = activo;
    }

    public boolean autenticar(String passwordIngresada) {
        return this.activo && this.password != null && this.password.equals(passwordIngresada);
    }

    public void cambiarPassword(String nuevaPassword) {
        if (nuevaPassword != null && !nuevaPassword.trim().isEmpty()) {
            this.password = nuevaPassword;
        }
    }

    public abstract String obtenerRol();

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    @Override
    public String toString() {
        return nombreCompleto + " (" + obtenerRol() + ")";
    }
}
