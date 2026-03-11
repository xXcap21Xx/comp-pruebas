// Importaciones para crear ventanas, tablas y usar mapas ordenados
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Map;
import java.util.TreeMap;

/*
 ESTA CLASE ES UNA VENTANA DE AYUDA (VISOR DE DOCUMENTACIÓN).
 Su propósito es abrir ventanas emergentes que muestren información útil al usuario
 sin cerrar la ventana principal del compilador.
 
 Muestra dos tipos de información:
 1. Tabla de Palabras Reservadas (Diccionario).
 2. Gramática del Lenguaje (Manual de sintaxis).
 */
public class VentanaReferencia extends JFrame {

    /*
     CONSTRUCTOR GENÉRICO
     Este constructor configura una ventana vacía pero lista para usarse.
     Recibe:
     - titulo: El texto que aparecerá en la barra superior.
     - contenido: El componente visual (tabla o texto) que se mostrará en el centro.
     */
    public VentanaReferencia(String titulo, Component contenido) {
        setTitle(titulo);
        setSize(750, 800); // Tamaño grande para que se lea bien
        setLocationRelativeTo(null); // Centra la ventana en la pantalla
        
        // DISPOSE_ON_CLOSE significa que al cerrar esta ventana solo se cierra ELLA,
        // no se cierra toda la aplicación.
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        setLayout(new BorderLayout());
        add(contenido, BorderLayout.CENTER);
    }

