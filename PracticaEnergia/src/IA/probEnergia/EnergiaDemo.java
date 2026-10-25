package IA.probEnergia;

import IA.Energia.Central;

import aima.search.framework.HeuristicFunction;
import aima.search.framework.Problem;
import aima.search.framework.Search;
import aima.search.framework.SearchAgent;
import aima.search.informed.HillClimbingSearch;
import aima.search.informed.Scheduler;
import aima.search.informed.SimulatedAnnealingSearch;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*!
 * Programa principal: demostracion y experimentos del enunciado (resultados en CSV separado por ;)
 */
public class EnergiaDemo {

    /// Escenario inicial del enunciado
    private static final int[] CENTRALES = {5, 10, 25};
    private static final int CLIENTES = 1000;
    private static final double[] PROP_CLIENTES = {0.25, 0.30, 0.45};
    private static final double PROP_GARANTIZADOS = 0.75;

    /// Valor de penalizacion que indica que se usa la heuristica original
    private static final double SIN_PENALIZACION = -1;

    /// Experimento 3: t0 es la iteracion en que la probabilidad de aceptar un estado DELTA_E_REFERENCIA euros peor
    /// baja de PROBABILIDAD_CERO (1 euro es la unidad del beneficio)
    private static final double DELTA_E_REFERENCIA = 1;
    private static final double PROBABILIDAD_CERO = 0.001;

    /// Opciones de la linea de comandos (clave=valor)
    private static Map<String, String> opciones = new HashMap<String, String>();

    /// Elementos que se fijan con los experimentos 1, 2 y 3 (se pueden cambiar con opciones)
    private static int operadores;
    private static int inicial;
    private static int saSteps;
    private static int saStiter;
    private static int saK;
    private static double saLambda;
    /// Repeticiones de cada configuracion (semillas 1..repeticiones)
    private static int repeticiones;

    /// Resultado de una busqueda
    private static class Ejecucion {
        EnergiaBoard fin;
        List<?> camino;
        List<?> acciones;
        int nodosExpandidos;
        long tiempo;
    }

    public static void main(String[] args) throws Exception {
        String modo = args.length > 0 ? args[0] : "demo";
        for (int a = 1; a < args.length; a++) {
            String[] claveValor = args[a].split("=");
            if (claveValor.length != 2) {
                uso();
                return;
            }
            opciones.put(claveValor[0], claveValor[1]);
        }

        operadores = conjuntoOperadores(opcion("ops", "A"));
        inicial = estrategiaInicial(opcion("init", "greedy"));
        // Parametros de SA elegidos en el experimento 3 (ver experiment3_SA.md)
        saSteps = Integer.parseInt(lista("steps", "260000")[0]);
        saStiter = Integer.parseInt(lista("stiter", "100")[0]);
        saK = Integer.parseInt(lista("k", "25")[0]);
        saLambda = Double.parseDouble(lista("lambda", "0.0001")[0]);
        repeticiones = Integer.parseInt(opcion("reps", "10"));

        if (modo.equals("demo")) demo();
        else if (modo.equals("traza")) traza();
        else if (modo.equals("exp1")) experimento1();
        else if (modo.equals("exp2")) experimento2();
        else if (modo.equals("exp3")) experimento3();
        else if (modo.equals("exp4")) experimento4();
        else if (modo.equals("exp5")) experimento5();
        else if (modo.equals("exp6")) experimento6();
        else uso();
    }

