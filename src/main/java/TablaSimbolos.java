
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.Set;
import java.util.HashSet;

/*
 ESTA CLASE ES LA "MEMORIA" DEL COMPILADOR (TABLA DE SÍMBOLOS).
 Ahora soporta "Ámbitos" (Scope). Las variables locales creadas dentro de 
 un IF o FOR se destruirán automáticamente al terminar el bloque.
 */
public class TablaSimbolos {

    // Ahora usamos una Pila de Mapas. 
    // La caja 0 es el ámbito global. La caja 1, 2, etc., son los ámbitos locales.
    private final Stack<Map<String, Simbolo>> pilaAmbitos;
  

    public TablaSimbolos() {
        this.pilaAmbitos = new Stack<>();
        // Al arrancar, creamos el Ámbito Global por defecto
        this.pilaAmbitos.push(new HashMap<>());
    }

    // --- MAGIA DE LOS ÁMBITOS (SCOPE) ---
    // Se llama cuando el compilador entra a un IF, WHILE, FOR, etc.
    public void entrarAmbito() {
        pilaAmbitos.push(new HashMap<>());
    }

    // Se llama cuando el compilador sale de la llave de cierre "}"
    public void salirAmbito() {
        if (pilaAmbitos.size() > 1) { // Protegemos el ámbito global para no borrarlo
            pilaAmbitos.pop(); // Destruimos la caja temporal y todas sus variables
        }
    }

    // --- FUNCIONES TRADICIONALES ---
    public void insertar(String nombre, String tipo, Object valor) {
        // Siempre se guarda en la caja de hasta arriba (el ámbito actual)
        pilaAmbitos.peek().put(nombre, new Simbolo(nombre, tipo, valor));
    }

    public boolean existe(String nombre) {
        // Busca de arriba hacia abajo (Primero en las locales, luego en la global)
        for (int i = pilaAmbitos.size() - 1; i >= 0; i--) {
            if (pilaAmbitos.get(i).containsKey(nombre)) {
                return true;
            }
        }
        return false;
    }

    public Simbolo getSimbolo(String nombre) {
        // Retorna la primera que encuentre de arriba hacia abajo
        for (int i = pilaAmbitos.size() - 1; i >= 0; i--) {
            if (pilaAmbitos.get(i).containsKey(nombre)) {
                return pilaAmbitos.get(i).get(nombre);
            }
        }
        return null;
    }

    public void limpiar() {
        pilaAmbitos.clear();
        pilaAmbitos.push(new HashMap<>());
    }

    // --- PARA TU INTERFAZ GRÁFICA ---
    // Este método une todas las variables vivas para que tu JTable no se rompa
    public Map<String, Simbolo> getTodosLosSimbolos() {
        Map<String, Simbolo> todos = new HashMap<>();
        for (Map<String, Simbolo> ambito : pilaAmbitos) {
            todos.putAll(ambito);
        }
        return todos;
    }
    
    

public static class Simbolo {

    String nombre;
    String tipo;
    Object valor;
      public int tamano = 0;
    Set<String> nodosGrafo;
    Set<String> valoresInsertados; // Para TABLA_HASH y listas si quieres
    Set<String> aristasInsertadas;

    public Simbolo(String nombre, String tipo, Object valor) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.valor = valor;
        this.nodosGrafo = new HashSet<>();
        this.valoresInsertados = new HashSet<>();
        this.aristasInsertadas = new HashSet<>();
    }
}

public void eliminar(String nombre) {

    // Buscar desde el ámbito más interno hacia el global
    for (int i = pilaAmbitos.size() - 1; i >= 0; i--) {

        Map<String, Simbolo> ambito = pilaAmbitos.get(i);

        if (ambito.containsKey(nombre)) {
            ambito.remove(nombre);
            return;
        }
    }
}





}
