
import java.util.List;


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
        tablaSimbolos.limpiar(); // Limpiamos la memoria antes de empezar la ejecución
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
        // 1. GESTIÓN DE ÁMBITOS
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

            if (!tablaSimbolos.existe(nombreVar)) {

                Object valorInicial = "Vacio";

                if (nodoValor != null) {

                    String tipoValor = evaluarTipoExpresion(nodoValor);
                    valorInicial = nodoValor.getValor();

                    if (!tipoVar.equals(tipoValor) && !tipoValor.equals("DESCONOCIDO")) {

                        tablaErrores.reporte(linea, "Semántico",
                                "DSL(303) Incompatibilidad: No puedes asignar un valor de tipo "
                                + tipoValor + " a la variable '" + nombreVar + "' que es de tipo " + tipoVar + ".");
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
        // 3. USO DE VARIABLES
        // ==========================================================
        if (tipoNodo.equals("ID_ESTRUCTURA")
                || tipoNodo.equals("ID_VAR")
                || tipoNodo.equals("ID")
                || tipoNodo.equals("IDENTIFICADOR")) {
            String palabra = valorNodo.toUpperCase();
            // EVITAR QUE PALABRAS DEL DSL SE TRATEN COMO VARIABLES
            if (palabra.equals("TAMANO")
                    || palabra.equals("LLENA")
                    || palabra.equals("VACIA")
                    || palabra.equals("TOPE")
                    || palabra.equals("FRENTE")) {
                return;
            }

            if (!tablaSimbolos.existe(valorNodo)) {

                tablaErrores.reporte(linea, "Semántico",
                        "DSL(301) La variable '" + valorNodo + "' no existe.");
            } else {

                TablaSimbolos.Simbolo sim = tablaSimbolos.getSimbolo(valorNodo);

                if (sim != null
                        && (sim.tipo.equals("NUMERO") || sim.tipo.equals("TEXTO"))
                        && sim.valor.equals("Vacio")) {
                    tablaErrores.reporte(linea, "Semántico",
                            "DSL(306) La variable '" + valorNodo + "' se usa sin valor.");
                }
            }
        }

        // ==========================================================
// 4. OPERACIONES SOBRE ESTRUCTURAS
// ==========================================================
        if (tipoNodo.equals("OPERACION") || tipoNodo.equals("PROPIEDAD")) {
            String verbo = valorNodo.toUpperCase();
            String nombreEstructura = "";

            if (nodo.getHijos() != null) {

                for (NodoAST hijo : nodo.getHijos()) {

                    String etiqueta = hijo.getTipo() != null
                            ? hijo.getTipo().toUpperCase().trim()
                            : "";

                    if (etiqueta.equals("ID_ESTRUCTURA") || etiqueta.equals("ID")) {

                        nombreEstructura = hijo.getValor().trim();
                        break;
                    }
                }
            }

            if (!nombreEstructura.isEmpty() && tablaSimbolos.existe(nombreEstructura)) {

                TablaSimbolos.Simbolo sim = tablaSimbolos.getSimbolo(nombreEstructura);
                String tipo = sim.tipo.toUpperCase();

                validarCompatibilidad(verbo, tipo, nombreEstructura, linea);

                // ======================================================
                // REGLAS TABLA HASH
                // ======================================================
                if (tipo.equals("TABLA_HASH")) {

                    String clave = "";

                    if (nodo.getHijos() != null && nodo.getHijos().size() >= 1) {
                        clave = nodo.getHijos().get(0).getValor();
                    }

                    if (verbo.equals("INSERTAR")) {

                        if (sim.valoresInsertados.contains(clave)) {

                            tablaErrores.reporte(linea, "Semántico",
                                    "DSL(307) La clave '" + clave
                                    + "' ya existe en '" + nombreEstructura + "'.");

                        } else {

                            sim.valoresInsertados.add(clave);
                        }
                    }

                    if (verbo.equals("ELIMINAR")) {

                        if (!sim.valoresInsertados.contains(clave)) {

                            tablaErrores.reporte(linea, "Semántico",
                                    "DSL(308) No se puede eliminar la clave '" + clave
                                    + "' porque no existe en '" + nombreEstructura + "'.");

                        } else {

                            sim.valoresInsertados.remove(clave);
                        }
                    }

                    if (verbo.equals("BUSCAR")) {

                        if (!sim.valoresInsertados.contains(clave)) {

                            tablaErrores.reporte(linea, "Semántico",
                                    "DSL(309) La clave '" + clave
                                    + "' no existe en '" + nombreEstructura + "'.");

                        }
                    }
                }

                // ======================================================
                // REGLAS GRAFOS
                // ======================================================
                if (tipo.equals("GRAFO")) {

                    String clave1 = "";
                    String clave2 = "";

                    if (nodo.getHijos() != null && nodo.getHijos().size() >= 1) {
                        clave1 = nodo.getHijos().get(0).getValor();
                    }

                    if (nodo.getHijos() != null && nodo.getHijos().size() >= 2) {
                        clave2 = nodo.getHijos().get(1).getValor();
                    }

                    String arista = clave1 + "-" + clave2;

                    // -----------------------------
                    // AGREGAR NODO
                    // -----------------------------
                    if (verbo.equals("AGREGARNODO")) {

                        if (sim.nodosGrafo.contains(clave1)) {

                            tablaErrores.reporte(linea, "Semántico",
                                    "DSL(320) El nodo '" + clave1
                                    + "' ya existe en el grafo '" + nombreEstructura + "'.");

                        } else {

                            sim.nodosGrafo.add(clave1);
                        }
                    }

                    // -----------------------------
                    // ELIMINAR NODO
                    // -----------------------------
                    if (verbo.equals("ELIMINARNODO")) {

                        if (!sim.nodosGrafo.contains(clave1)) {

                            tablaErrores.reporte(linea, "Semántico",
                                    "DSL(321) El nodo '" + clave1
                                    + "' no existe en el grafo '" + nombreEstructura + "'.");

                        } else {

                            sim.nodosGrafo.remove(clave1);
                        }
                    }

                    // -----------------------------
                    // AGREGAR ARISTA
                    // -----------------------------
                    if (verbo.equals("AGREGARARISTA")) {

                        if (!sim.nodosGrafo.contains(clave1) || !sim.nodosGrafo.contains(clave2)) {

                            tablaErrores.reporte(linea, "Semántico",
                                    "DSL(322) No se puede crear la arista porque uno de los nodos no existe.");

                        } else if (sim.aristasInsertadas.contains(arista)) {

                            tablaErrores.reporte(linea, "Semántico",
                                    "DSL(323) La arista '" + clave1 + "-" + clave2
                                    + "' ya existe en '" + nombreEstructura + "'.");

                        } else {

                            sim.aristasInsertadas.add(arista);
                        }
                    }

                    // -----------------------------
                    // ELIMINAR ARISTA
                    // -----------------------------
                    if (verbo.equals("ELIMINARARISTA")) {

                        if (!sim.aristasInsertadas.contains(arista)) {

                            tablaErrores.reporte(linea, "Semántico",
                                    "DSL(324) La arista '" + clave1 + "-" + clave2
                                    + "' no existe en '" + nombreEstructura + "'.");

                        } else {

                            sim.aristasInsertadas.remove(arista);
                        }
                    }
                }
            }
        }
 
        // ==========================================================
        // 5. BORRAR ESTRUCTURA
        // ==========================================================
        if (valorNodo.equalsIgnoreCase("BORRAR") && nodo.getHijos().size() >= 1) {

            String nombre = nodo.getHijos().get(0).getValor().trim();

            if (!tablaSimbolos.existe(nombre)) {

                tablaErrores.reporte(linea, "Semántico",
                        "DSL(301) No se puede borrar '" + nombre + "' porque no existe.");

            } else {

                tablaSimbolos.eliminar(nombre);
            }

            return;
        }

        // ==========================================================
        // 6. RECORRER HIJOS
        // ==========================================================
        if (nodo.getHijos() != null) {

            for (NodoAST hijo : nodo.getHijos()) {
                evaluarNodo(hijo);
            }
        }

        // ==========================================================
        // 7. SALIR DE ÁMBITO
        // ==========================================================
        if (esBloque) {
            tablaSimbolos.salirAmbito();
        }
    }

    private String formatearArista(String origen, String destino) {
        return origen.compareTo(destino) < 0 ? origen + "-" + destino : destino + "-" + origen;
    }

    /**
     * Evalúa recursivamente el tipo de una expresión (NUMERO, TEXTO, o
     * DESCONOCIDO). Esto ayuda a validar asignaciones y operaciones.
     */
    private String evaluarTipoExpresion(NodoAST nodo) {
        if (nodo == null) {
            return "DESCONOCIDO";
        }

        String tipoNodo = nodo.getTipo().toUpperCase();

        switch (tipoNodo) {
            case "NUMERO":
            case "LITERAL_NUMERICA":
                return "NUMERO";
            case "CADENA":
            case "LITERAL_CADENA":
                return "TEXTO";
            case "IDENTIFICADOR":
            case "ID":
                if (tablaSimbolos.existe(nodo.getValor())) {
                    return tablaSimbolos.getSimbolo(nodo.getValor()).tipo.toUpperCase();
                }
                return "DESCONOCIDO"; // El error de "no existe" se reporta en otra parte
            case "OPERACION":
            case "COMPARACION":
                if (nodo.getHijos().size() < 2) {
                    return "DESCONOCIDO";
                }
                String tipoIzq = evaluarTipoExpresion(nodo.getHijos().get(0));
                String tipoDer = evaluarTipoExpresion(nodo.getHijos().get(1));

                if (tipoIzq.equals("NUMERO") && tipoDer.equals("NUMERO")) {
                    return "NUMERO"; // Operaciones matemáticas entre números dan un número
                }
                if (!tipoIzq.equals(tipoDer)) {
                    tablaErrores.reporte(nodo.getLinea(), "Semántico", "DSL(303) Incompatibilidad: No se pueden mezclar tipos " + tipoIzq + " y " + tipoDer + " en una operación.");
                    return "DESCONOCIDO";
                }
                return tipoIzq; // Si ambos son iguales (ej. TEXTO + TEXTO)
            default:
                return "DESCONOCIDO";
        }
    }

    /**
     * Módulo de Reglas Estrictas de Estructuras de Datos.
     */
    private void validarCompatibilidad(String verbo, String tipoEstructura, String nombreVar, int linea) {

        // Normalizar el verbo para evitar problemas de mayúsculas
        verbo = verbo.toUpperCase().trim();

        boolean esValido = false;

        switch (tipoEstructura) {

            // =========================
            // PILAS
            // =========================
            case "PILA":
            case "PILA_CIRCULAR":

                esValido = verbo.equals("APILAR")
                        || verbo.equals("PUSH")
                        || verbo.equals("DESAPILAR")
                        || verbo.equals("POP")
                        || verbo.equals("TOPE")
                        || verbo.equals("PEEK")
                        || verbo.equals("VACIA")
                        || verbo.equals("LLENA")
                        || verbo.equals("TAMANO")
                        || verbo.equals("MOSTRAR");

                break;

            // =========================
            // COLAS
            // =========================
            case "COLA":
            case "BICOLA":

                esValido = verbo.equals("ENCOLAR")
                        || verbo.equals("ENQUEUE")
                        || verbo.equals("DESENCOLAR")
                        || verbo.equals("DEQUEUE")
                        || verbo.equals("FRENTE")
                        || verbo.equals("FRONT")
                        || verbo.equals("VACIA")
                        || verbo.equals("LLENA")
                        || verbo.equals("TAMANO")
                        || verbo.equals("MOSTRAR");

                break;

            // =========================
            // LISTAS ENLAZADAS
            // =========================
            case "LISTA_ENLAZADA":
            case "LISTA_CIRCULAR":

                esValido = verbo.equals("INSERTAR")
                        || verbo.equals("INSERTAR_INICIO")
                        || verbo.equals("INSERTAR_FINAL")
                        || verbo.equals("INSERTAR_EN_POSICION")
                        || verbo.equals("ELIMINAR")
                        || verbo.equals("ELIMINAR_INICIO")
                        || verbo.equals("ELIMINAR_FINAL")
                        || verbo.equals("ELIMINAR_POSICION")
                        || verbo.equals("BUSCAR")
                        || verbo.equals("RECORRER")
                        || verbo.equals("TAMANO")
                        || verbo.equals("MOSTRAR");

                break;

            // =========================
            // LISTA DOBLE
            // =========================
            case "LISTA_DOBLE_ENLAZADA":

                esValido = verbo.equals("INSERTAR")
                        || verbo.equals("INSERTAR_INICIO")
                        || verbo.equals("INSERTAR_FINAL")
                        || verbo.equals("INSERTAR_EN_POSICION")
                        || verbo.equals("ELIMINAR")
                        || verbo.equals("ELIMINAR_INICIO")
                        || verbo.equals("ELIMINAR_FINAL")
                        || verbo.equals("ELIMINAR_POSICION")
                        || verbo.equals("BUSCAR")
                        || verbo.equals("RECORRERADELANTE")
                        || verbo.equals("RECORRERATRAS")
                        || verbo.equals("TAMANO")
                        || verbo.equals("MOSTRAR");

                break;

            // =========================
            // GRAFOS
            // =========================
            case "GRAFO":

                esValido = verbo.equals("AGREGARNODO")
                        || verbo.equals("ELIMINARNODO")
                        || verbo.equals("AGREGARARISTA")
                        || verbo.equals("ELIMINARARISTA")
                        || verbo.equals("BFS")
                        || verbo.equals("DFS")
                        || verbo.equals("MOSTRAR");

                break;

            // =========================
            // TABLAS HASH
            // =========================
            case "TABLA_HASH":

                esValido = verbo.equals("INSERTAR")
                        || verbo.equals("ELIMINAR")
                        || verbo.equals("BUSCAR")
                        || verbo.equals("ACTUALIZAR")
                        || verbo.equals("MOSTRAR");

                break;

            // =========================
            // TIPOS PRIMITIVOS
            // =========================
            case "NUMERO":
            case "TEXTO":

                esValido = verbo.equals("MOSTRAR");

                break;

            default:
                esValido = true;
        }

        if (!esValido) {

            tablaErrores.reporte(linea, "Semántico",
                    "DSL(304) El comando '" + verbo
                    + "' NO pertenece a la estructura "
                    + tipoEstructura + " ('" + nombreVar + "').");
        }
    }

}
