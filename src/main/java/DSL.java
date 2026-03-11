// importamos la libreira java.util ya que nescesitaremos hash maps array list y sets 

import java.util.*;

/*ESTA CLASE IMPLEMENTA LOS SIGUIENTES METODOS         
 getEstadosAceptacionDSL retorna el conjunto estados de aceptación del automata




 */
public class DSL {

    /*
    // En este metodo creamos un set, el cual es un hash map que no puede tener elementos repetidos y a la vez 
    //tiene una gran utilidad  para las busquedas ya que tiene tiempos de busquedas rapidos.
    A este set  usamos el metodo Set.of el cual nos dará un valor estatico, estos valores serán nuestras palabrs reservadas 
    las cuales  serán nuestros estados de aceptacion en el automta 
    
    
 */
    public static Set<String> getEstadosAceptacion() {
        return Set.of(
                "PILA", "PILA_CIRCULAR", "COLA", "BICOLA", "LISTA_ENLAZADA", "LISTA_DOBLE_ENLAZADA", "LISTA_CIRCULAR", "ARBOL_BINARIO", "TABLA_HASH", "GRAFO",
                "INSERTAR", "INSERTAR_FINAL", "INSERTAR_INICIO", "INSERTAR_EN_POSICION", "INSERTARIZQUIERDA", "INSERTARDERECHA", "AGREGARNODO", "APILAR", "ENCOLAR", "PUSH", "ENQUEUE",
                "ELIMINAR", "ELIMINAR_INICIO", "ELIMINAR_FINAL", "ELIMINAR_FRENTE", "ELIMINAR_POSICION", "ELIMINARNODO", "DESAPILAR", "POP", "DESENCOLAR", "DEQUEUE",
                "BUSCAR", "TOPE", "FRENTE", "PEEK", "VERFILA", "FRONT", "CLAVE",
                "RECORRER", "RECORRERADELANTE", "RECORRERATRAS", "PREORDEN", "INORDEN", "POSTORDEN", "RECORRIDOPORNIVELES",
                "ACTUALIZAR", "REHASH", "AGREGARARISTA", "ELIMINARARISTA", "VECINOS", "BFS", "DFS", "CAMINOCORTO",
                "VACIA", "LLENA", "TAMANO", "ALTURA", "HOJAS", "NODOS",
                "EN", "CON", "VALOR", "CREAR",
                "MOSTRAR", "IF", "ELSE", "DO", "WHILE", "FOR","INSERTAR_FRENTE","Tamano"
        );
    }


    /*

    
    Este metodo  es el encargado de definir todos los estados , al igual lo hace con un set<>,
    El cual contendra los nombres de todos los estados que llevan a una palabra reservada
    
    El metodo se divide en 4 partes se declara un estado set <> el cual contendra todos los estados 
    a este metodo se le agrega el estado de inicio 
    se hace un for each que recorre todos los estados disponibles en los estados de aceptacion
    dentro de este for each hay un for anidado el cual agrega al set llamado todosLosEstados
    un nuevo estado que sera llamado igual que el estado que esta iterando -1 posicion 
    por ejemplo: 
    SE empieza con el estado de aceptacion PILA 
    Se agrega el estado PIL al set 
    luego el estado PI y asi sucesivamente hasta que se llega a una cadena de longitud 1 en este caso P
    en caso de que multiples estados empiecen en P el set los ignorará y solo agregará uno haciendo que tenga los menos estados posibles
    se repetirá hasta que no haya mas estados en el set de estados finales 
    
    
     */
    public static Set<String> getEstadosDSL() {
        Set<String> todosLosEstados = new HashSet<>();
        todosLosEstados.add("INICIO");

        for (String pr : getEstadosAceptacion()) {
            for (int i = 1; i <= pr.length(); i++) {
                todosLosEstados.add(pr.substring(0, i));
            }
        }
        return todosLosEstados;
    }

