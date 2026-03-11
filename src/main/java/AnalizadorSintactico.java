// Importamos las librerías necesarias de Java para manejar listas (List) y conjuntos de datos únicos (Set).

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/*
 ESTA CLASE ES EL "ANALIZADOR SINTÁCTICO" (PARSER).
 Recibe las palabras (tokens) que encontró el Analizador Léxico
 y verifica si el orden de esas palabras tiene sentido según las reglas del lenguaje.
 
 Además, construye un "Árbol de Sintaxis Abstracta" (AST), que es una representación jerárquica y ordenada del código.
 */
public class AnalizadorSintactico {

    // --- ATRIBUTOS (VARIABLES GLOBALES DE LA CLASE) ---
    // Aquí guardamos la lista de palabras (tokens) que vamos a analizar.
    private final Token[] tokens;

    // Este entero actúa como un "cursor" o "dedo apuntador". Nos dice en qué posición de la lista de tokens estamos.
    private int actual;

    // Una lista de textos para ir guardando paso a paso cómo se construye el árbol (útil para ver qué hizo el programa).
    private final List<String> logDerivacion;

    // Una lista para anotar todos los errores gramaticales que encontremos.
    private final List<String> errores;

    // Estas tablas son para guardar variables y errores de forma estructurada (para uso externo o visual).
    private TablaSimbolos tablaSimbolos;
    private TablaErrores tablaErrores;

    // --- EXCEPCIÓN PERSONALIZADA ---
    /*
     Creamos nuestro propio tipo de error. Esto nos sirve para detener el análisis de golpe
     cuando encontramos algo que no tiene sentido y poder reportarlo limpiamente.
     */
    private static class ParserException extends RuntimeException {

        public ParserException(String message) {
            super(message);
        }
    }

    // --- CONSTRUCTOR ---
    /*
     Este es el método que "nace" cuando creamos una instancia del analizador.
     Inicializa todas las listas vacías y pone el cursor (actual) en 0 (el principio).
     */
    public AnalizadorSintactico(Token[] tokens, TablaSimbolos ts, TablaErrores te) {
        this.tokens = tokens;
        this.tablaSimbolos = ts;
        this.tablaErrores = te;
        this.actual = 0;
        this.logDerivacion = new ArrayList<>();
        this.errores = new ArrayList<>();
    }

    // --- GETTERS (MÉTODOS PARA OBTENER INFORMACIÓN) ---
    // Permiten que otras clases (como la interfaz gráfica) saquen el reporte del árbol y de los errores.
    public List<String> getArbolDerivacion() {
        return logDerivacion;
    }

    public List<String> getErrores() {
        return errores;
    }

    // --- MÉTODO PRINCIPAL: ANALIZAR ---
    /*
    Inicia todo el proceso.
     1. Limpia los registros anteriores.
     2. Crea el nodo "RAÍZ" del árbol.
     3. Llama a la regla "programa()" para empezar a leer el código.
     4. Si hay errores críticos, los atrapa (catch).
     5. Al final (finally), siempre dibuja el árbol visualmente.
     */
    public NodoAST analizar() {
        logDerivacion.clear();
        errores.clear();
        this.actual = 0;

        // Creamos la raíz del árbol que contendrá todo el código.
        NodoAST raiz = new NodoAST("PROGRAMA", "RAIZ", 0);

        try {
            // Validación de seguridad: Si el código empieza con una llave de cierre '}', es un error obvio.
            if (!esFin() && checar("}")) {
                throw error("Error de flujo: Se encontró '}' sin apertura previa.", 202);
            }

            //  Llamamos a la regla que lee todo el programa.
            NodoAST nodoPrograma = programa();

            // Si obtuvimos un programa válido (o parcialmente válido), lo pegamos a la raíz.
            if (nodoPrograma != null) {
                raiz.agregarHijo(nodoPrograma);
            }

            // Validación final: Si sobró una llave de cierre '}', avisamos.
            if (!esFin() && checar("}")) {
                throw error("Error de flujo: Llave de cierre '}' inesperada al final.", 202);
            }

        } catch (ParserException e) {
            // Si el error fue de sintaxis (nuestro error personalizado).
            errores.add(e.getMessage());
        } catch (Exception e) {
            // Si pasó algo muy grave inesperado (bug de programación o error crítico).
            errores.add("DSL(999) Error crítico: " + e.getMessage());
        } finally {
            // Pase lo que pase, generamos el dibujo en texto del árbol para mostrarlo.
            dibujarArbolEnLog(raiz, "", true);
        }

        return raiz;
    }

