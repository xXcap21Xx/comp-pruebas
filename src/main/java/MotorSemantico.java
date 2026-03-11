public class MotorSemantico {

    public static void ejecutar(NodoAST raiz, TablaSimbolos ts, TablaErrores te) {
        try {
            if (raiz == null) {
                return;
            }

            AnalizadorSemantico analizador = new AnalizadorSemantico(ts, te);
            analizador.analizar(raiz);

        } catch (Exception e) {
            te.reporte(0, "Semántico", "Error crítico en MotorSemantico: " + e.getMessage());
            e.printStackTrace();
        }
    }
}