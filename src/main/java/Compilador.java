// importamos las librerias graficas de swing y awt ya que nescesitaremos ventanas, tablas y eventos
// tambien importamos io para el manejo de archivos y util para las listas y regex

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Compilador extends JFrame {

    // --- Componentes de la Interfaz ---
    private TablaSimbolos ts;
    private TablaErrores te;
    private JTextPane txtEntrada;
    private JTextArea txtNumerosLineas;
    private StyledDocument doc;
    private JTextArea txtSintactico;
    
    // TABLAS SEPARADAS
    private JTable tablaTokens;
    private DefaultTableModel modeloTokens;
    
    private JTable tablaSimbolos;
    private DefaultTableModel simbolos;
    
    private JTable tablaErrores;
    private DefaultTableModel errores;

    private File archivoActual = null;
    private NodoAST raizAST = null;

    private JLabel lblResumen;

    // estilos para el coloreado de sintaxis
    private Style normal;
    private Style reservada;
    private Style numero;
    private Style operador;
    private Style errorStyle;
    private Style verdeComentario;
    private Style estructuraDato;
    private Style cadenaStyle;

    private Timer timerColoreo;
    private boolean coloreando = false;

    private static final Set<String> PALABRAS_RESERVADAS = Set.of(
            "CREAR", "IF", "ELSE", "MOSTRAR",
            "INSERTAR", "INSERTAR_FINAL", "INSERTAR_INICIO", "INSERTAR_EN_POSICION",
            "INSERTARIZQUIERDA", "INSERTARDERECHA", "AGREGARNODO",
            "APILAR", "ENCOLAR", "PUSH", "ENQUEUE",
            "ELIMINAR", "ELIMINAR_INICIO", "ELIMINAR_FINAL",
            "ELIMINAR_FRENTE", "ELIMINAR_POSICION", "ELIMINARNODO",
            "DESAPILAR", "POP", "DESENCOLAR", "DEQUEUE",
            "BUSCAR",
            "RECORRER", "RECORRERADELANTE", "RECORRERATRAS",
            "PREORDEN", "INORDEN", "POSTORDEN", "RECORRIDOPORNIVELES",
            "BFS", "DFS", "AGREGARARISTA", "ELIMINARARISTA", "CAMINOCORTO",
            "EN", "PESO", "ACTUALIZAR", "REHASH", "VACIA",
            "TOPE", "FRENTE", "FRONT", "PEEK", "VERFILA", "CLAVE",
            "TAMANO", "ALTURA", "HOJAS", "NODOS", "VECINOS", "LLENA",
            "NUMERO", "TEXTO", "FOR", "WHILE", "DO", "VER_FILA", "INSERTAR_FRENTE"
    );

    private static final Set<String> ESTRUCTURAS_DATOS = Set.of(
            "PILA", "COLA", "BICOLA", "LISTA_ENLAZADA", "LISTA_CIRCULAR",
            "ARBOL_BINARIO", "TABLA_HASH", "GRAFO", "PILA_CIRCULAR"
    );

    public Compilador() {

        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JMenuBar barraMenu = new JMenuBar();

        // --- MENU ARCHIVO ---
        JMenu menuArchivo = new JMenu("Archivo");

        JMenuItem itemAbrir = new JMenuItem("Abrir archivo (.txt)");
        itemAbrir.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        itemAbrir.addActionListener(e -> abrirArchivo());

        JMenuItem itemGuardar = new JMenuItem("Guardar");
        itemGuardar.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        itemGuardar.addActionListener(e -> guardarArchivo());

        JMenuItem itemGuardarComo = new JMenuItem("Guardar Como...");
        itemGuardarComo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        itemGuardarComo.addActionListener(e -> guardarArchivoComo());

        JMenuItem itemSalir = new JMenuItem("Salir");
        itemSalir.addActionListener(e -> System.exit(0));

        menuArchivo.add(itemAbrir);
        menuArchivo.add(itemGuardar);
        menuArchivo.add(itemGuardarComo);
        menuArchivo.addSeparator();
        menuArchivo.add(itemSalir);

        JMenu menuReferencias = new JMenu("Referencias");

        JMenuItem itemTabla = new JMenuItem("Tabla de Símbolos (Léxico)");
        itemTabla.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
        itemTabla.addActionListener(e -> VentanaReferencia.mostrarTablaSimbolos());

        JMenuItem itemGramatica = new JMenuItem("Gramática BNF (Sintáctico)");
        itemGramatica.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_DOWN_MASK));
        itemGramatica.addActionListener(e -> VentanaReferencia.mostrarGramatica());

        menuReferencias.add(itemTabla);
        menuReferencias.addSeparator();
        menuReferencias.add(itemGramatica);

        this.ts = new TablaSimbolos();
        this.te = new TablaErrores();

        // --- MENU RUN ---
        JMenu menuRun = new JMenu("Run");

        JMenuItem itemRunLexico = new JMenuItem("Analizador Léxico (Solo Tokens)");
        itemRunLexico.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        itemRunLexico.addActionListener(e -> ejecutarAnalisisSoloLexico());

        JMenuItem itemRunSintactico = new JMenuItem("Analizador Sintáctico (Completo)");
        itemRunSintactico.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0));
        itemRunSintactico.addActionListener(e -> ejecutarAnalisisSintactico());

        menuRun.add(itemRunLexico);
        menuRun.add(itemRunSintactico);

        barraMenu.add(menuArchivo);
        barraMenu.add(menuReferencias);
        barraMenu.add(menuRun);

        setJMenuBar(barraMenu);

        // --- EDITOR DE CÓDIGO ---
        JPanel panelCodigo = new JPanel(new BorderLayout(5, 5));
        panelCodigo.setBorder(BorderFactory.createTitledBorder(" Editor de Código DSL "));

        txtEntrada = new JTextPane();
        txtEntrada.setFont(new Font("Consolas", Font.PLAIN, 14));
        txtEntrada.setText("// --- DECLARACIÓN CON TAMAÑO ---\n"
                + "CREAR PILA miPila TAMANO 100;\n"
                + "CREAR COLA miCola TAMANO 50;\n"
                + "\n"
                + "// --- DECLARACIÓN CON INICIALIZACIÓN ---\n"
                + "CREAR NUMERO num1 = 10;\n"
                + "CREAR NUMERO num2 = 20;\n"
                + "CREAR TEXTO texto1 = \"Hola Mundo\";\n"
                + "\n"
                + "// --- OPERACIONES EN CADENA ---\n"
                + "APILAR 1 EN miPila;\n"
                + "APILAR 2 EN miPila;\n"
                + "APILAR 3 EN miPila;\n"
                + "MOSTRAR TOPE EN miPila;\n"
                + "DESAPILAR EN miPila;\n"
                + "MOSTRAR TOPE EN miPila;\n"
                + "\n"
                + "// --- OPERACIONES DE BÚSQUEDA ---\n"
                + "CREAR LISTA_ENLAZADA miLista;\n"
                + "INSERTAR_FINAL 5 EN miLista;\n"
                + "INSERTAR_FINAL 10 EN miLista;\n"
                + "INSERTAR_FINAL 15 EN miLista;\n"
                + "\n"
                + "// --- RECORRIDOS DE ÁRBOL ---\n"
                + "CREAR ARBOL_BINARIO miArbol;\n"
                + "AGREGARNODO 1 25 EN miArbol;\n"
                + "AGREGARNODO 1 15 EN miArbol;\n"
                + "AGREGARNODO 1 35 EN miArbol;\n"
                + "AGREGARNODO 1 10 EN miArbol;\n"
                + "AGREGARNODO 1 20 EN miArbol;\n"
                + "AGREGARNODO 1 30 EN miArbol;\n"
                + "AGREGARNODO 1 40 EN miArbol;\n"
                + "\n"
                + "MOSTRAR PREORDEN EN miArbol;\n"
                + "MOSTRAR INORDEN EN miArbol;\n"
                + "MOSTRAR POSTORDEN EN miArbol;\n"
                + "MOSTRAR RECORRIDOPORNIVELES EN miArbol;\n"
                + "\n"
                + "// --- OPERACIONES AVANZADAS DE GRAFO ---\n"
                + "CREAR GRAFO miGrafo;\n"
                + "\n"
                + "AGREGARNODO 1 100 EN miGrafo;\n"
                + "AGREGARNODO 2 200 EN miGrafo;\n"
                + "AGREGARNODO 3 300 EN miGrafo;\n"
                + "AGREGARNODO 4 400 EN miGrafo;\n"
                + "AGREGARNODO 5 500 EN miGrafo;\n"
                + "\n"
                + "AGREGARARISTA 1 2 EN miGrafo;\n"
                + "AGREGARARISTA 2 3 EN miGrafo;\n"
                + "AGREGARARISTA 3 4 EN miGrafo;\n"
                + "AGREGARARISTA 4 5 EN miGrafo;\n"
                + "AGREGARARISTA 1 5 EN miGrafo;\n"
                + "\n"
                + "MOSTRAR VECINOS 1 EN miGrafo;\n"
                + "\n"
                + "// --- OPERACIONES DE TABLA HASH ---\n"
                + "CREAR TABLA_HASH miHash;\n"
                + "INSERTAR 101 1000 EN miHash;\n"
                + "INSERTAR 102 2000 EN miHash;\n"
                + "INSERTAR 103 3000 EN miHash;");

        doc = txtEntrada.getStyledDocument();
        inicializarEstilos();

        txtNumerosLineas = new JTextArea("1");
        txtNumerosLineas.setFont(new Font("Consolas", Font.PLAIN, 14));
        txtNumerosLineas.setBackground(new Color(230, 230, 230));
        txtNumerosLineas.setForeground(Color.GRAY);
        txtNumerosLineas.setEditable(false);
        txtNumerosLineas.setMargin(new Insets(0, 5, 0, 5));

        JScrollPane scrollCodigo = new JScrollPane(txtEntrada);
        scrollCodigo.setRowHeaderView(txtNumerosLineas);
        scrollCodigo.setPreferredSize(new Dimension(1000, 200));

        JButton btnAnalizar = new JButton("Compilar / Ejecutar");
        btnAnalizar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnAnalizar.setForeground(Color.WHITE);
        btnAnalizar.setBackground(new Color(0, 120, 215));
        btnAnalizar.setFocusPainted(false);
        btnAnalizar.setBorderPainted(false);
        btnAnalizar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAnalizar.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        btnAnalizar.addActionListener(e -> ejecutarAnalisisSintactico());

        btnAnalizar.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnAnalizar.setBackground(new Color(0, 90, 170));
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnAnalizar.setBackground(new Color(0, 120, 215));
            }
        });

        panelCodigo.add(scrollCodigo, BorderLayout.CENTER);
        panelCodigo.add(btnAnalizar, BorderLayout.EAST);

        // --- PESTAÑAS DE RESULTADOS ---
        JTabbedPane pestañas = new JTabbedPane();

        // 1. DICCIONARIO
        String[] colsDiccionario = {"Código", "Categoría", "Significado / Solución"};
        DefaultTableModel modeloDic = new DefaultTableModel(getDatosDiccionario(), colsDiccionario) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        JTable tablaDiccionario = new JTable(modeloDic);
        tablaDiccionario.setRowHeight(25);
        tablaDiccionario.getColumnModel().getColumn(0).setPreferredWidth(80);
        tablaDiccionario.getColumnModel().getColumn(1).setPreferredWidth(100);
        tablaDiccionario.getColumnModel().getColumn(2).setPreferredWidth(400);
        tablaDiccionario.setBackground(new Color(245, 245, 250));

        // 2. TABLA DE TOKENS (NUEVO)
        String[] colsTokens = {"Lexema", "Línea", "Col", "Tipo Token"};
        modeloTokens = new DefaultTableModel(colsTokens, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tablaTokens = new JTable(modeloTokens);
        tablaTokens.setFillsViewportHeight(true);
        JScrollPane scrollTokens = new JScrollPane(tablaTokens);
        pestañas.addTab("Tabla de Tokens (Léxico)", scrollTokens);

        // 3. TABLA DE SÍMBOLOS REAL (ACTUALIZADO)
        String[] colsSimbolos = {"Nombre", "Tipo", "Valor"};
        simbolos = new DefaultTableModel(colsSimbolos, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tablaSimbolos = new JTable(simbolos);
        tablaSimbolos.setFillsViewportHeight(true);
        JScrollPane scrollSimbolos = new JScrollPane(tablaSimbolos);
        pestañas.addTab("Tabla de Símbolos (Memoria)", scrollSimbolos);

        // 4. ÁRBOL SINTÁCTICO
        txtSintactico = new JTextArea();
        txtSintactico.setEditable(false);
        txtSintactico.setFont(new Font("Consolas", Font.PLAIN, 12));
        txtSintactico.setForeground(new Color(40, 40, 40));
        JScrollPane scrollSintactico = new JScrollPane(txtSintactico);

        JPanel panelArbol = new JPanel(new BorderLayout());
        panelArbol.add(scrollSintactico, BorderLayout.CENTER);

        JButton btnGuardarArbol = new JButton("Guardar Árbol");
        btnGuardarArbol.addActionListener(e -> imprimirArbolSintactico());
        panelArbol.add(btnGuardarArbol, BorderLayout.SOUTH);

        pestañas.addTab("Árbol Sintáctico (AST)", panelArbol);

        // 5. ERRORES
        String[] colsErrores = {"Línea", "Fase", "Descripción del Error"};
        errores = new DefaultTableModel(colsErrores, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tablaErrores = new JTable(errores);
        tablaErrores.setFillsViewportHeight(true);
        tablaErrores.getColumnModel().getColumn(0).setMaxWidth(60);
        tablaErrores.getColumnModel().getColumn(1).setMaxWidth(100);

        JScrollPane scrollErrores = new JScrollPane(tablaErrores);
        pestañas.addTab("Errores Encontrados", scrollErrores);
        
        // Se agrega el diccionario al final
        pestañas.addTab("Diccionario de Errores", new JScrollPane(tablaDiccionario));

        lblResumen = new JLabel(" Esperando código...");
        lblResumen.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblResumen.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel panelInferior = new JPanel(new BorderLayout());
        panelInferior.add(lblResumen, BorderLayout.WEST);

        add(panelCodigo, BorderLayout.NORTH);
        add(pestañas, BorderLayout.CENTER);
        add(panelInferior, BorderLayout.SOUTH);

        timerColoreo = new Timer(500, e -> colorearTexto());
        timerColoreo.setRepeats(false);

        doc.addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                actualizarLineas();
                timerColoreo.restart();
            }

            public void removeUpdate(DocumentEvent e) {
                actualizarLineas();
                timerColoreo.restart();
            }

            public void changedUpdate(DocumentEvent e) {
            }
        });

        actualizarLineas();
        colorearTexto();
    }

    private void inicializarEstilos() {
        StyleContext sc = StyleContext.getDefaultStyleContext();
        normal = sc.addStyle("normal", null);
        StyleConstants.setForeground(normal, Color.BLACK);

        reservada = sc.addStyle("reservada", null);
        StyleConstants.setForeground(reservada, Color.BLUE);
        StyleConstants.setBold(reservada, true);

        numero = sc.addStyle("numero", null);
        StyleConstants.setForeground(numero, new Color(150, 0, 150));

        operador = sc.addStyle("operador", null);
        StyleConstants.setForeground(operador, Color.DARK_GRAY);

        errorStyle = sc.addStyle("error", null);
        StyleConstants.setForeground(errorStyle, Color.RED);

        verdeComentario = sc.addStyle("comentario", null);
        StyleConstants.setForeground(verdeComentario, new Color(0, 128, 0));
        StyleConstants.setItalic(verdeComentario, true);

        estructuraDato = sc.addStyle("estructura", null);
        StyleConstants.setForeground(estructuraDato, new Color(255, 140, 0));
        StyleConstants.setBold(estructuraDato, true);

        cadenaStyle = sc.addStyle("cadena", null);
        StyleConstants.setForeground(cadenaStyle, new Color(200, 20, 20));
    }

    private void actualizarLineas() {
        int lineas = txtEntrada.getText().split("\n", -1).length;
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= lineas; i++) {
            sb.append(i).append("\n");
        }
        txtNumerosLineas.setText(sb.toString());
    }

    private void limpiarTablas() {
        if(modeloTokens != null) modeloTokens.setRowCount(0);
        if(simbolos != null) simbolos.setRowCount(0);
        if(errores != null) errores.setRowCount(0);
        txtSintactico.setText("");
        raizAST = null;
        lblResumen.setForeground(Color.BLACK);
        lblResumen.setText("Analizando...");
    }

    private void ejecutarAnalisisSoloLexico() {
        limpiarTablas();
        String codigo = txtEntrada.getText();

        MotorLexico.ResultadoLexico resultado = MotorLexico.ejecutar(codigo);

        for (Object[] fila : resultado.datosSimbolos) {
            modeloTokens.addRow(fila); // <--- Llenamos la nueva tabla de tokens
        }

        for (MotorLexico.ErrorDatos err : resultado.errores) {
            errores.addRow(new Object[]{err.linea, "LÉXICO", err.descripcion});
        }

        if (resultado.errores.isEmpty()) {
            lblResumen.setText(" Análisis LÉXICO finalizado con ÉXITO.");
            lblResumen.setForeground(new Color(0, 128, 0));
            txtSintactico.setText("--- Modo Solo Léxico ---\nEl árbol no se genera en este modo.");
        } else {
            lblResumen.setText(" Se encontraron " + resultado.errores.size() + " errores léxicos.");
            lblResumen.setForeground(Color.RED);
        }
    }

    /* * MÉTODO CORREGIDO QUE INTEGRA EL MOTOR SEMÁNTICO Y UNIFICA LAS TABLAS DE ERRORES 
     */
    private void ejecutarAnalisisSintactico() {
        limpiarTablas();
        String codigo = txtEntrada.getText();

        // 1. Ejecutamos léxico
        MotorLexico.ResultadoLexico resLexico = MotorLexico.ejecutar(codigo);

        // --- SOLUCIÓN 1: LLENAR LA TABLA DE TOKENS (LÉXICO) ---
        if (resLexico.datosSimbolos != null) {
            for (Object[] fila : resLexico.datosSimbolos) {
                modeloTokens.addRow(fila); // <--- Llenamos la nueva tabla de tokens
            }
        }

        // 2. Ejecutamos sintáctico con los tokens válidos
        MotorSintactico.ResultadoSintactico resSintactico = MotorSintactico.ejecutar(resLexico.tokensValidos);

        // Preparamos las tablas asegurando que no sean nulas
        TablaSimbolos tsActual = resSintactico.tablaSimbolos != null ? resSintactico.tablaSimbolos : new TablaSimbolos();
        TablaErrores teActual = resSintactico.tablaErrores != null ? resSintactico.tablaErrores : new TablaErrores();

        // 3. ¡FASE SEMÁNTICA! 
        MotorSemantico.ejecutar(resSintactico.raiz, tsActual, teActual);

        // Sincronizamos las variables globales de la ventana 
        this.ts = tsActual;
        this.te = teActual;
        this.raizAST = resSintactico.raiz;

        // --- SOLUCIÓN 2: LLENAR LA VERDADERA TABLA DE SÍMBOLOS ---
        if (this.ts != null) {
            java.util.Map<String, TablaSimbolos.Simbolo> variablesMemoria = this.ts.getTodosLosSimbolos();
            if (variablesMemoria != null) {
                for (TablaSimbolos.Simbolo sim : variablesMemoria.values()) {
                    // Llenamos la tabla gráfica de memoria con {Nombre, Tipo, Valor}
                    simbolos.addRow(new Object[]{sim.nombre, sim.tipo, sim.valor});
                }
            }
        }

        // 4. Actualizamos el Árbol en pantalla
        txtSintactico.setText(resSintactico.logArbol);
        txtSintactico.setCaretPosition(0);

        // 5. --- SOLUCIÓN 3: RECOPILAR ERRORES CON VALIDACIÓN ---
        int totalErrores = 0;

        // a) Errores Léxicos
        if (resLexico.errores != null) {
            for (MotorLexico.ErrorDatos err : resLexico.errores) {
                errores.addRow(new Object[]{err.linea, "LÉXICO", err.descripcion});
                totalErrores++;
            }
        }

        // b) Errores Sintácticos
        if (resSintactico.errores != null) {
            for (MotorSintactico.ErrorDatosSintactico err : resSintactico.errores) {
                errores.addRow(new Object[]{err.linea, "SINTÁCTICO", err.descripcion});
                totalErrores++;
            }
        }

        // c) Errores Semánticos (Desde la TablaErrores)
        if (teActual != null) {
            Object[][] datosSemanticos = teActual.getDatosParaTabla();
            if (datosSemanticos != null) {
                for (Object[] filaError : datosSemanticos) {
                    errores.addRow(filaError);
                    totalErrores++;
                }
            }
        }

        // 6. Actualizamos el label de resumen final
        if (totalErrores == 0) {
            lblResumen.setText(" Compilación exitosa (0 errores).");
            lblResumen.setForeground(new Color(0, 128, 0));
        } else {
            lblResumen.setText(" Se encontraron " + totalErrores + " errores en total.");
            lblResumen.setForeground(Color.RED);
        }
    }

    // --- MÉTODOS DE ARCHIVO ---
    private void abrirArchivo() {
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Abrir archivo de código DSL");
        selector.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivos de texto (.txt)", "txt"));

        if (selector.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            archivoActual = selector.getSelectedFile();
            try (BufferedReader br = new BufferedReader(new FileReader(archivoActual))) {
                txtEntrada.setText("");
                String linea;
                while ((linea = br.readLine()) != null) {
                    doc.insertString(doc.getLength(), linea + "\n", normal);
                }
                setTitle("Analizador DSL - " + archivoActual.getName());
                colorearTexto();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al leer el archivo: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void guardarArchivo() {
        if (archivoActual != null) {
            escribirArchivo(archivoActual, txtEntrada.getText());
        } else {
            guardarArchivoComo();
        }
    }

    private void guardarArchivoComo() {
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Guardar código como...");
        selector.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivos de texto (.txt)", "txt"));

        if (selector.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File archivoAGuardar = selector.getSelectedFile();
            if (!archivoAGuardar.getName().toLowerCase().endsWith(".txt")) {
                archivoAGuardar = new File(archivoAGuardar.getAbsolutePath() + ".txt");
            }
            escribirArchivo(archivoAGuardar, txtEntrada.getText());
            archivoActual = archivoAGuardar;
            setTitle("Analizador DSL - " + archivoActual.getName());
        }
    }

    private void imprimirArbolSintactico() {
        String contenidoArbol = txtSintactico.getText();
        if (contenidoArbol.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El árbol está vacío. Primero ejecuta el análisis.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Guardar Árbol Sintáctico");
        selector.setSelectedFile(new File("arbol_sintactico.txt"));
        selector.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivos de texto (.txt)", "txt"));

        if (selector.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File archivoAGuardar = selector.getSelectedFile();
            if (!archivoAGuardar.getName().toLowerCase().endsWith(".txt")) {
                archivoAGuardar = new File(archivoAGuardar.getAbsolutePath() + ".txt");
            }
            escribirArchivo(archivoAGuardar, contenidoArbol);
            JOptionPane.showMessageDialog(this, "Árbol guardado con éxito.");
        }
    }

    private void escribirArchivo(File archivo, String contenido) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(archivo))) {
            bw.write(contenido);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error al guardar el archivo: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- MÉTODOS DE COLOREADO ---
    private void colorearTexto() {

        if (coloreando) {
            return;
        }
        coloreando = true;

        SwingUtilities.invokeLater(() -> {

            try {

                int posicionCursor = txtEntrada.getCaretPosition();

                String texto = doc.getText(0, doc.getLength());

                // limpiar estilos
                doc.setCharacterAttributes(0, texto.length(), normal, true);

                Pattern patron = Pattern.compile(
                        "//.*"
                        + // comentario
                        "|\"[^\"]*\""
                        + // cadena
                        "|\\b\\d+\\b"
                        + // número
                        "|\\b[A-Za-z_][A-Za-z0-9_]*\\b" // palabra
                );

                Matcher m = patron.matcher(texto);

                while (m.find()) {

                    String token = m.group();
                    int inicio = m.start();
                    int longitud = m.end() - m.start();

                    if (token.startsWith("//")) {

                        doc.setCharacterAttributes(inicio, longitud, verdeComentario, false);

                    } else if (token.startsWith("\"")) {

                        doc.setCharacterAttributes(inicio, longitud, cadenaStyle, false);

                    } else if (token.matches("\\d+")) {

                        doc.setCharacterAttributes(inicio, longitud, numero, false);

                    } else {

                        String palabra = token.toUpperCase();

                        if (ESTRUCTURAS_DATOS.contains(palabra)) {

                            doc.setCharacterAttributes(inicio, longitud, estructuraDato, false);

                        } else if (PALABRAS_RESERVADAS.contains(palabra)) {

                            doc.setCharacterAttributes(inicio, longitud, reservada, false);

                        }
                    }
                }

                // restaurar cursor
                txtEntrada.setCaretPosition(Math.min(posicionCursor, doc.getLength()));

            } catch (Exception ex) {

                ex.printStackTrace();

            } finally {

                coloreando = false;

            }

        });
    }

    private Object[][] getDatosDiccionario() {
        return new Object[][]{
            {"DSL(101)", "Léxico", "Símbolo no reconocido en el alfabeto del lenguaje."},
            {"DSL(102)", "Léxico", "Cadena de texto (string) sin cerrar o mal formada."},
            {"DSL(103)", "Léxico", "Número o identificador mal formado (ej. 123abc)."},
            {"DSL(201)", "Sintáctico", "Falta el punto y coma (;) al final de la sentencia."},
            {"DSL(202)", "Sintáctico", "Falta un delimitador de bloque ( } ) o paréntesis ( ) )."},
            {"DSL(203)", "Sintáctico", "Sentencia no válida o palabra reservada mal posicionada."},
            {"DSL(204)", "Sintáctico", "Tipo de estructura de datos desconocido o no soportado."},
            {"DSL(205)", "Sintáctico", "Falta la palabra clave 'EN' necesaria para la operación."},
            {"DSL(206)", "Sintáctico", "Se esperaba un operador relacional (==, !=, <, >, etc)."},
            {"DSL(207)", "Sintáctico", "Expresión matemática o lógica mal formada."},
            {"DSL(301)", "Semántico", "La variable no ha sido declarada previamente con CREAR."},
            {"DSL(302)", "Semántico", "La variable ya existe en la Tabla de Símbolos."},
            {"DSL(303)", "Semántico", "Incompatibilidad de tipos en la operación (ej. Numero + Texto)."},
            {"DSL(304)", "Semántico", "Comando no compatible con el tipo de estructura de datos utilizada."}
        };
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> new Compilador().setVisible(true));
    }
}