    // --- REGLAS GRAMATICALES ---

    /*
     REGLA: PROGRAMA
     Un programa no es más que una lista de sentencias (instrucciones) una tras otra.
     Este método usa un ciclo 'while' para leer instrucción por instrucción hasta que se acabe el archivo.
     */
    private NodoAST programa() {
        NodoAST nodoBloque = new NodoAST("BLOQUE_CODIGO", "Lista Sentencias", 0);

        while (!esFin()) {
            // Si encontramos una llave de cierre '}', significa que terminó un bloque (como el fin de un IF o WHILE).
            // Rompemos el ciclo para regresar el control al método anterior.
            if (checar("}")) {
                break;
            }

            try {
                // Intentamos leer una sentencia completa.
                NodoAST sent = sentencia();
                if (sent != null) {
                    nodoBloque.agregarHijo(sent);
                }
            } catch (ParserException e) {
                /* RECUPERACIÓN DE ERRORES (MODO PÁNICO):
                 Si una línea tiene error, no queremos detener todo el compilador.
                 1. Guardamos el error.
                 2. Creamos un nodo visual de error en el árbol.
                 3. Llamamos a 'sincronizar()' que salta texto hasta encontrar un punto y coma
                    o una palabra conocida para intentar seguir analizando la siguiente línea.
                 */
                errores.add(e.getMessage());

                NodoAST nodoError = new NodoAST("!!! ERROR SINTÁCTICO !!!", "ERROR", tokenActual().getLinea());
                nodoError.agregarHijo(new NodoAST(e.getMessage(), "DETALLE", tokenActual().getLinea()));
                nodoBloque.agregarHijo(nodoError);

                sincronizar();
            }
        }
        return nodoBloque;
    }

    /*
     REGLA: SENTENCIA (EL DISTRIBUIDOR DE TRÁFICO)
     Este método mira la primera palabra de la línea actual y decide a qué método llamar.
     Si la palabra es "IF", llama a flujoIf(). Si es "CREAR", llama a declaracion(), etc.
     */
    private NodoAST sentencia() {

        Token token = tokenActual();
        String lexema = token.getLexema().toUpperCase();
        String tipo = token.getTipoToken();

        if (lexema.equals("CREAR")) {
            return declaracion();
        } else if (lexema.equals("IF")) {
            return flujoIf();
        } else if (lexema.equals("WHILE")) {
            return bucleWhile();
        } else if (lexema.equals("FOR")) {
            return bucleFor();
        } else if (lexema.equals("DO")) {
            return bucleDoWhile();
        } else if (lexema.equals("MOSTRAR")) {
            return salida();
        } else if (esPropiedad(lexema)) {   // VACIA o LLENA
            return propiedad();
        } else if (esVerboOperacion(lexema)) {
            return operacionEstructura();
        } else if (lexema.equals("BORRAR")) {
            return borrar();
        } else if (tipo.equals("IDENTIFICADOR")) {
            return asignacion();
        } else {
            throw error("Instrucción no reconocida o inválida: " + lexema, 203);
        }
    }


    /*
     REGLA: DECLARACIÓN
     Maneja la creación de variables. Estructura esperada: CREAR [TIPO] [NOMBRE];
     También soporta inicialización: CREAR ENTERO X = 10;
     */
    private NodoAST declaracion() {

        int linea = tokenActual().getLinea();
        consumir("CREAR");

        String tipoDato = tokenActual().getLexema().toUpperCase();
        if (!esTipoValido(tipoDato)) {
            throw error("Tipo de dato desconocido: " + tipoDato, 204);
        }
        avanzar();

        String nombreVar = tokenActual().getLexema();
        consumir("IDENTIFICADOR");

        NodoAST nodoDecl = new NodoAST("DECLARACION", "Declaracion", linea);

        nodoDecl.agregarHijo(new NodoAST(tipoDato, "TIPO", linea));
        nodoDecl.agregarHijo(new NodoAST(nombreVar, "ID", linea));

        if (coincide("TAMANO")) {

            if (!(tokenActual().getTipoToken().contains("LITERAL_NUMERICA"))) {
                throw error("Se esperaba un número después de TAMANO.", 205);
            }

            String tam = tokenActual().getLexema();
            avanzar();

            nodoDecl.agregarHijo(new NodoAST(tam, "TAMANO", linea));
        }

        // Inicialización opcional para variables
        if (checar("=")) {
            consumir("=");
            nodoDecl.agregarHijo(expresion());
        }

        consumir(";");

        return nodoDecl;
    }