    /*!\brief Hill Climbing y Simulated Annealing sobre el escenario del enunciado, con salida legible
     */
    private static void demo() throws Exception {
        int seed = Integer.parseInt(opcion("seed", "1"));
        EnergiaBoard inicio = new EnergiaBoard(CENTRALES, CLIENTES, PROP_CLIENTES, PROP_GARANTIZADOS, seed, inicial);
        System.out.println("Solucion inicial: " + inicio);

        System.out.println("\nHill Climbing  -->");
        Ejecucion hc = ejecutar(inicio, false, new EnergiaHeuristicFunction());
        for (Object accion : hc.acciones) System.out.println(accion);
        System.out.println("Solucion final: " + hc.fin);
        System.out.println("Pasos: " + hc.camino.size() + "  Nodos expandidos: " + hc.nodosExpandidos + "  Tiempo: " + hc.tiempo + " ms");

        System.out.println("\nSimulated Annealing (steps=" + saSteps + " stiter=" + saStiter + " k=" + saK + " lambda=" + saLambda + ")  -->");
        Ejecucion sa = ejecutar(inicio, true, new EnergiaHeuristicFunction());
        System.out.println("Solucion final: " + sa.fin);
        System.out.println("Pasos aceptados hasta la mejor solucion: " + sa.camino.size() + "  Tiempo: " + sa.tiempo + " ms");
    }

    /*!\brief Beneficio de cada estado del camino de Hill Climbing y Simulated Annealing (para graficas de evolucion)
     */
    private static void traza() throws Exception {
        int seed = Integer.parseInt(opcion("seed", "1"));
        System.out.println("algoritmo;paso;beneficio");
        for (int a = 0; a < 2; a++) {
            boolean simulatedAnnealing = (a == 1);
            String nombre = simulatedAnnealing ? "SA" : "HC";
            EnergiaBoard inicio = new EnergiaBoard(CENTRALES, CLIENTES, PROP_CLIENTES, PROP_GARANTIZADOS, seed, inicial);
            Ejecucion e = ejecutar(inicio, simulatedAnnealing, new EnergiaHeuristicFunction());
            System.out.println(nombre + ";0;" + Math.round(inicio.getBeneficio()));
            for (int p = 0; p < e.camino.size(); p++) {
                System.out.println(nombre + ";" + (p + 1) + ";" + Math.round(((EnergiaBoard) e.camino.get(p)).getBeneficio()));
            }
        }
    }

    /*!\brief Experimento 1: conjuntos de operadores con Hill Climbing
     */
    private static void experimento1() throws Exception {
        int ini = opciones.containsKey("init") ? inicial : EnergiaBoard.INICIAL_ORDENADA;
        calentar();
        cabecera();
        String[] conjuntos = {"A", "B"};
        for (String conjunto : conjuntos) {
            operadores = conjuntoOperadores(conjunto);
            List<double[]> datos = new ArrayList<double[]>();
            for (int seed = 1; seed <= repeticiones; seed++) {
                datos.add(experimentar("1", "operadores=" + conjunto, CENTRALES, CLIENTES, ini, false, SIN_PENALIZACION, seed));
            }
            resumen("operadores=" + conjunto, "HC", datos);
        }
    }

    /*!\brief Experimento 2: estrategias de solucion inicial con Hill Climbing
     */
    private static void experimento2() throws Exception {
        calentar();
        cabecera();
        String[] estrategias = {"ordenada", "greedy"};
        for (String estrategia : estrategias) {
            List<double[]> datos = new ArrayList<double[]>();
            for (int seed = 1; seed <= repeticiones; seed++) {
                datos.add(experimentar("2", "inicial=" + estrategia, CENTRALES, CLIENTES, estrategiaInicial(estrategia), false, SIN_PENALIZACION, seed));
            }
            resumen("inicial=" + estrategia, "HC", datos);
        }
    }

