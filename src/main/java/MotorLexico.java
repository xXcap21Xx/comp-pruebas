import java.util.ArrayList;
import java.util.List;

/*
 ESTA CLASE ACTÚA COMO EL MOTOR O CONTROLADOR DE LA FASE LÉXICA
 Su función principal es orquestar el proceso:
 1. Obtener los tokens crudos usando el Tokenizador.
 2. Configurar el Autómata con las reglas del DSL.
 3. Clasificar los tokens y separar los válidos de los errores.
 4. Empaquetar todo en un objeto limpio para que la GUI solo tenga que mostrarlo.
 */
public class MotorLexico {

    /*
    Clase interna auxiliar (DTO) para transportar los resultados.
    Sirve para devolver tres cosas a la vez:
    - La lista limpia de tokens para el analizador sintáctico.
    - Los datos formateados para llenar la JTable en la ventana.
    - La lista de errores encontrados.
     */
    public static class ResultadoLexico {
        public List<Token> tokensValidos;
        public List<Object[]> datosSimbolos;
        public List<ErrorDatos> errores;

        public ResultadoLexico() {
            tokensValidos = new ArrayList<>();
            datosSimbolos = new ArrayList<>();
            errores = new ArrayList<>();
        }
    }

    // Clase auxiliar simple para guardar la linea y descripcion de un error
    public static class ErrorDatos {
        public int linea;
        public String descripcion;
        public ErrorDatos(int l, String d) { this.linea = l; this.descripcion = d; }
    }

    /*
    MÉTODO PRINCIPAL DE EJECUCIÓN LÉXICA
    Recibe el código fuente como texto y retorna el paquete completo de resultados.
    Aquí es donde se conecta el DSLCore con el Automata.
     */
    public static ResultadoLexico ejecutar(String codigo) {
        ResultadoLexico resultado = new ResultadoLexico();

        try {
            // 1. Primero convertimos el texto plano en una lista de tokens crudos
            // usando las expresiones regulares definidas en DSLCore
            Token[] tokensBrutos = DSL.CrearToken(codigo);
            
            // 2. Instanciamos el autómata finito determinista (AFD)
            // Le pasamos todos los mapas y sets que definimos en DSLCore (estados, alfabeto, transiciones)
            Automata auto = new Automata(
                DSL.getEstadosAceptacion(), 
                DSL.getAlfabetoDSL(), 
                DSL.getTransiciones(), 
                "INICIO", 
                DSL.getEstadosAceptacion()
            );

            // 3. Hacemos que el autómata procese los tokens para determinar su tipo real
            Token[] resultadosLexicos = auto.aceptar(tokensBrutos);

            // 4. Filtramos los resultados: Separamos lo que sirve de lo que es error
            for (Token t : resultadosLexicos) {
                
                // Si el autómata marcó el token como ERROR o si no existe en el alfabeto
                if (t.getTipoToken().startsWith("ERROR") || !t.existeSimbolo()) {
                 
                    // Asignamos un código de error específico para el manual de usuario
                    String codigoError = "DSL(100)"; // Error genérico
                    if (t.getTipoToken().contains("CADENA")) codigoError = "DSL(102)"; // String mal cerrado
                    else if (t.getTipoToken().contains("SIMBOLO")) codigoError = "DSL(101)"; // Caracter ilegal
                    else if (t.getTipoToken().contains("MALFORMADO")) codigoError = "DSL(103)"; // ID mal formado

                    String desc = String.format("%s Lexema '%s' no válido. Causa: %s",
                            codigoError, t.getLexema(), t.getTipoToken());
                    
                    // Lo agregamos a la lista de errores para mostrar en la consola roja
                    resultado.errores.add(new ErrorDatos(t.getLinea(), desc));
                } else {
                 
                    // Si es válido, lo guardamos en la lista que usará el Sintáctico
                    resultado.tokensValidos.add(t);
                    
                    // Y también preparamos el array de objetos para la Tabla de Símbolos visual
                    resultado.datosSimbolos.add(new Object[]{
                        t.getLexema(), 
                        t.getLinea(), 
                        t.getColumna(), 
                        t.getTipoToken() 
                    });
                }
            }
        } catch (Exception e) {
            // Captura de errores criticos del sistema para que no se cierre el programa
            resultado.errores.add(new ErrorDatos(0, "Error crítico en motor léxico: " + e.getMessage()));
        }

        return resultado;
    }
}