    private NodoAST operacionEstructura() {

        int linea = tokenActual().getLinea();
        String verbo = tokenActual().getLexema().toUpperCase();

        NodoAST nodoOp = new NodoAST(verbo, "OPERACION", linea);

        consumir(verbo);

        // =====================================
        // AGREGARNODO clave valor EN grafo
        // =====================================
        if (verbo.equals("AGREGARNODO")) {

            nodoOp.agregarHijo(expresion()); // clave
            nodoOp.agregarHijo(expresion()); // valor
        } // =====================================
        // ELIMINARNODO clave EN grafo
        // =====================================
        else if (verbo.equals("ELIMINARNODO")) {

            nodoOp.agregarHijo(expresion()); // clave
        } // =====================================
        // AGREGARARISTA origen destino EN grafo
        // ELIMINARARISTA origen destino EN grafo
        // =====================================
        else if (verbo.equals("AGREGARARISTA") || verbo.equals("ELIMINARARISTA")) {

            nodoOp.agregarHijo(expresion()); // origen
            nodoOp.agregarHijo(expresion()); // destino
        } // =====================================
        // INSERTAR clave valor EN tabla_hash
        // SOLO NUMEROS
        // =====================================
        else if (verbo.equals("INSERTAR")) {

            NodoAST clave = expresion();

            if (!clave.getTipo().equalsIgnoreCase("NUMERO")) {
                throw error("La clave de TABLA_HASH debe ser un NUMERO.", 208);
            }

            nodoOp.agregarHijo(clave);

            if (checar("EN")) {
                throw error("Falta el valor para insertar.", 205);
            }

            NodoAST valor = expresion();

            if (!valor.getTipo().equalsIgnoreCase("NUMERO")) {
                throw error("El valor de TABLA_HASH debe ser un NUMERO.", 209);
            }

            nodoOp.agregarHijo(valor);
        } // =====================================
        // OPERACIONES GENERALES (1 parametro)
        // =====================================
        else if (!esVerboSinParametros(verbo)) {

            if (checar("EN")) {
                throw error("Falta el valor antes de 'EN'.", 205);
            }

            nodoOp.agregarHijo(expresion());
        }

        // =====================================
        // PALABRA EN
        // =====================================
        if (!checar("EN")) {
            throw error("Se esperaba la palabra 'EN'.", 206);
        }

        consumir("EN");

        // =====================================
        // IDENTIFICADOR DE ESTRUCTURA
        // =====================================
        if (!checar("IDENTIFICADOR")) {
            throw error("Se esperaba el identificador de la estructura.", 207);
        }

        String nombre = tokenActual().getLexema();

        NodoAST nodoEstructura = new NodoAST(nombre, "ID_ESTRUCTURA", linea);
        nodoOp.agregarHijo(nodoEstructura);

        consumir("IDENTIFICADOR");

        // =====================================
        // ;
        // =====================================
        consumir(";");

        return nodoOp;
    }

    /*
     REGLA: OPERACIONES DE ESTRUCTURA
     Esta es la lógica compleja para comandos como APILAR, INSERTAR, AGREGARARISTA.
     Tiene lógica especial para asegurarse de que el usuario no olvide poner los valores.
     */
    // Regla: ASIGNACIÓN SIMPLE (Ej: x = 5 + 3;)
    private NodoAST asignacion() {
        int linea = tokenActual().getLinea();
        String id = tokenActual().getLexema();

        consumir("IDENTIFICADOR");
        consumir("=");

        NodoAST expr = expresion(); // Evaluamos lo que hay a la derecha del igual
        consumir(";");

        NodoAST nodoAsig = new NodoAST("=", "ASIGNACION", linea);
        nodoAsig.agregarHijo(new NodoAST(id, "ID_VAR", linea));
        nodoAsig.agregarHijo(expr);
        return nodoAsig;
    }