    /*!\brief Experimento 3: parametros de Simulated Annealing, con Hill Climbing de referencia, en tres fases (opcion fase):
     *        kl:     todas las combinaciones de k y lambda, con steps grande y stiter fijo
     *        steps:  con k y lambda elegidos, steps = factor * t0
     *        stiter: con k, lambda y steps elegidos, varios divisores de steps
     */
    private static void experimento3() throws Exception {
        String fase = opcion("fase", "kl");
        if (!fase.equals("kl") && !fase.equals("steps") && !fase.equals("stiter")) {
            uso();
            return;
        }
        calentar();
        cabecera();
        List<double[]> referencia = new ArrayList<double[]>();
        for (int seed = 1; seed <= repeticiones; seed++) {
            referencia.add(experimentar("3", "referencia", CENTRALES, CLIENTES, inicial, false, SIN_PENALIZACION, seed));
        }
        resumen("referencia", "HC", referencia);
        System.out.println("# t0 = iteracion en que la probabilidad de aceptar un estado " + texto(DELTA_E_REFERENCIA)
                + " euro peor baja de " + texto(PROBABILIDAD_CERO)
                + "; iteraciones_reales = las que hace AIMA (para antes si la temperatura llega a 0)");

        if (fase.equals("kl")) {
            int steps = Integer.parseInt(opcion("steps", "500000"));
            for (String k : lista("k", "1,5,25,125,625,3125,15625")) {
                for (String lambda : lista("lambda", "1,0.01,0.0001")) {
                    configuracionSA(steps, saStiter, Integer.parseInt(k), Double.parseDouble(lambda));
                }
            }
        } else if (fase.equals("steps")) {
            int t0 = iteracionProbabilidadCero(saK, saLambda, saStiter);
            for (String factor : lista("factores", "1,2,5,10,20")) {
                configuracionSA(Integer.parseInt(factor) * t0, saStiter, saK, saLambda);
            }
        } else {
            List<Integer> valores = new ArrayList<Integer>();
            if (opciones.containsKey("stiter")) {
                for (String s : lista("stiter", "")) valores.add(Integer.parseInt(s));
            } else {
                for (int s = 1; s <= 10000 && s < saSteps; s *= 10) valores.add(s);
                valores.add(saSteps);   // la temperatura no cambia nunca: SA sin enfriamiento
            }
            for (int stiter : valores) {
                if (saSteps % stiter != 0) System.out.println("# stiter=" + stiter + " no divide steps=" + saSteps + ": se omite");
                else configuracionSA(saSteps, stiter, saK, saLambda);
            }
        }
    }

    /*!\brief Repeticiones de una configuracion de Simulated Annealing del experimento 3
     */
    private static void configuracionSA(int steps, int stiter, int k, double lambda) throws Exception {
        saSteps = steps;
        saStiter = stiter;
        saK = k;
        saLambda = lambda;
        String configuracion = "steps=" + steps + " stiter=" + stiter + " k=" + k + " lambda=" + texto(lambda);
        int t0 = iteracionProbabilidadCero(k, lambda, stiter);
        System.out.println("# " + configuracion + ": t0=" + t0 + " steps/t0=" + redondear((double) steps / t0)
                + " iteraciones_reales=" + iteracionesReales(steps, stiter, k, lambda));
        List<double[]> datos = new ArrayList<double[]>();
        for (int seed = 1; seed <= repeticiones; seed++) {
            datos.add(experimentar("3", configuracion, CENTRALES, CLIENTES, inicial, true, SIN_PENALIZACION, seed));
        }
        resumen(configuracion, "SA", datos);
    }

    /*!\brief t0: primera iteracion (multiplo de stiter) en que la probabilidad de aceptar un estado DELTA_E_REFERENCIA
     *        euros peor baja de PROBABILIDAD_CERO
     */
    private static int iteracionProbabilidadCero(int k, double lambda, int stiter) {
        // AIMA acepta un estado peor con probabilidad 1 / (1 + e^(|deltaE| / T)), que es menor que p si T < |deltaE| / ln(1/p - 1)
        double temperaturaCero = DELTA_E_REFERENCIA / Math.log(1 / PROBABILIDAD_CERO - 1);
        if (k < temperaturaCero) return 0;
        // T = k * e^(-lambda * t) baja de temperaturaCero cuando t > ln(k / temperaturaCero) / lambda
        return (int) Math.ceil(Math.log(k / temperaturaCero) / lambda / stiter) * stiter;
    }

