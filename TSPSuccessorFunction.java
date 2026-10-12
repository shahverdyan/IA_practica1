import aima.search.framework.SuccessorFunction;
import java.util.*;

public class TSPSuccessorFunction implements SuccessorFunction {
    public TSPSuccessorFunction() {
    
    }
    
    public List getSuccessors(Object aState) {
        ArrayList retVal = new ArrayList();
        TSPBoard taulell = (TSPBoard) aState;
        TSPHeuristicFunction FHeristic = new TSPHeuristicFunction();
        
        for (int i = 0; i < board.getCentrals(); i++) {
            for (int j = 0; j < board.getCentrals(); j++) {
                
            }
        }
        
        return retVal;
    }
}
