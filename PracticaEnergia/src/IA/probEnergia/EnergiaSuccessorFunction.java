package IA.probEnergia;

import aima.search.framework.Successor;
import aima.search.framework.SuccessorFunction;

import java.util.ArrayList;
import java.util.List;

/*!
 * Genera todos los sucesores de un estado (Hill Climbing) con el conjunto de operadores elegido
 */
public class EnergiaSuccessorFunction implements SuccessorFunction {

    /// Conjuntos de operadores
    public static final int MOVER_DESASIGNAR = 0;
    public static final int MOVER_DESASIGNAR_INTERCAMBIAR = 1;

    /// Conjunto de operadores que se aplica
    private int operadores;

    public EnergiaSuccessorFunction(int operadores) {
        this.operadores = operadores;
    }

    public List<Successor> getSuccessors(Object aState) {
        ArrayList<Successor> retVal = new ArrayList<Successor>();
        EnergiaBoard board = (EnergiaBoard) aState;
        int n = board.getNumClientes();
        int m = board.getNumCentrales();

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (board.puedeMover(i, j)) {
                    EnergiaBoard nuevo = new EnergiaBoard(board);
                    nuevo.mover(i, j);
                    retVal.add(new Successor(EnergiaBoard.MOVER + " cliente " + i + " a central " + j, nuevo));
                }
            }
            if (board.puedeDesasignar(i)) {
                EnergiaBoard nuevo = new EnergiaBoard(board);
                nuevo.desasignar(i);
                retVal.add(new Successor(EnergiaBoard.DESASIGNAR + " cliente " + i, nuevo));
            }
        }

        if (operadores == MOVER_DESASIGNAR_INTERCAMBIAR) {
            for (int i1 = 0; i1 < n; i1++) {
                for (int i2 = i1 + 1; i2 < n; i2++) {
                    if (board.puedeIntercambiar(i1, i2)) {
                        EnergiaBoard nuevo = new EnergiaBoard(board);
                        nuevo.intercambiar(i1, i2);
                        retVal.add(new Successor(EnergiaBoard.INTERCAMBIAR + " clientes " + i1 + " y " + i2, nuevo));
                    }
                }
            }
        }

        return retVal;
    }
}