    /*
     MÉTODO ESTÁTICO: MOSTRAR TABLA DE SÍMBOLOS
     Este método se puede llamar desde cualquier parte sin crear una instancia de la clase.
     Lo que hace es:
     1. Obtener todas las palabras reservadas del Autómata.
     2. Ordenarlas alfabéticamente (usando TreeMap).
     3. Crean una tabla visual (JTable) con esos datos.
     4. Abrir la ventana con esa tabla.
     */
    public static void mostrarTablaSimbolos() {
        // Obtenemos el diccionario del Autómata
        Map<String, String> mapaOriginal = Automata.getPalabrasReservadas();
        
        // Usamos TreeMap para que las palabras aparezcan en orden A-Z automáticamente
        Map<String, String> mapaOrdenado = new TreeMap<>(mapaOriginal);

        // Configuración de columnas para la tabla
        String[] columnas = {"Palabra Reservada / Token", "Categoría / Tipo"};
        
        // Modelo de tabla no editable (para que el usuario solo pueda leer)
        DefaultTableModel modelo = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        // Llenamos la tabla fila por fila
        for (Map.Entry<String, String> entrada : mapaOrdenado.entrySet()) {
            modelo.addRow(new Object[]{entrada.getKey(), entrada.getValue()});
        }

        // Diseño visual de la tabla
        JTable tabla = new JTable(modelo);
        tabla.setFillsViewportHeight(true);
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 14)); // Letra legible
        tabla.setRowHeight(25); // Filas un poco más altas para que no se vea apretado
        
        // Ponemos la tabla dentro de un ScrollPane por si son muchas palabras
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createTitledBorder(" Diccionario de Palabras Clave y Tipos "));

        // ¡Abrimos la ventana!
        new VentanaReferencia("Referencia: Tabla de Símbolos", scroll).setVisible(true);
    }

    /*
     MÉTODO ESTÁTICO: MOSTRAR GRAMÁTICA
     Muestra un documento de texto con todas las reglas del lenguaje.
     Esto es útil para que el usuario sepa si se escribe "IF (x)" o "IF x".
     */
    public static void mostrarGramatica() {
        JTextArea txtGramatica = new JTextArea();
        txtGramatica.setEditable(false); // Solo lectura
        txtGramatica.setFont(new Font("Consolas", Font.PLAIN, 13)); // Fuente tipo código
        txtGramatica.setText(getTextoGramatica()); // Cargamos el texto largo
        txtGramatica.setCaretPosition(0); // Scroll al inicio

        JScrollPane scroll = new JScrollPane(txtGramatica);
        scroll.setBorder(BorderFactory.createTitledBorder(" Especificación Formal EBNF Completa "));

        new VentanaReferencia("Referencia: Gramática del DSL", scroll).setVisible(true);
    }

    /*
     TEXTO DE LA GRAMÁTICA (HARDCODED)
     Aquí definimos manualmente el texto que explica cómo funciona nuestro lenguaje.
     Está en formato EBNF (Extended Backus-Naur Form), que es el estándar para documentar lenguajes.
     */
    private static String getTextoGramatica() {
        return 
            "=== GRAMÁTICA LIBRE DE CONTEXTO (Formato EBNF) ===\n\n" +
            "Símbolo Inicial: <Programa>\n\n" +
            "1. ESTRUCTURA GENERAL\n" +
            // Un programa es una sentencia seguida de otro programa, o nada (epsilon).
            "<Programa> ::= <Sentencia> <Programa> | ε\n" +
            "<Sentencia> ::= <Declaracion> | <Operacion_Estructura> | <Control_Flujo> | <Salida> | <Asignacion> | \";\"\n\n" +
            
            "2. DECLARACIONES\n" +
            "<Declaracion> ::= \"CREAR\" <Tipo> \"IDENTIFICADOR\" [ \"=\" <Expresion> ] \";\"\n" +
            "<Tipo> ::= <Tipo_Primitivo> | <Tipo_Estructura>\n" +
            "<Tipo_Primitivo> ::= \"NUMERO\" | \"TEXTO\"\n" +
            "<Tipo_Estructura> ::= \"PILA\" | \"COLA\" | \"BICOLA\" | \"LISTA_ENLAZADA\" | \"LISTA_CIRCULAR\" | \"PILA_CIRCULAR\" |\n" +
            "                      \"ARBOL_BINARIO\" | \"TABLA_HASH\" | \"GRAFO\"\n\n" +
            
            "3. CONTROL DE FLUJO\n" +
            "<If_Statement>    ::= \"IF\" \"(\" <Condicion> \")\" \"{\" <Programa> \"}\" [ \"ELSE\" \"{\" <Programa> \"}\" ]\n" +
            "<While_Statement> ::= \"WHILE\" \"(\" <Condicion> \")\" \"{\" <Programa> \"}\"\n" +
            "<Do_While_Stmt>   ::= \"DO\" \"{\" <Programa> \"}\" \"WHILE\" \"(\" <Condicion> \")\" \";\"\n" +
            "<For_Statement>   ::= \"FOR\" \"(\" <Asignacion> \";\" <Condicion> \";\" <Asignacion_Sin_Semicolon> \")\" \"{\" <Programa> \"}\"\n\n" +
            
            "4. OPERACIONES DE ESTRUCTURA\n" +
            "<Operacion_Estructura> ::= <Verbo_Simple> <Expresion> \"EN\" \"IDENTIFICADOR\" \";\"\n" +
            "                         | <Verbo_Posicion> <Expresion> <Expresion> \"EN\" \"IDENTIFICADOR\" \";\"\n" +
            "                         | <Verbo_Sin_Param> \"EN\" \"IDENTIFICADOR\" \";\"\n\n" +
            
            "  - Verbos Simples: INSERTAR, INSERTAR_FINAL, INSERTAR_INICIO, APILAR, ENCOLAR, BUSCAR, INSERTARIZQUIERDA, \n" +
            "                    INSERTARDERECHA, AGREGARNODO, ELIMINARNODO, AGREGARARISTA, ELIMINARARISTA, INSERTAR_FRENTE\n" +
            "  - Verbos Posición: INSERTAR_EN_POSICION (Sintaxis: INSERTAR_EN_POSICION <pos> <valor> EN <id>;)\n" +
            "  - Verbos Sin Param: ELIMINAR, DESAPILAR, DESENCOLAR, RECORRER, BFS, DFS, CAMINOCORTO, REHASH, ELIMINAR_FRENTE, \n" +
            "                      ELIMINAR_FINAL, PREORDEN, INORDEN, POSTORDEN, RECORRIDOPORNIVELES, VER_FILA\n\n" +
            
            "5. PROPIEDADES (Uso dentro de Expresiones)\n" +
            "<Propiedad> ::= <Verbo_Propiedad> \"EN\" \"IDENTIFICADOR\"\n" +
            "              | \"VECINOS\" <Expresion> \"EN\" \"IDENTIFICADOR\"\n" +
            "  - Verbo_Propiedad: TOPE, TAMANO, VACIA, LLENA, PREORDEN, INORDEN, POSTORDEN, ALTURA, HOJAS, NODOS, GRADO, VERFILA\n\n" +
            
            "6. EXPRESIONES Y LÓGICA\n" +
            "<Expresion> ::= <Termino> { (\"+\" | \"-\") <Termino> }\n" +
            "<Termino>   ::= <Factor> { (\"*\" | \"/\") <Factor> }\n" +
            "<Factor>    ::= \"IDENTIFICADOR\" | \"NUMERO\" | \"TEXTO\" | \"VERDADERO\" | \"FALSO\" | <Propiedad> | \"(\" <Expresion> \")\"\n" +
            "<Condicion> ::= <Expresion> (\"==\" | \"!=\" | \"<\" | \">\" | \"<=\" | \">=\") <Expresion>\n\n" +
            
            "7. SALIDA Y ASIGNACIÓN\n" +
            "<Salida>     ::= \"MOSTRAR\" <Expresion> \";\"\n" +
            "<Asignacion> ::= \"IDENTIFICADOR\" \"=\" <Expresion> \";\"";
    }
}