package IA.probEnergia;

import aima.search.framework.Successor;
import aima.search.framework.SuccessorFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/*!
 * Genera un unico sucesor (Simulated Annealing): un operador al azar con parametros al azar
 */
public class EnergiaSuccessorFunctionSA implements SuccessorFunction {

    /// Intentos para encontrar un operador aplicable antes de devolver la lista vacia
    private static final int MAX_INTENTOS = 1000;

    /// Conjunto de operadores (constantes de EnergiaSuccessorFunction)
    private int operadores;
    private Random random = new Random();

    public EnergiaSuccessorFunctionSA(int operadores) {
        this.operadores = operadores;
    }

    public List<Successor> getSuccessors(Object aState) {
        ArrayList<Successor> retVal = new ArrayList<Successor>();
        EnergiaBoard board = (EnergiaBoard) aState;
        int n = board.getNumClientes();
        int m = board.getNumCentrales();
        int numOperadores = (operadores == EnergiaSuccessorFunction.MOVER_DESASIGNAR_INTERCAMBIAR) ? 3 : 2;

        for (int intento = 0; intento < MAX_INTENTOS && retVal.isEmpty(); intento++) {
            int operador = random.nextInt(numOperadores);
            if (operador == 0) {
                int i = random.nextInt(n);
                int j = random.nextInt(m);
                if (board.puedeMover(i, j)) {
                    EnergiaBoard nuevo = new EnergiaBoard(board);
                    nuevo.mover(i, j);
                    retVal.add(new Successor(EnergiaBoard.MOVER + " cliente " + i + " a central " + j, nuevo));
                }
            } else if (operador == 1) {
                int i = random.nextInt(n);
                if (board.puedeDesasignar(i)) {
                    EnergiaBoard nuevo = new EnergiaBoard(board);
                    nuevo.desasignar(i);
                    retVal.add(new Successor(EnergiaBoard.DESASIGNAR + " cliente " + i, nuevo));
                }
            } else {
                int i1 = random.nextInt(n);
                int i2 = random.nextInt(n);
                if (board.puedeIntercambiar(i1, i2)) {
                    EnergiaBoard nuevo = new EnergiaBoard(board);
                    nuevo.intercambiar(i1, i2);
                    retVal.add(new Successor(EnergiaBoard.INTERCAMBIAR + " clientes " + i1 + " y " + i2, nuevo));
                }
            }
        }

        return retVal;
    }
}