    // Regla: MOSTRAR (Imprimir en pantalla)
    private NodoAST salida() {
        int linea = tokenActual().getLinea();
        consumir("MOSTRAR");
        NodoAST nodoMostrar = new NodoAST("MOSTRAR", "Salida", linea);

        // Soporte especial para mostrar vecinos de un grafo
        if (checar("VECINOS")) {
            consumir("VECINOS");
            NodoAST nodoVecinos = new NodoAST("VECINOS", "OpGrafo", linea);
            nodoVecinos.agregarHijo(expresion()); // El nodo del cual queremos vecinos
            consumir("EN");
            String idG = tokenActual().getLexema();
            consumir("IDENTIFICADOR");
            nodoVecinos.agregarHijo(new NodoAST(idG, "GRAFO", linea));
            nodoMostrar.agregarHijo(nodoVecinos);
        } else {
            // Mostrar normal (un número, variable o texto)
            nodoMostrar.agregarHijo(expresion());
        }

        consumir(";");
        return nodoMostrar;
    }

    // --- ESTRUCTURAS DE CONTROL (IF, WHILE, FOR) ---
    // Estas funciones manejan bloques de código anidados (código dentro de llaves {})
    private NodoAST flujoIf() {

        int linea = tokenActual().getLinea();

        consumir("IF");
        consumir("(");

        NodoAST condicion = condicion();

        consumir(")");
        consumir("{");

        NodoAST bloque = sentencia();

        consumir("}");

        NodoAST nodoIf = new NodoAST("IF", "CONTROL", linea);
        nodoIf.agregarHijo(condicion);
        nodoIf.agregarHijo(bloque);

        return nodoIf;
    }

    private NodoAST bucleWhile() {
        int l = tokenActual().getLinea();
        consumir("WHILE");
        consumir("(");
        NodoAST cond = condicion();
        consumir(")");

        consumir("{");
        NodoAST cuerpo = programa(); // Cuerpo del ciclo
        consumir("}");

        NodoAST nodo = new NodoAST("WHILE", "Bucle", l);
        nodo.agregarHijo(cond);
        nodo.agregarHijo(cuerpo);
        return nodo;
    }

    private NodoAST bucleDoWhile() {
        int l = tokenActual().getLinea();
        consumir("DO");
        consumir("{");
        NodoAST cuerpo = programa();
        consumir("}");

        consumir("WHILE");
        consumir("(");
        NodoAST cond = condicion();
        consumir(")");
        consumir(";");

        NodoAST nodo = new NodoAST("DO_WHILE", "Bucle", l);
        nodo.agregarHijo(cuerpo);
        nodo.agregarHijo(cond);
        return nodo;
    }

    private NodoAST bucleFor() {
        int l = tokenActual().getLinea();
        consumir("FOR");
        consumir("(");

        NodoAST init = asignacionParaFor(); // Parte 1: i = 0
        consumir(";");
        NodoAST cond = condicion();         // Parte 2: i < 10
        consumir(";");
        NodoAST step = asignacionParaFor(); // Parte 3: i = i + 1

        consumir(")");
        consumir("{");
        NodoAST cuerpo = programa();        // Bloque a repetir
        consumir("}");

        NodoAST nodo = new NodoAST("FOR", "Bucle", l);
        nodo.agregarHijo(init);
        nodo.agregarHijo(cond);
        nodo.agregarHijo(step);
        nodo.agregarHijo(cuerpo);
        return nodo;
    }

    // Método especial para la asignación dentro del FOR, ya que no lleva ';' al final.
    private NodoAST asignacionParaFor() {
        int l = tokenActual().getLinea();
        String id = tokenActual().getLexema();

        consumir("IDENTIFICADOR");
        consumir("=");
        NodoAST expr = expresion();

        NodoAST n = new NodoAST("=", "ACTUALIZACION", l);
        n.agregarHijo(new NodoAST(id, "ID", l));
        n.agregarHijo(expr);
        return n;
    }

