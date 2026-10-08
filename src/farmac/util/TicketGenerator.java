package farmac.util;

import farmac.dominio.DetalleVenta;
import farmac.dominio.Venta;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Generador nativo de tickets en formato PDF estandar (PDF-1.4) sin dependencias externas.
 * Organiza los tickets en carpetas diarias: tickets/YYYY-MM-DD/ticket_XXXXX_HHmmss.pdf
 */
public class TicketGenerator {

    private static final String CARPETA_BASE = "tickets";
    private static final DateTimeFormatter FORMATO_FECHA_CARPETA = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FORMATO_HORA_ARCHIVO = DateTimeFormatter.ofPattern("HHmmss");
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static File generarTicketPDF(Venta venta) {
        if (venta == null) return null;

        try {
            LocalDate hoy = LocalDate.now();
            String nombreCarpetaDia = hoy.format(FORMATO_FECHA_CARPETA);

            File dirDia = new File(CARPETA_BASE, nombreCarpetaDia);
            if (!dirDia.exists()) {
                dirDia.mkdirs();
            }

            String horaStr = venta.getFechaHora() != null ? venta.getFechaHora().format(FORMATO_HORA_ARCHIVO) : "000000";
            String nombreArchivo = String.format("ticket_%05d_%s.pdf", venta.getId(), horaStr);
            File archivoPDF = new File(dirDia, nombreArchivo);

            // Preparar lineas de texto para el ticket
            List<String> lineas = new ArrayList<>();
            lineas.add("==========================================");
            lineas.add("                FARMAC                    ");
            lineas.add("       Farmacia Integral S.A. de C.V.     ");
            lineas.add("       RFC: FAR-920815-KL0                ");
            lineas.add("       Sucursal Principal - Centro        ");
            lineas.add("==========================================");
            lineas.add(String.format("Folio de Venta: #%05d", venta.getId()));
            lineas.add("Fecha y Hora:  " + (venta.getFechaHora() != null ? venta.getFechaHora().format(FORMATO_FECHA_HORA) : hoy.toString()));
            lineas.add("Cajero:        " + (venta.getUsuario() != null ? venta.getUsuario().getNombreCompleto() : "General"));
            lineas.add("Forma de Pago: " + venta.getFormaPago());
            lineas.add("------------------------------------------");
            lineas.add(String.format("%-18s %4s %8s %8s", "MEDICAMENTO / LOTE", "CANT", "PRECIO", "TOTAL"));
            lineas.add("------------------------------------------");

            for (DetalleVenta d : venta.getDetalles()) {
                String nom = (d.getLote() != null && d.getLote().getProducto() != null)
                        ? d.getLote().getProducto().getNombre() : "Medicamento";
                if (nom.length() > 18) {
                    nom = nom.substring(0, 15) + "...";
                }
                lineas.add(String.format("%-18s %4d %8.2f %8.2f", nom, d.getCantidad(), d.getPrecioUnitario(), d.getSubtotal()));
                if (d.getLote() != null) {
                    lineas.add(String.format("  [Lote: %s | Cad: %s]", d.getLote().getNumeroLote(), d.getLote().getFechaCaducidad()));
                }
            }

            lineas.add("------------------------------------------");
            lineas.add(String.format("TOTAL A PAGAR:                $%10.2f", venta.getTotal()));
            lineas.add("==========================================");

            if (venta.getReceta() != null && venta.getReceta().validarDatos()) {
                lineas.add("DATOS DE RECETA MEDICA (COFEPRIS):");
                lineas.add("Folio Receta: " + venta.getReceta().getFolioReceta());
                lineas.add("Medico:       Dr. " + venta.getReceta().getNombreMedico());
                lineas.add("Cedula Prof:  " + venta.getReceta().getCedulaProfesional());
                lineas.add("Expedicion:   " + venta.getReceta().getFechaExpedicion());
                lineas.add("------------------------------------------");
            }

            lineas.add("        Cada lote, bajo control.          ");
            lineas.add("       ¡Gracias por su compra!            ");
            lineas.add("==========================================");

            // Escribir estructura binaria PDF-1.4 valida
            byte[] bytesPdf = construirPdfBytes(lineas);
            try (FileOutputStream fos = new FileOutputStream(archivoPDF)) {
                fos.write(bytesPdf);
            }

            return archivoPDF;
        } catch (Exception e) {
            System.err.println("Error al generar ticket PDF: " + e.getMessage());
            return null;
        }
    }

    /**
     * Construye un archivo PDF 1.4 valido con el texto del ticket.
     */
    private static byte[] construirPdfBytes(List<String> lineas) throws Exception {
        ByteArrayOutputStream streamContent = new ByteArrayOutputStream();
        streamContent.write("BT\n".getBytes("ISO-8859-1"));
        streamContent.write("/F1 10 Tf\n".getBytes("ISO-8859-1"));
        streamContent.write("14 TL\n".getBytes("ISO-8859-1"));
        streamContent.write("35 550 Td\n".getBytes("ISO-8859-1"));

        for (String linea : lineas) {
            String limpia = linea.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
            streamContent.write(String.format("(%s) '\n", limpia).getBytes("ISO-8859-1"));
        }
        streamContent.write("ET\n".getBytes("ISO-8859-1"));

        byte[] contentBytes = streamContent.toByteArray();

        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();

        pdf.write("%PDF-1.4\n".getBytes("ISO-8859-1"));

        // Obj 1: Catalog
        offsets.add(pdf.size());
        pdf.write("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n".getBytes("ISO-8859-1"));

        // Obj 2: Pages
        offsets.add(pdf.size());
        pdf.write("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n".getBytes("ISO-8859-1"));

        // Obj 3: Page
        offsets.add(pdf.size());
        pdf.write("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 300 600] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>\nendobj\n".getBytes("ISO-8859-1"));

        // Obj 4: Content Stream
        offsets.add(pdf.size());
        pdf.write(String.format("4 0 obj\n<< /Length %d >>\nstream\n", contentBytes.length).getBytes("ISO-8859-1"));
        pdf.write(contentBytes);
        pdf.write("\nendstream\nendobj\n".getBytes("ISO-8859-1"));

        // Obj 5: Font (Courier monoespaciado para ticket termico perfecto)
        offsets.add(pdf.size());
        pdf.write("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>\nendobj\n".getBytes("ISO-8859-1"));

        // XRef
        int xrefOffset = pdf.size();
        pdf.write(String.format("xref\n0 %d\n", offsets.size() + 1).getBytes("ISO-8859-1"));
        pdf.write("0000000000 65535 f \n".getBytes("ISO-8859-1"));
        for (int off : offsets) {
            pdf.write(String.format("%010d 00000 n \n", off).getBytes("ISO-8859-1"));
        }

        pdf.write(String.format("trailer\n<< /Size %d /Root 1 0 R >>\nstartxref\n%d\n%%%%EOF\n", offsets.size() + 1, xrefOffset).getBytes("ISO-8859-1"));

        return pdf.toByteArray();
    }
}
