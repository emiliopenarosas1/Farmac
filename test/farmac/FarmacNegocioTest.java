package farmac;

import farmac.dominio.*;
import farmac.util.TicketGenerator;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Pruebas unitarias de las 10 clases POO y generador de tickets en formato PDF.
 */
public class FarmacNegocioTest {

    private Producto productoReceta;
    private Producto productoLibre;
    private Lote loteVigente;
    private Lote loteVencido;
    private Encargado admin;
    private Empleado cajero;
    private TurnoCaja turno;

    @Before
    public void setUp() {
        admin = new Encargado(1, "admin", "Administrador General", "123456", true);
        cajero = new Empleado(2, "empleado1", "Carlos Lopez", "123456", true);
        turno = cajero.iniciarTurno(500.0);

        productoReceta = new Producto(1, "75010001", "Amoxicilina 500mg", "Antibioticos", "Farmaceutica Nal", 85.0, true, 5);
        productoLibre = new Producto(2, "75010002", "Paracetamol 500mg", "Analgesicos", "Laboratorios Alfa", 25.0, false, 10);

        loteVigente = new Lote(1, "LT-2027-01", LocalDate.now().plusMonths(6), 50, 40.0, productoReceta);
        loteVencido = new Lote(2, "LT-2025-02", LocalDate.now().minusMonths(1), 10, 35.0, productoReceta);
    }

    @Test
    public void testHerenciaYAutenticacionUsuarios() {
        assertEquals("ENCARGADO", admin.obtenerRol());
        assertEquals("EMPLEADO", cajero.obtenerRol());
        assertTrue(admin.autenticar("123456"));
        assertFalse(admin.autenticar("wrongpass"));
    }

    @Test
    public void testCalculoSubtotalDetalleYTotalVenta() {
        Venta venta = new Venta(1001, turno, cajero, "EFECTIVO");
        venta.agregarDetalle(loteVigente, 2, productoReceta.getPrecioVenta());

        assertEquals(1, venta.getDetalles().size());
        assertEquals(170.0, venta.getTotal(), 0.001);
    }

    @Test
    public void testControlStockYCancelacionVenta() {
        Venta venta = new Venta(1002, turno, cajero, "EFECTIVO");
        loteVigente.descontarStock(3);
        assertEquals(47, loteVigente.getCantidadActual());

        venta.agregarDetalle(loteVigente, 3, productoReceta.getPrecioVenta());
        venta.cancelar("Error de cajero");

        assertEquals("CANCELADA", venta.getEstado());
        assertEquals(50, loteVigente.getCantidadActual());
    }

    @Test
    public void testRecetaMedicaValidacion() {
        RecetaMedica recetaValida = new RecetaMedica(1, productoReceta, "Dr. Roberto Garza", "MED-884920", LocalDate.now(), "REC-001");
        assertTrue(recetaValida.validarDatos());
        assertTrue(recetaValida.estaVigente(LocalDate.now()));

        RecetaMedica recetaInvalida = new RecetaMedica(2, productoReceta, "", "", null, "");
        assertFalse(recetaInvalida.validarDatos());
    }

    @Test
    public void testArqueoCajaYCorte() {
        double diferencia = turno.cerrarTurno(1250.0, 750.0);
        assertEquals(0.0, diferencia, 0.001);
        assertEquals("CERRADO", turno.getEstado());
    }

    @Test
    public void testOperacionesInventarioFEFO() {
        OperacionesInventario op = new OperacionesInventario(60);
        op.registrarEntrada(loteVigente);
        op.registrarEntrada(loteVencido);

        List<Lote> fefo = op.obtenerLotesFEFO(productoReceta, 1);
        assertEquals(1, fefo.size());
        assertEquals("LT-2027-01", fefo.get(0).getNumeroLote());
    }

    @Test
    public void testGeneracionTicketPDFEnCarpetaDiaria() {
        Venta venta = new Venta(555, turno, cajero, "EFECTIVO");
        venta.agregarDetalle(loteVigente, 1, productoReceta.getPrecioVenta());
        RecetaMedica rec = new RecetaMedica(1, productoReceta, "Dr. Roberto Garza", "MED-884920", LocalDate.now(), "REC-555");
        venta.asignarReceta(rec);

        File ticketPdf = TicketGenerator.generarTicketPDF(venta);
        assertNotNull(ticketPdf);
        assertTrue(ticketPdf.exists());
        assertTrue(ticketPdf.getName().endsWith(".pdf"));
        assertTrue(ticketPdf.getPath().contains("tickets"));
    }
}
