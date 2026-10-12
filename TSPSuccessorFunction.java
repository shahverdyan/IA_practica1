import aima.search.framework.SuccessorFunction;
import java.util.*;
//importar altres classes de la mateixa carpeta ???

public class TSPSuccessorFunction implements SuccessorFunction {
    public TSPSuccessorFunction() {
    
    }

    //Et retorna una llista amb tots els taulells resultants en intercanviar les posicions de totes les Centrals
    //Potser variar swapCities per una altre opcio
    public List getSuccessors(Object aState) {
        ArrayList LlistaFinal = new ArrayList();
        TSPBoard taulell = (TSPBoard) aState;
        TSPHeuristicFunction FHeuristic = new TSPHeuristicFunction();
        
        for (int i = 0; i < board.getCentrals(); i++) {
            for (int j = 0; j < board.getCentrals(); j++) {
                TSPBoard newBoard = new TSPBoard(board.getCentrals(), board.getPath(), board.getDists());
                newBoard.swapCities(i, j);

                int ValorHeuristic = FHeuristic.getHeuristicValue(newBoard);
                String IntercanviFet = TSPBoard.INTERCAMBIO + " " + i + " " + j + " Cost(" + V + ") -----> " + newBoard.toString();

                LlistaFinal.add(new Successor(S, newBoard));
            }
        }
        
        return LlistaFinal;
    }
}
