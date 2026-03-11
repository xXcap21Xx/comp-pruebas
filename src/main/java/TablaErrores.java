import java.util.ArrayList;
import java.util.List;

/*
 ESTA CLASE ES EL GESTOR DE ERRORES.
 Su función es servir como registro centralizado.
 Cada vez que el analizador Léxico, Sintáctico o Semántico encuentra un problema,
 lo reporta aquí en lugar de imprimirlo en consola.
 
 Esto permite que después la Interfaz Gráfica (GUI) pueda pedir todos los errores
 juntos y mostrarlos en una tabla.
 */
public class TablaErrores {

    // Lista dinámica para guardar los errores. Usamos una lista porque no sabemos
    // cuántos errores cometerá el usuario (pueden ser 0 o más).
    private final List<ErrorDetalle> listaErrores;

    // CONSTRUCTOR
    // Inicializa la lista vacía, lista para empezar a recibir quejas del compilador.
    public TablaErrores() {
        this.listaErrores = new ArrayList<>();
    }

    /*
     MÉTODO: REPORTE
     Este es el método que llaman los analizadores (Léxico/Sintáctico) cuando algo sale mal.
     Recibe:
       - linea: ¿Dónde ocurrió el error?
       - fase: ¿Quién lo detectó? (Ej: "Léxico", "Sintáctico", "Semántico")
       - descripcion: ¿Qué pasó? (Ej: "Falta punto y coma", "Símbolo desconocido")
     */
    public void reporte(int linea, String fase, String descripcion) {
        // Crea un nuevo objeto con los detalles y lo guarda en la lista.
        listaErrores.add(new ErrorDetalle(linea, fase, descripcion));
    }

    // Método auxiliar para saber si el código está limpio o sucio.
    // Útil para decidir si detener la compilación o continuar.
    public boolean tieneErrores() {
        return !listaErrores.isEmpty();
    }

    // Método para borrar todo. Se usa cuando el usuario presiona "Compilar" de nuevo,
    // para no mezclar errores viejos con los nuevos.
    public void limpiar() {
        listaErrores.clear();
    }

    /*
     MÉTODO: GET DATOS PARA TABLA
     Este método es el "traductor" entre tu lógica y la Interfaz Gráfica (Java Swing).
     
     Los componentes visuales como JTable no entienden de listas de objetos personalizados,
     prefieren una matriz simple de filas y columnas (Object[][]).
     
     Aquí transformamos nuestra List<ErrorDetalle> en esa matriz.
     */
    public Object[][] getDatosParaTabla() {
        // Creamos una matriz con tantas filas como errores haya, y 3 columnas (Línea, Fase, Descripción).
        Object[][] datos = new Object[listaErrores.size()][3];
        
        for (int i = 0; i < listaErrores.size(); i++) {
            ErrorDetalle err = listaErrores.get(i);
            
            // Columna 0: Número de línea
            datos[i][0] = err.linea;
            
            // Columna 1: Fase del error (Léxico/Sintáctico)
            datos[i][1] = err.fase;
            
            // Columna 2: Mensaje explicativo
            datos[i][2] = err.descripcion;
        }
        
        // Retornamos la matriz lista para ser pintada en la ventana.
        return datos;
    }

    /*
     CLASE INTERNA: ErrorDetalle
     Esta es una clase auxiliar pequeña (un POJO) que sirve solo para agrupar
     los 3 datos de un error en una sola variable.
     Es como una "cajita" que guarda la información de cada error individual.
     */
    private static class ErrorDetalle {
        int linea;
        String fase;       // Quién lo encontró
        String descripcion; // Qué pasó

        public ErrorDetalle(int linea, String fase, String descripcion) {
            this.linea = linea;
            this.fase = fase;
            this.descripcion = descripcion;
        }
    }
}