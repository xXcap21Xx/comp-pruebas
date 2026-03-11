import java.util.List;

/**
 * ANALIZADOR SEMÁNTICO - DSL DE ESTRUCTURAS DE DATOS
 * Este componente valida que la lógica del programa sea correcta,
 * verificando tipos, existencia de variables y reglas de estructuras.
 */
public class AnalizadorSemantico {

    private final TablaSimbolos tablaSimbolos;
    private final TablaErrores tablaErrores;

    public AnalizadorSemantico(TablaSimbolos ts, TablaErrores te) {
        this.tablaSimbolos = ts;
        this.tablaErrores = te;
    }

    public void analizar(NodoAST raiz) {
        if (raiz == null) {
            return;
        }
        tablaSimbolos.limpiar(); // Limpiamos la memoria antes de empezar
        evaluarNodo(raiz);
    }

    private void evaluarNodo(NodoAST nodo) {
        if (nodo == null) {
            return;
        }

        String tipoNodo = nodo.getTipo() != null ? nodo.getTipo().toUpperCase().trim() : "";
        String valorNodo = nodo.getValor() != null ? nodo.getValor().trim() : "";
        int linea = nodo.getLinea();

        // ==========================================================
        // 1. GESTIÓN DE ÁMBITOS (BLOQUES DE CÓDIGO)
        // ==========================================================
        boolean esBloque = tipoNodo.equals("IF") || tipoNodo.equals("WHILE")
                || tipoNodo.equals("FOR") || tipoNodo.equals("DO") || tipoNodo.equals("BLOQUE");
        if (esBloque) {
            tablaSimbolos.entrarAmbito();
        }

        // ==========================================================
        // 2. DECLARACIÓN DE VARIABLES
        // ==========================================================
        if (tipoNodo.equals("DECLARACION")) {
            String tipoVar = nodo.getHijos().get(0).getValor().toUpperCase().trim();
            String nombreVar = nodo.getHijos().get(1).getValor().trim();
            NodoAST nodoValor = nodo.getHijos().size() > 2 ? nodo.getHijos().get(2) : null;

            // --- DETECTA ERROR 2: Variable redeclarada ---
            // Ejemplo DSL: CREAR PILA miPila; CREAR PILA miPila;
            if (!tablaSimbolos.existe(nombreVar)) {
                Object valorInicial = "Vacio";

                if (nodoValor != null) {
                    String tipoValor = evaluarTipoExpresion(nodoValor);
                    valorInicial = nodoValor.getValor();

                    // --- DETECTA ERROR 3: Incompatibilidad de tipos ---
                    // Ejemplo DSL: CREAR NUMERO numero1 = "Esto es texto";
                    if (!tipoVar.equals(tipoValor) && !tipoValor.equals("DESCONOCIDO")) {
                        tablaErrores.reporte(linea, "Semántico",
                                "DSL(303) Incompatibilidad: No puedes asignar " + tipoValor + " a " + tipoVar);
                    }
                }
                tablaSimbolos.insertar(nombreVar, tipoVar, valorInicial);
            } else {
                tablaErrores.reporte(linea, "Semántico",
                        "DSL(302) La variable '" + nombreVar + "' ya fue declarada.");
            }
            return;
        }

        // ==========================================================
        // 3. USO DE VARIABLES (VALIDACIÓN DE EXISTENCIA Y ESTADO)
        // ==========================================================
        if (tipoNodo.equals("ID_ESTRUCTURA") || tipoNodo.equals("ID_VAR") || tipoNodo.equals("ID") || tipoNodo.equals("IDENTIFICADOR")) {
            String palabra = valorNodo.toUpperCase();
            
            // Ignorar palabras reservadas que el parser a veces confunde con IDs
            if (palabra.equals("TAMANO") || palabra.equals("LLENA") || palabra.equals("VACIA") || palabra.equals("TOPE") || palabra.equals("FRENTE")) {
                return;
            }

            // --- DETECTA ERROR 1 y 19: Variable no declarada ---
            // Ejemplo DSL: MOSTRAR miVariableInexistente; 
            // Ejemplo DSL: MOSTRAR VACIA EN estructuraFantasma;
            if (!tablaSimbolos.existe(valorNodo)) {
                tablaErrores.reporte(linea, "Semántico", "DSL(301) La variable '" + valorNodo + "' no existe.");
            } else {
                TablaSimbolos.Simbolo sim = tablaSimbolos.getSimbolo(valorNodo);

                // --- DETECTA ERROR 14: Variable usada sin valor ---
                // Ejemplo DSL: CREAR NUMERO numSinValor; MOSTRAR numSinValor;
                if (sim != null && (sim.tipo.equals("NUMERO") || sim.tipo.equals("TEXTO")) && sim.valor.equals("Vacio")) {
                    tablaErrores.reporte(linea, "Semántico", "DSL(306) La variable '" + valorNodo + "' se usa sin valor.");
                }
            }
        }

        // ==========================================================
        // 4. OPERACIONES SOBRE ESTRUCTURAS (PILA, COLA, GRAFO, ETC)
        // ==========================================================
        if (tipoNodo.equals("OPERACION") || tipoNodo.equals("PROPIEDAD")) {
            String verbo = valorNodo.toUpperCase();
            String nombreEstructura = "";

            if (nodo.getHijos() != null) {
                for (NodoAST hijo : nodo.getHijos()) {
                    String etiqueta = hijo.getTipo() != null ? hijo.getTipo().toUpperCase().trim() : "";
                    if (etiqueta.equals("ID_ESTRUCTURA") || etiqueta.equals("ID")) {
                        nombreEstructura = hijo.getValor().trim();
                        break;
                    }
                }
            }

            if (!nombreEstructura.isEmpty() && tablaSimbolos.existe(nombreEstructura)) {
                TablaSimbolos.Simbolo sim = tablaSimbolos.getSimbolo(nombreEstructura);
                String tipo = sim.tipo.toUpperCase();

                // --- DETECTA ERRORES 4, 5, 6, 7, 15, 16, 17, 20: Comandos inválidos ---
                // Ejemplo DSL: MOSTRAR TOPE EN miCola; (Tope es de Pila)
                // Ejemplo DSL: BFS EN miPila4; (BFS es de Grafo)
                validarCompatibilidad(verbo, tipo, nombreEstructura, linea);

                // --- REGLAS PARA TABLA_HASH ---
                if (tipo.equals("TABLA_HASH")) {
                    String clave = (nodo.getHijos() != null && !nodo.getHijos().isEmpty()) ? nodo.getHijos().get(0).getValor() : "";

                    // --- DETECTA ERROR 8: Clave duplicada ---
                    // Ejemplo DSL: INSERTAR 100 1000 EN miHash; INSERTAR 100 2000 EN miHash;
                    if (verbo.equals("INSERTAR")) {
                        if (sim.valoresInsertados.contains(clave)) {
                            tablaErrores.reporte(linea, "Semántico", "DSL(307) La clave '" + clave + "' ya existe.");
                        } else {
                            sim.valoresInsertados.add(clave);
                        }
                    }

                    // --- DETECTA ERROR 9: Eliminar clave inexistente ---
                    // Ejemplo DSL: ELIMINAR 99 EN miHash2;
                    if (verbo.equals("ELIMINAR")) {
                        if (!sim.valoresInsertados.contains(clave)) {
                            tablaErrores.reporte(linea, "Semántico", "DSL(308) No existe la clave '" + clave + "' para eliminar.");
                        } else {
                            sim.valoresInsertados.remove(clave);
                        }
                    }

                    // --- DETECTA ERROR 10: Buscar clave inexistente ---
                    // Ejemplo DSL: BUSCAR 999 EN miHash;
                    if (verbo.equals("BUSCAR") && !sim.valoresInsertados.contains(clave)) {
                        tablaErrores.reporte(linea, "Semántico", "DSL(309) La clave '" + clave + "' no existe.");
                    }
                }

                // --- REGLAS PARA GRAFOS ---
                if (tipo.equals("GRAFO")) {
                    String c1 = (nodo.getHijos() != null && nodo.getHijos().size() >= 1) ? nodo.getHijos().get(0).getValor() : "";
                    String c2 = (nodo.getHijos() != null && nodo.getHijos().size() >= 2) ? nodo.getHijos().get(1).getValor() : "";
                    String arista = c1 + "-" + c2;

                    // --- DETECTA ERROR 11: Eliminar nodo inexistente ---
                    // Ejemplo DSL: ELIMINARNODO Z EN miGrafo;
                    if (verbo.equals("ELIMINARNODO") && !sim.nodosGrafo.contains(c1)) {
                        tablaErrores.reporte(linea, "Semántico", "DSL(321) El nodo '" + c1 + "' no existe.");
                    }

                    // --- DETECTA ERROR 12: Arista con nodo inexistente ---
                    // Ejemplo DSL: AGREGARARISTA A Z EN miGrafo2;
                    if (verbo.equals("AGREGARARISTA")) {
                        if (!sim.nodosGrafo.contains(c1) || !sim.nodosGrafo.contains(c2)) {
                            tablaErrores.reporte(linea, "Semántico", "DSL(322) Un nodo de la arista no existe.");
                        }
                    }

                    // --- DETECTA ERROR 13: Eliminar arista inexistente ---
                    // Ejemplo DSL: ELIMINARARISTA X Y EN miGrafo3;
                    if (verbo.equals("ELIMINARARISTA") && !sim.aristasInsertadas.contains(arista)) {
                        tablaErrores.reporte(linea, "Semántico", "DSL(324) La arista '" + arista + "' no existe.");
                    }
                }
            }
        }

        // ==========================================================
        // 5. RECORRER HIJOS Y GESTIÓN DE ÁMBITOS (CIERRE)
        // ==========================================================
        if (nodo.getHijos() != null) {
            for (NodoAST hijo : nodo.getHijos()) {
                evaluarNodo(hijo);
            }
        }

        if (esBloque) {
            tablaSimbolos.salirAmbito();
        }
    }