    /*!\brief Iteraciones que hace realmente SA: AIMA para antes de steps si la temperatura llega a ser exactamente 0
     */
    private static int iteracionesReales(int steps, int stiter, int k, double lambda) {
        Scheduler scheduler = new Scheduler(k, lambda, stiter);
        for (int t = 0; t < steps; t += stiter) {
            if (scheduler.getTemp(t) == 0.0) return t;
        }
        return steps;
    }

    /*!\brief Experimento 4: tiempo de Hill Climbing al crecer el numero de clientes y el de centrales
     */
    private static void experimento4() throws Exception {
        calentar();
        cabecera();
        for (String c : lista("clientes", "250,500,750,1000,1250,1500")) {
            if (!serieTamano("centrales=40 clientes=" + c, CENTRALES, Integer.parseInt(c))) break;
        }
        for (String c : lista("centrales", "40,80,120,160,200,240")) {
            int total = Integer.parseInt(c);
            int tipoA = total * 5 / 40;
            int tipoB = total * 10 / 40;
            int[] centrales = {tipoA, tipoB, total - tipoA - tipoB};
            if (!serieTamano("centrales=" + c + " clientes=" + CLIENTES, centrales, CLIENTES)) break;
        }
    }

    /*!\brief Repeticiones de una configuracion del experimento 4; devuelve false si ninguna tiene solucion inicial valida
     */
    private static boolean serieTamano(String configuracion, int[] centrales, int clientes) throws Exception {
        List<double[]> datos = new ArrayList<double[]>();
        for (int seed = 1; seed <= repeticiones; seed++) {
            try {
                datos.add(experimentar("4", configuracion, centrales, clientes, inicial, false, SIN_PENALIZACION, seed));
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                System.out.println("# " + configuracion + " semilla " + seed + " sin solucion inicial valida: " + e.getMessage());
            }
        }
        resumen(configuracion, "HC", datos);
        return !datos.isEmpty();
    }

    /*!\brief Experimento 5: penalizacion por no servir garantizados, partiendo de la solucion vacia
     */
    private static void experimento5() throws Exception {
        calentar();
        cabecera();
        for (String p : lista("penal", "0,100,500,1000,2000,5000,10000")) {
            for (int a = 0; a < 2; a++) {
                boolean simulatedAnnealing = (a == 1);
                List<double[]> datos = new ArrayList<double[]>();
                for (int seed = 1; seed <= repeticiones; seed++) {
                    datos.add(experimentar("5", "penalizacion=" + p, CENTRALES, CLIENTES, EnergiaBoard.INICIAL_VACIA,
                            simulatedAnnealing, Double.parseDouble(p), seed));
                }
                resumen("penalizacion=" + p, simulatedAnnealing ? "SA" : "HC", datos);
            }
        }
    }

    /*!\brief Experimento 6: duplicar y triplicar las centrales de tipo C
     */
    private static void experimento6() throws Exception {
        calentar();
        cabecera();
        for (String f : lista("factorC", "1,2,3")) {
            int[] centrales = {CENTRALES[0], CENTRALES[1], CENTRALES[2] * Integer.parseInt(f)};
            for (int a = 0; a < 2; a++) {
                boolean simulatedAnnealing = (a == 1);
                List<double[]> datos = new ArrayList<double[]>();
                for (int seed = 1; seed <= repeticiones; seed++) {
                    datos.add(experimentar("6", "factorC=" + f, centrales, CLIENTES, inicial, simulatedAnnealing, SIN_PENALIZACION, seed));
                }
                resumen("factorC=" + f, simulatedAnnealing ? "SA" : "HC", datos);
            }
        }
    }

