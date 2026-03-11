
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 ESTA CLASE ES EL "MOTOR" DEL ANÁLISIS LÉXICO.
 Un autómata es como una máquina teórica que lee caracteres uno por uno y cambia de estado.
 Su objetivo principal aquí es decidir si una palabra es una "Palabra Reservada" (como IF, WHILE, PILA)
 o si es otra cosa (como un Identificador o un error).
 */
public class Automata {

    // --- ATRIBUTOS DEL AUTÓMATA ---
    // Conjunto de todos los estados posibles por los que puede pasar la máquina.
    private final Set<String> estados;

    // El alfabeto: son los caracteres válidos que la máquina entiende (letras, números, símbolos).
    private final Set<Character> alfabeto;

    // El "mapa de carreteras". Dice: "Si estoy en el estado A y leo la letra 'x', me muevo al estado B".
    private final Map<String, Map<Character, String>> transiciones;

    // El punto de partida. Donde empieza la máquina antes de leer nada.
    private final String estadoInicial;

    // Los estados "meta". Si la máquina termina aquí al final de la palabra, la palabra es válida.
    private final Set<String> estadosAceptacion;

    /*
     DICCIONARIO DE TIPOS:
     Este mapa es una lista maestra que nos dice qué "etiqueta" ponerle a cada palabra reservada.
     Por ejemplo: Si encontramos "PILA", le ponemos la etiqueta "PALABRA_RESERVADA".
     Si encontramos "IF", le ponemos "PC_IF" (Palabra de Control IF).
     */
    private static final Map<String, String> TIPO_POR_PR = Map.ofEntries(
            Map.entry("PILA", "PALABRA_RESERVADA"),
            Map.entry("PILA_CIRCULAR", "PALABRA_RESERVADA"),
            Map.entry("COLA", "PALABRA_RESERVADA"),
            Map.entry("BICOLA", "PALABRA_RESERVADA"),
            Map.entry("LISTA_ENLAZADA", "PALABRA_RESERVADA"),
            Map.entry("LISTA_DOBLE_ENLAZADA", "PALABRA_RESERVADA"),
            Map.entry("LISTA_CIRCULAR", "PALABRA_RESERVADA"),
            Map.entry("ARBOL_BINARIO", "PALABRA_RESERVADA"),
            Map.entry("TABLA_HASH", "PALABRA_RESERVADA"),
            Map.entry("GRAFO", "PALABRA_RESERVADA"),
            Map.entry("VER_FILA", "PALABRA_RESERVADA"),
            Map.entry("CREAR", "PALABRA_RESERVADA"),
            Map.entry("INSERTAR", "PALABRA_RESERVADA"),
            Map.entry("INSERTAR_FINAL", "PALABRA_RESERVADA"),
            Map.entry("INSERTAR_INICIO", "PALABRA_RESERVADA"),
            Map.entry("INSERTAR_EN_POSICION", "PALABRA_RESERVADA"),
            Map.entry("INSERTARIZQUIERDA", "PALABRA_RESERVADA"),
            Map.entry("INSERTARDERECHA", "PALABRA_RESERVADA"),
            Map.entry("AGREGARNODO", "PALABRA_RESERVADA"),
            Map.entry("APILAR", "PALABRA_RESERVADA"),
            Map.entry("ENCOLAR", "PALABRA_RESERVADA"),
            Map.entry("PUSH", "PALABRA_RESERVADA"),
            Map.entry("ENQUEUE", "PALABRA_RESERVADA"),
            Map.entry("ELIMINAR", "PALABRA_RESERVADA"),
            Map.entry("ELIMINAR_INICIO", "PALABRA_RESERVADA"),
            Map.entry("ELIMINAR_FINAL", "PALABRA_RESERVADA"),
            Map.entry("ELIMINAR_FRENTE", "PALABRA_RESERVADA"),
            Map.entry("ELIMINAR_POSICION", "PALABRA_RESERVADA"),
            Map.entry("ELIMINARNODO", "PALABRA_RESERVADA"),
            Map.entry("DESAPILAR", "PALABRA_RESERVADA"),
            Map.entry("POP", "PALABRA_RESERVADA"),
            Map.entry("DESENCOLAR", "PALABRA_RESERVADA"),
            Map.entry("DEQUEUE", "PALABRA_RESERVADA"),
            Map.entry("BUSCAR", "PALABRA_RESERVADA"),
            Map.entry("TOPE", "PALABRA_RESERVADA"),
            Map.entry("FRENTE", "PALABRA_RESERVADA"),
            Map.entry("VERFILA", "PALABRA_RESERVADA"),
            Map.entry("FRONT", "PALABRA_RESERVADA"),
            Map.entry("CLAVE", "PALABRA_RESERVADA"),
            Map.entry("RECORRER", "PALABRA_RESERVADA"),
            Map.entry("RECORRERADELANTE", "PALABRA_RESERVADA"),
            Map.entry("RECORRERATRAS", "PALABRA_RESERVADA"),
            Map.entry("PREORDEN", "PALABRA_RESERVADA"),
            Map.entry("INORDEN", "PALABRA_RESERVADA"),
            Map.entry("POSTORDEN", "PALABRA_RESERVADA"),
            Map.entry("RECORRIDOPORNIVELES", "PALABRA_RESERVADA"),
            Map.entry("ACTUALIZAR", "PALABRA_RESERVADA"),
            Map.entry("REHASH", "PALABRA_RESERVADA"),
            Map.entry("AGREGARARISTA", "PALABRA_RESERVADA"),
            Map.entry("ELIMINARARISTA", "PALABRA_RESERVADA"),
            Map.entry("VECINOS", "PALABRA_RESERVADA"),
            Map.entry("BFS", "PALABRA_RESERVADA"),
            Map.entry("DFS", "PALABRA_RESERVADA"),
            Map.entry("CAMINOCORTO", "PALABRA_RESERVADA"),
            Map.entry("VACIA", "PALABRA_RESERVADA"),
            Map.entry("LLENA", "PALABRA_RESERVADA"),
            Map.entry("TAMANO", "PALABRA_RESERVADA"),
            Map.entry("EN", "PALABRA_RESERVADA"),
            Map.entry("CON", "PALABRA_RESERVADA"),
            Map.entry("VALOR", "PALABRA_RESERVADA"),
            Map.entry("IF", "PC_IF"),
            Map.entry("ELSE", "PC_ELSE"),
            Map.entry("DO", "PC_DO"),
            Map.entry("WHILE", "PC_WHILE"),
            Map.entry("FOR", "PC_FOR"),
            Map.entry("MOSTRAR", "PALABRA_RESERVADA"),
            Map.entry("INSERTAR_FRENTE", "PALABRA_RESERVADA"),
            Map.entry(
                    "ALTURA", "PROPIEDAD_ARBOL"),
            Map.entry("HOJAS", "PROPIEDAD_ARBOL"),
            Map.entry("NODOS", "PROPIEDAD_ARBOL")
    );

