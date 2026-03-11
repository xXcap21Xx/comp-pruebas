
// clase la cual modelo los tokens que pbrtendremos a partir de codigo en crudo
public class Token {

    private String lexema;
    private int linea;
    private String tipoToken;
    private boolean existeSimbolo;
    private int columna;

    
    //Constructor utilizado para crear tokens en el metodo CRearTokens
    public Token(String lexema, int linea, int columna) {
        this.lexema = lexema;
        this.linea = linea;
        this.columna = columna;
        this.tipoToken = "Pendiente";
        this.existeSimbolo = false;
    }

    // constructor para el metodo aceptar en la clase automata 
    public Token(String lexema, int linea, int columna, String tipoToken, String estadoFinal, boolean existeSimbolo) {
        this.lexema = lexema;
        this.linea = linea;
        this.columna = columna;
        this.tipoToken = tipoToken;
        this.existeSimbolo = existeSimbolo;
    }

    // Constructor para el método CrearToken cuando se identifica un tipo directamente (como CADENA)
    public Token(String lexema, int linea, int columna, String tipoToken) {
        this.lexema = lexema;
        this.linea = linea;
        this.columna = columna;
        this.tipoToken = tipoToken;
        // Asumimos que si se crea así, es un token válido y reconocido (como una cadena literal)
        this.existeSimbolo = true; 
    }

    // Getters
    public String getLexema() {
        return lexema;
    }

    public int getLinea() {
        return linea;
    }

    public int getColumna() {
        return columna;
    }

    public String getTipoToken() {
        return tipoToken;
    }

  
    public boolean existeSimbolo() {
        return existeSimbolo;
    }
}