    /*!\brief Ejecuta Hill Climbing o Simulated Annealing desde la solucion inicial
     */
    private static Ejecucion ejecutar(EnergiaBoard inicio, boolean simulatedAnnealing, HeuristicFunction heuristica) throws Exception {
        Problem problem;
        Search search;
        if (simulatedAnnealing) {
            problem = new Problem(inicio, new EnergiaSuccessorFunctionSA(operadores), new EnergiaGoalTest(), heuristica);
            SimulatedAnnealingSearch sa = new SimulatedAnnealingSearch(saSteps, saStiter, saK, saLambda);
            if (opcion("trazaSA", "no").equals("si")) sa.traceOn();
            search = sa;
        } else {
            problem = new Problem(inicio, new EnergiaSuccessorFunction(operadores), new EnergiaGoalTest(), heuristica);
            search = new HillClimbingSearch();
        }

        Ejecucion e = new Ejecucion();
        long t0 = System.currentTimeMillis();
        SearchAgent agent = new SearchAgent(problem, search);
        e.tiempo = System.currentTimeMillis() - t0;
        e.fin = (EnergiaBoard) search.getGoalState();
        e.camino = search.getPathStates();
        e.acciones = agent.getActions();
        e.nodosExpandidos = search.getMetrics().getInt("nodesExpanded");
        return e;
    }

    /*!\brief Genera el escenario de una semilla, hace la busqueda, escribe una fila CSV y devuelve las medidas para el resumen
     */
    private static double[] experimentar(String experimento, String configuracion, int[] centrales, int clientes,
                                         int ini, boolean simulatedAnnealing, double penalizacion, int seed) throws Exception {
        EnergiaBoard.setGarantizadosObligatorios(penalizacion == SIN_PENALIZACION);
        EnergiaBoard inicio = new EnergiaBoard(centrales, clientes, PROP_CLIENTES, PROP_GARANTIZADOS, seed, ini);
        HeuristicFunction heuristica;
        if (penalizacion == SIN_PENALIZACION) heuristica = new EnergiaHeuristicFunction();
        else heuristica = new EnergiaHeuristicFunctionPenalizacion(penalizacion);

        Ejecucion e = ejecutar(inicio, simulatedAnnealing, heuristica);
        EnergiaBoard fin = e.fin;
        System.out.println(experimento + ";" + configuracion + ";" + seed + ";" + (simulatedAnnealing ? "SA" : "HC") + ";"
                + Math.round(inicio.getBeneficio()) + ";" + Math.round(fin.getBeneficio()) + ";" + e.tiempo + ";"
                + e.camino.size() + ";" + e.nodosExpandidos + ";"
                + fin.getClientesServidos(true) + ";" + fin.getClientesServidos(false) + ";"
                + Math.round(fin.getMWGarantizadosNoServidos()) + ";"
                + fin.getCentralesEnMarcha(Central.CENTRALA) + ";" + fin.getCentralesEnMarcha(Central.CENTRALB) + ";"
                + fin.getCentralesEnMarcha(Central.CENTRALC) + ";"
                + Math.round(fin.getMWAsignados(Central.CENTRALA)) + ";" + Math.round(fin.getMWAsignados(Central.CENTRALB)) + ";"
                + Math.round(fin.getMWAsignados(Central.CENTRALC)) + ";" + Math.round(fin.getMWPerdidos()));

        return new double[]{fin.getBeneficio(), e.tiempo, e.camino.size(), fin.getMWGarantizadosNoServidos(),
                fin.getCentralesEnMarcha(Central.CENTRALA), fin.getCentralesEnMarcha(Central.CENTRALB),
                fin.getCentralesEnMarcha(Central.CENTRALC), fin.getMWPerdidos()};
    }

    /*!\brief Busqueda previa que no se mide, para que la JVM compile el codigo antes de medir tiempos
     */
    private static void calentar() throws Exception {
        EnergiaBoard.setGarantizadosObligatorios(true);
        EnergiaBoard inicio = new EnergiaBoard(CENTRALES, 200, PROP_CLIENTES, PROP_GARANTIZADOS, 0, EnergiaBoard.INICIAL_ORDENADA);
        ejecutar(inicio, false, new EnergiaHeuristicFunction());
    }

    private static void cabecera() {
        System.out.println("experimento;configuracion;semilla;algoritmo;beneficio_inicial;beneficio;tiempo_ms;pasos;"
                + "nodos_expandidos;garantizados_servidos;no_garantizados_servidos;MW_garantizados_no_servidos;"
                + "centrales_A;centrales_B;centrales_C;MW_A;MW_B;MW_C;MW_perdidos");
    }

