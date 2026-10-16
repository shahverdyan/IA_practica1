package IA.probEnergia;

import aima.search.framework.HeuristicFunction;

/*!
 * Heuristica del criterio de calidad del enunciado: los algoritmos minimizan, asi que es el beneficio cambiado de signo
 */
public class EnergiaHeuristicFunction implements HeuristicFunction {

    public double getHeuristicValue(Object state) {
        EnergiaBoard board = (EnergiaBoard) state;
        return -board.getBeneficio();
    }
}
