package farmac;

import farmac.dominio.Empleado;
import farmac.dominio.Lote;
import farmac.dominio.MovimientoInventario;
import farmac.dominio.OperacionesInventario;
import farmac.dominio.OperacionesInventario.DespachoLote;
import farmac.dominio.Producto;
import farmac.dominio.Proveedor;
import farmac.dominio.RecetaMedica;
import farmac.dominio.TurnoCaja;
import farmac.dominio.Usuario;
import farmac.dominio.Venta;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class FarmacNegocioTest {

    private Proveedor proveedorTest;

    @Before
    public void setUp() {
        proveedorTest = new Proveedor(1, "Distribuidora FarmaCorp", "Juan Perez", "555-1234", true);
    }

    @Test
    public void testAutenticacionBCryptUsuario() {
        Usuario user = new Empleado(1, "carlos", "Carlos Lopez", "temp", true);
        user.cambiarPassword("claveSecreta123");
        
        // Verifica que la contrasena se haya transformado en un hash BCrypt ($2a$)
        Assert.assertNotEquals("claveSecreta123", user.getPassword());
        Assert.assertTrue(user.getPassword().startsWith("$2a$") || user.getPassword().startsWith("$2b$") || user.getPassword().startsWith("$2y$"));
        
        // Verificacion con clave correcta
        Assert.assertTrue("Debe autenticar con la clave correcta", user.autenticar("claveSecreta123"));
        
        // Verificacion con clave incorrecta
        Assert.assertFalse("Debe rechazar clave incorrecta", user.autenticar("claveIncorrecta"));
        Assert.assertFalse("Debe rechazar clave vacia", user.autenticar(""));
    }

    @Test
    public void testVigenciaRecetaMedica() {
        Producto pControlado = new Producto(1, "7501001", "Amoxicilina 500mg", "Antibioticos",
                proveedorTest, 95.00, true, 10, true);
        
        // Receta emitida hace 10 dias (debe estar vigente)
        RecetaMedica recetaValida = new RecetaMedica(1, pControlado, "Dr. Ramirez", "CED-12345",
                LocalDate.now().minusDays(10), "REC-001");
        Assert.assertTrue("Receta de 10 dias debe estar vigente", recetaValida.estaVigente());

        // Receta emitida hace exactamente 30 dias (debe estar vigente segun limite de 30 dias)
        RecetaMedica recetaLimite = new RecetaMedica(2, pControlado, "Dr. Ramirez", "CED-12345",
                LocalDate.now().minusDays(30), "REC-002");
        Assert.assertTrue("Receta de 30 dias debe estar vigente", recetaLimite.estaVigente());

        // Receta emitida hace 31 dias (vencida)
        RecetaMedica recetaVencida = new RecetaMedica(3, pControlado, "Dr. Ramirez", "CED-12345",
                LocalDate.now().minusDays(31), "REC-003");
        Assert.assertFalse("Receta de 31 dias no debe estar vigente", recetaVencida.estaVigente());
    }

    @Test
    public void testDespachoFEFOSurtidoMultilote() {
        Producto producto = new Producto(10, "7509999", "Paracetamol 500mg", "Analgesicos",
                proveedorTest, 35.00, false, 5, true);

        // Tres lotes con fechas de vencimiento escalonadas
        // Lote 1 vence en 2 meses, stock 5
        Lote loteProximo = new Lote(1, "LT-01", LocalDate.now().plusMonths(2), 5, 15.00, producto);
        // Lote 2 vence en 6 meses, stock 10
        Lote loteMedio = new Lote(2, "LT-02", LocalDate.now().plusMonths(6), 10, 15.00, producto);
        // Lote 3 vence en 12 meses, stock 20
        Lote loteLejano = new Lote(3, "LT-03", LocalDate.now().plusMonths(12), 20, 15.00, producto);

        OperacionesInventario op = new OperacionesInventario();
        // Insertamos en orden no cronológico para comprobar que ordena por FEFO
        op.registrarEntrada(loteLejano);
        op.registrarEntrada(loteProximo);
        op.registrarEntrada(loteMedio);

        // Pedimos 12 unidades: debe tomar 5 de LT-01 y 7 de LT-02
        List<DespachoLote> despachos = op.calcularDespachoFEFO(producto, 12);
        
        Assert.assertEquals(2, despachos.size());
        Assert.assertEquals(loteProximo.getNumeroLote(), despachos.get(0).getLote().getNumeroLote());
        Assert.assertEquals(5, despachos.get(0).getCantidad());

        Assert.assertEquals(loteMedio.getNumeroLote(), despachos.get(1).getLote().getNumeroLote());
        Assert.assertEquals(7, despachos.get(1).getCantidad());
    }

    @Test
    public void testTurnoCajaCalculosYArqueo() {
        Usuario cajero = new Empleado(2, "maria", "Maria Gomez", "pass123", true);
        TurnoCaja turno = new TurnoCaja(101, cajero, 500.00);

        Assert.assertTrue("Turno recien creado debe estar abierto", turno.estaAbierto());

        // Crear ventas
        Venta v1 = new Venta(1, turno, cajero, "EFECTIVO");
        v1.setTotal(150.00);
        
        Venta v2 = new Venta(2, turno, cajero, "TARJETA");
        v2.setTotal(300.00);

        Venta v3 = new Venta(3, turno, cajero, "EFECTIVO");
        v3.setTotal(50.00);
        v3.setEstado("CANCELADA"); // Venta cancelada

        turno.agregarVenta(v1);
        turno.agregarVenta(v2);
        turno.agregarVenta(v3);

        // Total ventas vigentes: solo v1 (150) + v2 (300) = 450
        Assert.assertEquals(450.00, turno.totalVentasVigentes(), 0.001);

        // Total efectivo: v1 (150)
        Assert.assertEquals(150.00, turno.totalPorFormaPago("EFECTIVO"), 0.001);
        // Total tarjeta: v2 (300)
        Assert.assertEquals(300.00, turno.totalPorFormaPago("TARJETA"), 0.001);

        // Arqueo:
        // Monto inicial: 500.00
        // Ventas efectivo vigentes: 150.00
        // Total esperado en caja: 650.00
        // Si el cajero cuenta 650.00, diferencia debe ser 0.00
        double difCero = turno.cerrarTurno(650.00, turno.totalPorFormaPago("EFECTIVO"));
        Assert.assertEquals(0.00, difCero, 0.001);
        Assert.assertFalse("El turno debe estar cerrado despues de cerrarTurno", turno.estaAbierto());
        Assert.assertNotNull(turno.getFechaCierre());
    }

    @Test
    public void testValorTotalInventarioYProductosBajoStock() {
        Producto p1 = new Producto(1, "P1", "Ibuprofeno", "Analgesicos", proveedorTest, 40.00, false, 10, true);
        Producto p2 = new Producto(2, "P2", "Loratadina", "Antihistaminicos", proveedorTest, 25.00, false, 15, true);

        // Lotes para P1: 5 unidades (bajo stock porque stockMinimo es 10)
        Lote l1 = new Lote(1, "L-P1", LocalDate.now().plusMonths(5), 5, 20.00, p1);
        // Lotes para P2: 20 unidades (stock normal porque stockMinimo es 15)
        Lote l2 = new Lote(2, "L-P2", LocalDate.now().plusMonths(8), 20, 10.00, p2);

        OperacionesInventario op = new OperacionesInventario();
        op.registrarEntrada(l1);
        op.registrarEntrada(l2);

        // Valor total inventario = (5 * 20.00) + (20 * 10.00) = 100 + 200 = 300.00
        double valorTotal = op.valorTotalInventario();
        Assert.assertEquals(300.00, valorTotal, 0.001);

        // Productos bajo stock: solo p1
        List<Producto> bajoStock = op.productosBajoStock(Arrays.asList(p1, p2));
        Assert.assertEquals(1, bajoStock.size());
        Assert.assertEquals("Ibuprofeno", bajoStock.get(0).getNombre());
    }

    @Test
    public void testAjusteInventarioGeneraMovimiento() {
        Producto p = new Producto(1, "P1", "Omeprazol", "Antiulcerosos", proveedorTest, 50.00, false, 5, true);
        Lote lote = new Lote(1, "LT-OME", LocalDate.now().plusMonths(6), 15, 25.00, p);
        Usuario admin = new Empleado(1, "admin", "Admin Sistema", "admin123", true);

        OperacionesInventario op = new OperacionesInventario();
        op.registrarEntrada(lote);

        // Descontar 3 por merma/danado
        MovimientoInventario mov = op.ajustarStockLote(lote, 3, "DANADO", admin);

        Assert.assertNotNull(mov);
        Assert.assertEquals(-3, mov.getCantidad());
        Assert.assertEquals("AJUSTE", mov.getTipo());
        Assert.assertEquals("DANADO", mov.getMotivo());
        Assert.assertEquals(12, lote.getCantidadActual());
    }

    @Test
    public void testProductoBajaLogica() {
        Producto p = new Producto(5, "750000000001", "Aspirina", "Analgesicos", proveedorTest, 20.00, false, 5, true);
        
        Assert.assertTrue("Producto nuevo debe estar activo", p.isActivo());
        
        // Baja logica
        p.setActivo(false);
        Assert.assertFalse("Producto dado de baja logica debe tener activo = false", p.isActivo());
    }

    @Test
    public void testConexionBDYBusquedaUsuarioDAO() {
        try {
            farmac.dominio.Usuario admin = farmac.dao.UsuarioDAO.buscarPorLogin("admin");
            Assert.assertNotNull("Usuario admin debe existir en BD", admin);
            Assert.assertEquals("admin", admin.getNombreUsuario());
            Assert.assertTrue("admin debe autenticar con admin123", admin.autenticar("admin123"));
        } catch (java.sql.SQLException e) {
            Assert.fail("Error conectando a BD: " + e.getMessage());
        }
    }
}