    /**
     * EVALUACIÓN DE TIPOS EN EXPRESIONES MATEMÁTICAS
     */
    private String evaluarTipoExpresion(NodoAST nodo) {
        if (nodo == null) return "DESCONOCIDO";

        String tipoNodo = nodo.getTipo().toUpperCase();
        switch (tipoNodo) {
            case "NUMERO": case "LITERAL_NUMERICA": return "NUMERO";
            case "CADENA": case "LITERAL_CADENA": return "TEXTO";
            case "ID": case "IDENTIFICADOR":
                return tablaSimbolos.existe(nodo.getValor()) ? tablaSimbolos.getSimbolo(nodo.getValor()).tipo : "DESCONOCIDO";
            case "OPERACION":
                String tIzq = evaluarTipoExpresion(nodo.getHijos().get(0));
                String tDer = evaluarTipoExpresion(nodo.getHijos().get(1));
                
                // --- DETECTA ERROR 18: Incompatibilidad en expresión ---
                // Ejemplo DSL: CREAR NUMERO c = 10 + "hola";
                if (!tIzq.equals(tDer)) {
                    tablaErrores.reporte(nodo.getLinea(), "Semántico", "DSL(303) No puedes mezclar " + tIzq + " y " + tDer);
                    return "DESCONOCIDO";
                }
                return tIzq;
            default: return "DESCONOCIDO";
        }
    }

