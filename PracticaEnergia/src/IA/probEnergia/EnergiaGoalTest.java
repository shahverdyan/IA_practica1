package IA.probEnergia;

import aima.search.framework.GoalTest;

/*!
 * En busqueda local no se conoce el estado final, nunca se para por haberlo encontrado
 */
public class EnergiaGoalTest implements GoalTest {

    public boolean isGoalState(Object aState) {
        return false;
    }
}