    // --- EXPRESIONES MATEMÁTICAS Y LÓGICAS ---
    /*
     Aquí manejamos la "precedencia de operadores".
     Funciona en capas: 
     1. Condicion (>, <, ==)
     2. Expresion (+, -) -> Menor prioridad, se procesa al último
     3. Termino (*, /) -> Mayor prioridad que suma
     4. Factor (Números, Parentesis) -> Máxima prioridad
     */
    private NodoAST condicion() {

        Token token = tokenActual();
        String lexema = token.getLexema().toUpperCase();

        // CASO 1: Propiedades (VACIA EN p1)
        if (esPropiedad(lexema)) {

            int linea = token.getLinea();

            consumir(lexema);
            consumir("EN");

            String id = tokenActual().getLexema();
            consumir("IDENTIFICADOR");

            NodoAST nodo = new NodoAST(lexema, "CONDICION_PROPIEDAD", linea);
            nodo.agregarHijo(new NodoAST(id, "ID_ESTRUCTURA", linea));

            return nodo;
        }

        // CASO 2: Condición relacional
        NodoAST izquierda = expresion();

        if (checar("<") || checar(">") || checar("==") || checar("!=") || checar("<=") || checar(">=")) {

            String op = tokenActual().getLexema();
            int linea = tokenActual().getLinea();
            avanzar();

            NodoAST derecha = expresion();

            NodoAST nodo = new NodoAST(op, "CONDICION", linea);
            nodo.agregarHijo(izquierda);
            nodo.agregarHijo(derecha);

            return nodo;
        }

        return izquierda;
    }

    private NodoAST expresion() {
        NodoAST izq = termino(); // Primero resolvemos multiplicaciones (términos)
        // Luego resolvemos sumas y restas
        while (checar("+") || checar("-")) {
            String op = tokenActual().getLexema();
            int l = tokenActual().getLinea();
            avanzar();
            NodoAST der = termino();
            NodoAST n = new NodoAST(op, "OPERACION", l);
            n.agregarHijo(izq);
            n.agregarHijo(der);
            izq = n;
        }
        return izq;
    }

    private NodoAST termino() {
        NodoAST izq = factor(); // Primero resolvemos factores (números o paréntesis)
        // Luego resolvemos multiplicaciones y divisiones
        while (checar("*") || checar("/")) {
            String op = tokenActual().getLexema();
            int l = tokenActual().getLinea();
            avanzar();
            NodoAST der = factor();
            NodoAST n = new NodoAST(op, "OPERACION", l);
            n.agregarHijo(izq);
            n.agregarHijo(der);
            izq = n;
        }
        return izq;
    }

    // El átomo de una expresión: un número, una variable o algo entre paréntesis.
    private NodoAST factor() {
        String lexema = tokenActual().getLexema().toUpperCase();
        String tipo = tokenActual().getTipoToken();
        int linea = tokenActual().getLinea();

        // Valores Booleanos
        if (lexema.equals("VERDADERO") || lexema.equals("FALSO")) {
            avanzar();
            return new NodoAST(lexema, "BOOLEANO", linea);
        }

        // Propiedades de Estructuras (ej: TOPE EN PILA)
        if (esPropiedad(lexema)) {
            String prop = lexema;
            consumir(prop);

            NodoAST nodoProp = new NodoAST(prop, "PROPIEDAD", linea);

            // Caso especial para VECINOS
            if (prop.equals("VECINOS")) {
                nodoProp.agregarHijo(expresion());
            }

            consumir("EN");
            String idEst = tokenActual().getLexema();
            consumir("IDENTIFICADOR");
            nodoProp.agregarHijo(new NodoAST(idEst, "ID_ESTRUCTURA", linea));
            return nodoProp;
        }

        // Paréntesis: ( 5 + 3 ) -> Reinicia la evaluación de expresión adentro
        if (lexema.equals("(")) {
            consumir("(");
            NodoAST e = expresion();
            consumir(")");
            return e;
        }

        // Identificadores (Variables)
        if (tipo.equals("IDENTIFICADOR")) {
            String val = tokenActual().getLexema();
            avanzar();
            return new NodoAST(val, "IDENTIFICADOR", linea);
        }
        // Números
        if (tipo.contains("NUMERO") || tipo.contains("LITERAL_NUMERICA")) {
            String val = tokenActual().getLexema();
            avanzar();
            return new NodoAST(val, "NUMERO", linea);
        }
        // Cadenas de texto
        if (tipo.contains("CADENA") || tipo.contains("TEXTO") || tipo.equals("LITERAL_CADENA")) {
            String val = tokenActual().getLexema();
            avanzar();
            return new NodoAST(val, "CADENA", linea);
        }

        throw error("Factor inválido en expresión: " + lexema, 207);
    }