    // Constructor: Recibe todas las piezas del autómata y las guarda para usarlas.
    public Automata(Set<String> estados,
            Set<Character> alfabeto,
            Map<String, Map<Character, String>> transiciones,
            String estadoInicial,
            Set<String> estadosAceptacion) {
        this.estados = estados;
        this.alfabeto = alfabeto;
        this.transiciones = transiciones;
        this.estadoInicial = estadoInicial;
        this.estadosAceptacion = estadosAceptacion;
    }

    /*
     MÉTODO PRINCIPAL: ACEPTAR
     Este método toma una lista de tokens "crudos" (que solo tienen texto y posición) 
     y decide qué son realmente (¿Es una palabra clave? ¿Un número? ¿Un error?).
     */
    public Token[] aceptar(Token[] tokensTokensIniciales) {
        List<Token> resultados = new ArrayList<>();

        // Vamos procesando token por token...
        for (Token tk : tokensTokensIniciales) {
            String lexema = tk.getLexema(); // La palabra escrita (ej: "PILA")
            String lexemaUpper = lexema.toUpperCase(); // Convertimos a mayúsculas para comparar fácil
            int linea = tk.getLinea();

            // PASO 1: FILTRO RÁPIDO
            // Primero preguntamos: "¿Es algo simple como un símbolo, un número o una cadena?"
            // Usamos el método auxiliar 'determinarTipoLexema' para no gastar tiempo en el autómata si no es necesario.
            String tipoAuxiliar = determinarTipoLexema(lexema);

            // Si el método auxiliar encontró que es un símbolo, número o error obvio...
            if (!tipoAuxiliar.startsWith("ERROR") && !tipoAuxiliar.equals("IDENTIFICADOR")) {
                // ...lo guardamos directamente y pasamos al siguiente token.
                resultados.add(new Token(lexema, linea, tk.getColumna(), tipoAuxiliar, "N/A", true));
                continue;
            }

            // PASO 2: SIMULACIÓN DEL AUTÓMATA
            // Si llegamos aquí, es porque parece una Palabra Reservada o un Identificador (nombre de variable).
            // Vamos a recorrer el autómata letra por letra.
            String estadoActual = estadoInicial;
            String ultimoEstadoAceptado = null;
            int ultimoCaracterAceptado = -1;
            boolean esPR = true; // Asumimos que es Palabra Reservada hasta que se demuestre lo contrario

            for (int j = 0; j < lexemaUpper.length(); j++) {
                char simbolo = lexemaUpper.charAt(j); // Leemos la letra actual

                // Buscamos en el mapa: "¿A dónde voy si estoy en 'estadoActual' y leo 'simbolo'?"
                Map<Character, String> transicionesEstado = transiciones.get(estadoActual);

                // Si existe un camino...
                if (transicionesEstado != null && transicionesEstado.containsKey(simbolo)) {
                    estadoActual = transicionesEstado.get(simbolo); // Nos movemos al nuevo estado

                    // ¿Este nuevo estado es un estado final (meta)?
                    if (estadosAceptacion.contains(estadoActual)) {
                        ultimoEstadoAceptado = estadoActual;
                        ultimoCaracterAceptado = j;
                    }
                } // Si NO hay camino para esa letra...
                else {
                    esPR = false; // Ya no puede ser palabra reservada porque el autómata se atascó.
                    break;
                }
            }

            // PASO 3: DECISIÓN FINAL
            // Ya terminamos de leer la palabra. Ahora decidimos qué es.
            // Recalculamos el tipo base por si acaso.
            String tipoFinalAuxiliar = determinarTipoLexema(lexema);

            // Buscamos en el diccionario si la palabra existe tal cual (ej: "IF" -> "PC_IF").
            String tipoFinal = TIPO_POR_PR.getOrDefault(lexemaUpper, tipoFinalAuxiliar);

            String estadoReporte = "N/A";
            boolean reconocido = true;

            // CASO A: Es una Palabra Reservada válida
            // (El autómata llegó al final sin romperse y terminó en un estado de aceptación).
            if (esPR && estadosAceptacion.contains(estadoActual)) {
                estadoReporte = estadoActual;
            } // CASO B: Parece un Identificador (Variable)
            else if (tipoFinalAuxiliar.equals("IDENTIFICADOR")) {

                // Verificamos si, por casualidad, está en el diccionario directo.
                if (TIPO_POR_PR.containsKey(lexemaUpper)) {
                    estadoReporte = lexemaUpper;
                } // CASO C: DETECCIÓN DE ERRORES INTELIGENTE (Levenshtein)
                // Si no es palabra reservada exacta, revisamos si el usuario se equivocó por poquito.
                // Ejemplo: Escribió "PILAS" en vez de "PILA".
                else if (Character.isUpperCase(lexema.charAt(0)) && esCasiPalabraReservada(lexemaUpper)) {
                    tipoFinal = "ERROR_LEXICO_PR_MAL_ESCRITA";
                    reconocido = false;
                    estadoReporte = "N/A";
                } else {
                    // Es un identificador normal y válido (ej: "miVariable").
                    estadoReporte = "N/A";
                }

            } else {
                // CASO D: Es un error léxico (ej: símbolos raros o mal formados).
                tipoFinal = tipoFinalAuxiliar;
                reconocido = false;
                resultados.add(new Token(lexema, linea, tk.getColumna(), tipoFinal, "N/A", reconocido));
                continue;
            }

            // Guardamos el token ya clasificado en la lista de resultados.
            resultados.add(new Token(lexema, linea, tk.getColumna(), tipoFinal, estadoReporte, reconocido));
        }

        return resultados.toArray(new Token[0]);
    }

