package IA.probEnergia;

import aima.search.framework.HeuristicFunction;

/*!
 * Heuristica del experimento 5: beneficio con una penalizacion por cada MW de demanda garantizada no servida
 */
public class EnergiaHeuristicFunctionPenalizacion implements HeuristicFunction {

    /// Euros de penalizacion por MW garantizado no servido
    private double penalizacion;

    public EnergiaHeuristicFunctionPenalizacion(double penalizacion) {
        this.penalizacion = penalizacion;
    }

    public double getHeuristicValue(Object state) {
        EnergiaBoard board = (EnergiaBoard) state;
        return -board.getBeneficio() + penalizacion * board.getMWGarantizadosNoServidos();
    }
}