    /**
     * CONTRATO DE ESTRUCTURAS: Valida si un comando es apto para un tipo de dato.
     */
    private void validarCompatibilidad(String verbo, String tipoEstructura, String nombreVar, int linea) {
        verbo = verbo.toUpperCase().trim();
        boolean esValido = false;

        switch (tipoEstructura) {
            case "PILA":
                // Valida que no se use FRENTE o ENCOLAR en una PILA
                esValido = verbo.equals("APILAR") || verbo.equals("DESAPILAR") || verbo.equals("TOPE") || verbo.equals("MOSTRAR") || verbo.equals("VACIA");
                break;
            case "COLA":
                // Valida que no se use TOPE o APILAR en una COLA
                esValido = verbo.equals("ENCOLAR") || verbo.equals("DESENCOLAR") || verbo.equals("FRENTE") || verbo.equals("MOSTRAR") || verbo.equals("VACIA");
                break;
            case "GRAFO":
                esValido = verbo.equals("AGREGARNODO") || verbo.equals("ELIMINARNODO") || verbo.equals("AGREGARARISTA") || verbo.equals("BFS") || verbo.equals("DFS") || verbo.equals("MOSTRAR");
                break;
            case "TABLA_HASH":
                // Valida que no se use APILAR en una HASH
                esValido = verbo.equals("INSERTAR") || verbo.equals("ELIMINAR") || verbo.equals("BUSCAR") || verbo.equals("MOSTRAR");
                break;
            case "LISTA_ENLAZADA":
                // Valida que no se use ENCOLAR en una LISTA
                esValido = verbo.equals("INSERTAR") || verbo.equals("ELIMINAR") || verbo.equals("BUSCAR") || verbo.equals("MOSTRAR") || verbo.equals("RECORRER");
                break;
            case "NUMERO": case "TEXTO":
                esValido = verbo.equals("MOSTRAR");
                break;
            default:
                esValido = true;
        }

        if (!esValido) {
            tablaErrores.reporte(linea, "Semántico", "DSL(304) El comando '" + verbo + "' NO es válido para " + tipoEstructura);
        }
    }
}