    // --- MÉTODOS DE UTILIDAD Y VALIDACIÓN ---

    /*
     DISTANCIA DE LEVENSHTEIN:
     Este algoritmo calcula "cuántos cambios" necesito para convertir una palabra en otra.
     Es útil para sugerir correcciones. Si la distancia es 1, significa que solo falló por una letra.
     */
    private static int calcularDistancia(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        // Llenado de la matriz base
        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    // Calculamos el costo mínimo: borrar, insertar o sustituir.
                    dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                            dp[i - 1][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1));
                }
            }
        }
        return dp[s1.length()][s2.length()]; // Retorna el número de diferencias.
    }

    // Verifica si la palabra escrita se parece mucho a alguna palabra reservada (distancia <= 1).
    private static boolean esCasiPalabraReservada(String lexemaUpper) {
        if (lexemaUpper.length() < 3) {
            return false; // Ignora palabras muy cortas
        }
        for (String pr : TIPO_POR_PR.keySet()) {
            // Solo comparamos si tienen longitudes similares para ahorrar tiempo
            if (Math.abs(lexemaUpper.length() - pr.length()) <= 1) {
                if (calcularDistancia(lexemaUpper, pr) == 1) {
                    return true; // ¡Se parece mucho! Probablemente es un error de dedo.
                }
            }
        }
        return false;
    }

    // Valida si es solo números (Regex)
    private static boolean esEntero(String lexema) {
        if (lexema == null) {
            return false;
        }
        return lexema.matches("\\d+");
    }

    // Valida si es una cadena de texto entre comillas (Regex)
    private static boolean esLiteralCadena(String lexema) {
        if (lexema == null) {
            return false;
        }
        return lexema.matches("\"[^\"]*\"");
    }

    // Valida si cumple las reglas de un nombre de variable (letras, números, guion bajo)
    private static boolean esIdentificador(String lexema) {
        if (lexema == null) {
            return false;
        }
        return lexema.matches("[A-Za-z_][A-Za-z0-9_]*");
    }

    /*
     CLASIFICADOR RÁPIDO (SWITCH):
     Este método asigna el tipo a símbolos conocidos directamente.
     Funciona como un filtro previo al autómata.
     */
    public static String determinarTipoLexema(String lexema) {
        switch (lexema) {
            case ";":
                return "DELIMITADOR";
            case "(":
                return "PARENTESIS_IZQ";
            case ")":
                return "PARENTESIS_DER";
            case "[":
                return "CORCHETE_IZQ";
            case "]":
                return "CORCHETE_DER";
            case ",":
                return "COMA";
            case "=":
                return "ASIGNACION";
            case "+":
                return "OP_SUMA";
            case "-":
                return "OP_RESTA";
            case "*":
                return "OP_MULTIPLICACION";
            case "/":
                return "OP_DIVISION";
            case "<":
                return "OP_MENOR_QUE";
            case ">":
                return "OP_MAYOR_QUE";
            case "==":
                return "OP_IGUAL";
            case "!=":
                return "OP_DIFERENTE";
            case "<=":
                return "OP_MENOR_IGUAL";
            case ">=":
                return "OP_MAYOR_IGUAL";
            case "{":
                return "LLAVE_IZQ";
            case "}":
                return "LLAVE_DER";
            // Palabras clave en minúsculas también se detectan aquí
            case "if":
                return "PC_IF";
            case "else":
                return "PC_ELSE";
            case "while":
                return "PC_WHILE";
            case "for":
                return "PC_FOR";
            case "do":
                return "PC_DO";
        }

        if (esEntero(lexema)) {
            return "LITERAL_NUMERICA";
        }

        if (esLiteralCadena(lexema)) {
            return "LITERAL_CADENA";
        }

        if (esIdentificador(lexema)) {
            return "IDENTIFICADOR";
        }

        // Detección de errores específicos
        if (lexema.startsWith("\"") && !esLiteralCadena(lexema)) {
            return "ERROR_CADENA_INCOMPLETA";
        }

        if (lexema.length() == 1) {
            return "ERROR_SIMBOLO_INVALIDO";
        }

        return "ERROR_TOKEN_MALFORMADO";
    }

    public boolean esIdentificadorValido(String lexema) {
        return lexema != null && lexema.matches("[a-zA-Z][a-zA-Z0-9_]*");
    }

    // Retorna el diccionario de palabras reservadas
    public static Map<String, String> getPalabrasReservadas() {
        return TIPO_POR_PR;
    }
}