    // --- UTILIDADES Y NAVEGACIÓN (HERRAMIENTAS INTERNAS) ---
    // Método "Exigente": Verifica que el token actual sea X. Si lo es, avanza. Si no, lanza error.
    private void consumir(String esperado) {
        if (checar(esperado)) {
            avanzar();
        } else {
            String encontrado = esFin() ? "FIN_DE_ARCHIVO" : tokenActual().getLexema();
            throw error("Se esperaba '" + esperado + "', pero se encontró '" + encontrado + "'", 203);
        }
    }

    // Método "Observador": Revisa si el token actual es X, pero no avanza el cursor.
    private boolean checar(String s) {
        if (esFin()) {
            return false;
        }
        Token t = tokenActual();
        // Permitir que 'CADENA' acepte también 'LITERAL_CADENA'
        if (s.equals("CADENA")) {
            return t.getTipoToken().equals("CADENA") || t.getTipoToken().equals("LITERAL_CADENA");
        }
        return t.getLexema().equalsIgnoreCase(s) || t.getTipoToken().equals(s);
    }

    // Método "Oportunista": Si el token es X, lo consume y retorna true. Si no, retorna false (no lanza error).
    private boolean coincide(String s) {
        if (checar(s)) {
            avanzar();
            return true;
        }
        return false;
    }

    // Mueve el cursor a la siguiente palabra.
    private void avanzar() {
        if (!esFin()) {
            actual++;
        }
    }

    // Nos dice si ya nos acabamos la lista de tokens.
    private boolean esFin() {
        return actual >= tokens.length;
    }

    // Obtiene el objeto Token actual sin riesgo de desbordar el arreglo.
    private Token tokenActual() {
        if (actual >= tokens.length) {
            return tokens[tokens.length - 1];
        }
        return tokens[actual];
    }

    // Genera la excepción con formato bonito.
    private ParserException error(String m, int codigo) {
        int linea = tokenActual().getLinea();
        return new ParserException("DSL(" + codigo + ") [Línea " + linea + "]: " + m);
    }

    // --- RECUPERACIÓN DE ERRORES (MODO PÁNICO) ---
    /*
     Cuando ocurre un error, este método "come" tokens a lo loco hasta encontrar
     un punto y coma (;) o una palabra clave de inicio (IF, WHILE, etc.).
     Esto permite que el compilador siga revisando las siguientes líneas en lugar de detenerse totalmente.
     */
    private void sincronizar() {
        avanzar();
        while (!esFin()) {
            if (tokenActual().getLexema().equals(";")) {
                avanzar();
                return;
            }
            // Si encontramos una palabra reservada de inicio, asumimos que hemos recuperado el flujo
            String lex = tokenActual().getLexema().toUpperCase();
            if (Set.of("CREAR", "IF", "WHILE", "FOR", "DO", "MOSTRAR", "INSERTAR", "APILAR", "ENCOLAR", "ELIMINAR", "BFS", "DFS", "BORRAR").contains(lex)) {
                return;
            }
            avanzar();
        }
    }

    // --- GENERADOR VISUAL DE ÁRBOL ---
    /*
     Este método usa recursividad para dibujar el árbol bonito con líneas.
     Se llama a sí mismo para dibujar a los hijos, agregando indentación (│   )
     */
    private void dibujarArbolEnLog(NodoAST nodo, String prefijo, boolean esUltimo) {
        if (nodo == null) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(prefijo).append(esUltimo ? "└── " : "├── ").append(nodo.getValor());
        logDerivacion.add(sb.toString());

        List<NodoAST> hijos = nodo.getHijos();
        for (int i = 0; i < hijos.size(); i++) {
            dibujarArbolEnLog(hijos.get(i), prefijo + (esUltimo ? "    " : "│   "), i == hijos.size() - 1);
        }
    }