    /*
    Este metodo hace un poco de lo mismo solo que un poco más complejo
    Este estado retorna un Mapa que tiene como identificador un caracter
    El mapa contiene otro mapa lo cual nos hace tener tres campos los cuales simularän nuestras transiciones 
    siendo el primer string el nombre del estado el character el caracter con el que va y el segundo string al estado al que va 
    
     */
    public static Map<String, Map<Character, String>> getTransiciones() {
        Map<String, Map<Character, String>> transiciones = new HashMap<>();

        // fpr para rellenar el hash transiciones el campo estados obteniendo la informacion del hash pasado  
        for (String estado : getEstadosDSL()) {
            transiciones.put(estado, new HashMap<>());
        }

        /* para rellenar el los campos de caracter y estado destino se hace un for anidado se rellenará las tarnsiciones
        1.- se obtiene el primer caracter del estado de aceptacion 
        2.- se define su estadodestino como pr.substring (i+1)  
        3.- se define su estado de origen como pr.substring (0+i)
         
        para  que se aclare más usaremos como ejemplo la palabra pila
        obtenemos pila 
        obtenemos el character en la posicion i en este caso 0 que seria p 
        el estado de destino seria pi
        y se valida que el estado de inicio sea 0 , para que no sea el estado de incio y en caso de que no lo sea se obtiene 
        el substring de 0 hasta i en este caso p
         */
        for (String pr : getEstadosAceptacion()) {
            for (int i = 0; i < pr.length(); i++) {
                char simbolo = pr.charAt(i);
                String estadoDestino = pr.substring(0, i + 1);
                String origen = (i == 0) ? "INICIO" : pr.substring(0, i);

                transiciones.get(origen).put(simbolo, estadoDestino);
            }
        }
        //Retorna el estado de transiciones 
        return transiciones;
    }

// ahora hacemos el set con los alfabetos completos
    public static Set<Character> getAlfabetoDSL() {
        Set<Character> alfabeto = new HashSet<>();
        
        // 1. Letras Mayúsculas
        for (char c = 'A'; c <= 'Z'; c++) {
            alfabeto.add(c);
        }
        
        // 2. Letras Minúsculas (¡Faltaba esto para validar variables como 'miCola'!)
        for (char c = 'a'; c <= 'z'; c++) {
            alfabeto.add(c);
        }
        
        // 3. Números
        for (char c = '0'; c <= '9'; c++) {
            alfabeto.add(c);
        }
        
        // 4. Símbolos especiales y de puntuación (¡Faltaba esto para los ';' y otros operadores!)
        char[] simbolosExtras = {'_', ';', '=', '+', '-', '*', '/', '(', ')', '{', '}', '<', '>', '!', '&', '|', '.', '"', '[', ']', ','};
        for (char c : simbolosExtras) {
            alfabeto.add(c);
        }
        
        return alfabeto;
    }

    public static Token[] CrearToken(String entrada) {
        List<Token> listaTokens = new ArrayList<>();
        // se crea la regex para la validacion de los tokens con la siguiente jerarquia 
        /*
        1.- Comentarios
        2.- cadenas
        3.-operadores logicos
        4.- identificadores
        cualquier otra cosa 
         */
        String regex = "(//.*)|"
        + "(\"[^\"]*\")|" // Grupo 2: Cadenas de texto
        + "(==|!=|<=|>=|&&|\\|\\|)|"
        + "([\\Q(){}[]|,;=+-*/<>\u0021&|.\\E])|"
        + "([^\\s\\Q(){}[]|,;=+-*/<>\u0021&|.\\E\"]+)";
        // convertimos la regex en un patron para que se más rapido de ejecutrar
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);

        // crea un arreglo donde separa las lineas
        String[] lineas = entrada.split("\n");
        int numLinea = 1;
        // por cada linea 
        for (String lineaOriginal : lineas) {
            java.util.regex.Matcher matcher = pattern.matcher(lineaOriginal);
            // si encuentra una coincidencia 
            while (matcher.find()) {
                // iguala la variable token donde hay una coincidencia
                String token = matcher.group();

                // Grupo 1: Si es un comentario, lo ignoramos.
                if (matcher.group(1) != null) {
                    continue;
                }
                
                // Si el token es solo espacio en blanco, lo ignoramos.
                if (token.trim().isEmpty()) {
                    continue;
                }

                int columna = matcher.start() + 1;
                // ¡LÓGICA CORREGIDA!
                // Grupo 2: Si es una cadena de texto, creamos el token con el tipo "LITERAL_CADENA".
                if (matcher.group(2) != null) {
                    listaTokens.add(new Token(token, numLinea, columna, "LITERAL_CADENA"));
                } else {
                    // Para todo lo demás, creamos un token genérico que será clasificado después.
                    listaTokens.add(new Token(token, numLinea, columna));
                }
            }
            //una vez acaba todas las columnas aumenta la linea 

            numLinea++;
        }
                // una vez recorre todas las lineas convierte el arraylist en un arreglo 

        return listaTokens.toArray(new Token[0]);
    }

}
