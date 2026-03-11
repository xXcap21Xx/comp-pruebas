import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CONTROLADOR DE LA FASE SINTÁCTICA
 */
public class MotorSintactico {

    
    public static class ResultadoSintactico {
        public NodoAST raiz;             // El árbol generado
        public String logArbol;         // El dibujo textual del árbol
        public List<ErrorDatosSintactico> errores; // Lista estructurada de errores
        public TablaSimbolos tablaSimbolos; // La tabla llena tras el análisis
        public TablaErrores tablaErrores;   // Los errores registrados en la tabla oficial

        public ResultadoSintactico() {
            this.errores = new ArrayList<>();
        }
    }

    /**
     * Representación de un error para la tabla visual.
     */
    public static class ErrorDatosSintactico implements Comparable<ErrorDatosSintactico> {
        public int linea;
        public String descripcion;
        
        public ErrorDatosSintactico(int l, String d) { 
            this.linea = l; 
            this.descripcion = d; 
        }

        @Override 
        public int compareTo(ErrorDatosSintactico o) {
            return Integer.compare(this.linea, o.linea);
        }
    }

    
    public static ResultadoSintactico ejecutar(List<Token> tokensValidos) {
        ResultadoSintactico resultado = new ResultadoSintactico();
        
        if (tokensValidos == null || tokensValidos.isEmpty()) {
            resultado.logArbol = "No hay tokens válidos para analizar.";
            return resultado;
        }

        try {
            // 1. Preparar dependencias del AnalizadorSintactico
            Token[] arrayTokens = tokensValidos.toArray(new Token[0]);
            TablaSimbolos ts = new TablaSimbolos();
            TablaErrores te = new TablaErrores();
            
            AnalizadorSintactico parser = new AnalizadorSintactico(arrayTokens, ts, te);
            resultado.raiz = parser.analizar();
            
            resultado.tablaSimbolos = ts;
            resultado.tablaErrores = te;

            StringBuilder sb = new StringBuilder();
            for (String paso : parser.getArbolDerivacion()) {
                sb.append(paso).append("\n");
            }
            resultado.logArbol = sb.toString();

            // 4. Procesar Errores del Parser
            List<String> erroresCrudos = parser.getErrores();
            for (String errStr : erroresCrudos) {
                int linea = extraerLinea(errStr);
                resultado.errores.add(new ErrorDatosSintactico(linea, errStr));
            }
            
            Collections.sort(resultado.errores);

        } catch (Exception e) {
            resultado.errores.add(new ErrorDatosSintactico(0, "Error crítico en MotorSintactico: " + e.getMessage()));
            e.printStackTrace();
        }

        return resultado;
    }

    /**
     * Método auxiliar para extraer el número de línea de un mensaje tipo "DSL(203) [Línea 5]: ..."
     */
    private static int extraerLinea(String mensaje) {
        try {
            if (mensaje.contains("[Línea ")) {
                int inicio = mensaje.indexOf("[Línea ") + 7;
                int fin = mensaje.indexOf("]");
                return Integer.parseInt(mensaje.substring(inicio, fin).trim());
            }
        } catch (Exception ignored) {}
        return 0;
    }
}