    /*!\brief Medias de las repeticiones de una configuracion (linea de comentario del CSV)
     */
    private static void resumen(String configuracion, String algoritmo, List<double[]> datos) {
        if (datos.isEmpty()) return;
        int validas = 0;
        for (double[] d : datos) if (d[3] == 0) validas++;
        System.out.println("# " + configuracion + " " + algoritmo + ": beneficio=" + Math.round(media(datos, 0))
                + " (desv " + Math.round(desviacion(datos, 0)) + ") tiempo_ms=" + Math.round(media(datos, 1))
                + " (desv " + Math.round(desviacion(datos, 1)) + ") pasos=" + Math.round(media(datos, 2))
                + " validas=" + validas + "/" + datos.size()
                + " centrales(A,B,C)=" + redondear(media(datos, 4)) + "," + redondear(media(datos, 5)) + "," + redondear(media(datos, 6))
                + " MW_perdidos=" + Math.round(media(datos, 7)));
    }

    private static double media(List<double[]> datos, int medida) {
        double suma = 0;
        for (double[] d : datos) suma += d[medida];
        return suma / datos.size();
    }

    private static double desviacion(List<double[]> datos, int medida) {
        if (datos.size() < 2) return 0;
        double m = media(datos, medida);
        double suma = 0;
        for (double[] d : datos) suma += (d[medida] - m) * (d[medida] - m);
        return Math.sqrt(suma / (datos.size() - 1));
    }

    private static double redondear(double x) {
        return Math.round(x * 10) / 10.0;
    }

    /// Numero sin notacion cientifica (0.0001 en lugar de 1.0E-4)
    private static String texto(double x) {
        return BigDecimal.valueOf(x).stripTrailingZeros().toPlainString();
    }

    private static String opcion(String clave, String defecto) {
        return opciones.containsKey(clave) ? opciones.get(clave) : defecto;
    }

    private static String[] lista(String clave, String defecto) {
        return opcion(clave, defecto).split(",");
    }

    private static int conjuntoOperadores(String nombre) {
        if (nombre.equals("A")) return EnergiaSuccessorFunction.MOVER_DESASIGNAR;
        if (nombre.equals("B")) return EnergiaSuccessorFunction.MOVER_DESASIGNAR_INTERCAMBIAR;
        throw new IllegalArgumentException("Conjunto de operadores desconocido: " + nombre);
    }

    private static int estrategiaInicial(String nombre) {
        if (nombre.equals("ordenada")) return EnergiaBoard.INICIAL_ORDENADA;
        if (nombre.equals("greedy")) return EnergiaBoard.INICIAL_GREEDY;
        if (nombre.equals("vacia")) return EnergiaBoard.INICIAL_VACIA;
        throw new IllegalArgumentException("Estrategia inicial desconocida: " + nombre);
    }

    private static void uso() {
        System.out.println("Uso: java IA.probEnergia.EnergiaDemo [modo] [opcion=valor ...]");
        System.out.println("  modos: demo (por defecto), traza, exp1, exp2, exp3, exp4, exp5, exp6");
        System.out.println("  opciones: ops=A|B  init=ordenada|greedy|vacia  reps=10  seed=1");
        System.out.println("            steps=260000 stiter=100 k=25 lambda=0.0001 (Simulated Annealing)");
        System.out.println("            exp3: fase=kl (listas k=1,5,... lambda=1,0.01,... con steps=500000)");
        System.out.println("                  fase=steps (factores=1,2,5,10,20: steps = factor * t0)");
        System.out.println("                  fase=stiter (lista stiter=1,10,...; solo los divisores de steps)");
        System.out.println("            trazaSA=si (muestra cada estado peor que acepta Simulated Annealing)");
        System.out.println("            clientes=250,500,... centrales=40,80,... (exp4)  penal=0,100,... (exp5)  factorC=1,2,3 (exp6)");
    }
}