    // --- LISTAS DE PALABRAS RESERVADAS (SETS) ---
    // Usamos Sets porque buscar en ellos es extremadamente rápido.
    private boolean esVerboOperacion(String s) {
        return Set.of(
                // Lista de todos los verbos que modifican estructuras
                "INSERTAR", "INSERTAR_FINAL", "INSERTAR_INICIO", "INSERTAR_EN_POSICION",
                "INSERTARIZQUIERDA", "INSERTARDERECHA", "AGREGARNODO", "APILAR", "ENCOLAR",
                "PUSH", "ENQUEUE", "ELIMINAR", "ELIMINAR_INICIO", "ELIMINAR_FINAL",
                "ELIMINAR_FRENTE", "ELIMINAR_POSICION", "ELIMINARNODO", "DESAPILAR", "POP",
                "DESENCOLAR", "DEQUEUE", "BUSCAR", "RECORRER", "BFS", "DFS", "AGREGARARISTA",
                "ELIMINARARISTA", "ACTUALIZAR", "REHASH", "CAMINOCORTO",
                "INSERTAR_FRENTE", "VER_FILA", "VERFILA", "PREORDEN", "INORDEN", "POSTORDEN", "RECORRIDOPORNIVELES",
                "RECORRERADELANTE", "RECORRERATRAS"
        ).contains(s);
    }

    private boolean esVerboSinParametros(String s) {
        return Set.of(
                "DESAPILAR", "POP",
                "DESENCOLAR", "DEQUEUE",
                "ELIMINAR_INICIO", "ELIMINAR_FINAL", "ELIMINAR_FRENTE",
                "RECORRER", "RECORRERADELANTE", "RECORRERATRAS",
                "BFS", "DFS",
                "PREORDEN", "INORDEN", "POSTORDEN",
                "RECORRIDOPORNIVELES",
                "VACIA",
                "VER_FILA", "VERFILA",
                "REHASH"
        ).contains(s);
    }

    private boolean esPropiedad(String s) {
        // Lista de propiedades que devuelven un valor (altura, tamaño, tope, etc.)
        return Set.of(
                "TOPE", "FRENTE", "FRONT", "PEEK", "CLAVE", "TAMANO", "ALTURA",
                "HOJAS", "NODOS", "VECINOS",
                "VACIA", "LLENA", "GRADO", "VER_FILA", "VERFILA", "PREORDEN", "INORDEN", "POSTORDEN", "RECORRIDOPORNIVELES"
        ).contains(s);
    }

    private boolean esTipoValido(String s) {
        // Lista de tipos de datos permitidos para declarar variables
        return Set.of(
                "PILA", "COLA", "BICOLA", "LISTA_ENLAZADA", "LISTA_CIRCULAR", "LISTA_DOBLE_ENLAZADA",
                "ARBOL_BINARIO", "TABLA_HASH", "GRAFO", "PILA_CIRCULAR", "NUMERO", "TEXTO"
        ).contains(s);
    }

    private NodoAST propiedad() {

        int linea = tokenActual().getLinea();
        String prop = tokenActual().getLexema().toUpperCase();

        consumir(prop);
        consumir("EN");

        String id = tokenActual().getLexema();
        consumir("IDENTIFICADOR");

        consumir(";");

        NodoAST nodo = new NodoAST(prop, "PROPIEDAD", linea);
        nodo.agregarHijo(new NodoAST(id, "ID_ESTRUCTURA", linea));

        return nodo;
    }

    private NodoAST borrar() {

        int linea = tokenActual().getLinea();

        consumir("BORRAR");

        if (!checar("IDENTIFICADOR")) {
            throw error("Se esperaba un identificador después de BORRAR.", 301);
        }

        String id = tokenActual().getLexema();
        consumir("IDENTIFICADOR");

        consumir(";");

        NodoAST nodo = new NodoAST("BORRAR", "OPERACION", linea);
        nodo.agregarHijo(new NodoAST(id, "ID_ESTRUCTURA", linea));

        return nodo;
    }

}
