package farmac.dominio;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Clase base abstracta que representa a un usuario general del sistema Farmac.
 * Aplica hash seguro BCrypt (A5) y herencia polimórfica (Encargado / Empleado).
 */
public abstract class Usuario {
    protected int id;
    protected String nombreUsuario;
    protected String nombreCompleto;
    protected String password; // Puede almacenar hash BCrypt o texto durante captura
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

    /**
     * Valida la contraseña ingresada contra el hash BCrypt o texto plano si fuera migración.
     */
    public boolean autenticar(String passwordIngresada) {
        if (!this.activo || this.password == null || passwordIngresada == null) {
            return false;
        }
        try {
            if (this.password.startsWith("$2a$") || this.password.startsWith("$2b$") || this.password.startsWith("$2y$")) {
                return BCrypt.checkpw(passwordIngresada, this.password);
            }
        } catch (Exception e) {
            // Si el formato no coincide con BCrypt, fallback a comparación directa
        }
        return this.password.equals(passwordIngresada);
    }

    /**
     * Hashea y actualiza la contraseña usando BCrypt (A5).
     */
    public void cambiarPassword(String nuevaPassword) {
        if (nuevaPassword != null && !nuevaPassword.trim().isEmpty()) {
            this.password = BCrypt.hashpw(nuevaPassword, BCrypt.gensalt());
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
