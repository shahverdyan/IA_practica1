/*!
 * Representacion del estado del problema de asignacion de clientes a centrales electricas
 */
package IA.probEnergia;

import IA.Energia.Central;
import IA.Energia.Centrales;
import IA.Energia.Cliente;
import IA.Energia.Clientes;
import IA.Energia.VEnergia;

import java.util.Arrays;

public class EnergiaBoard {

    /// Strings que describen los operadores
    public static final String MOVER = "Mover";
    public static final String DESASIGNAR = "Desasignar";
    public static final String INTERCAMBIAR = "Intercambiar";

    /// Estrategias para generar la solucion inicial
    public static final int INICIAL_ORDENADA = 0;
    public static final int INICIAL_GREEDY = 1;
    /// Solucion inicial sin ningun cliente servido (solo para el experimento con penalizacion)
    public static final int INICIAL_VACIA = 2;

    /// Valor de la asignacion de un cliente al que no se sirve
    public static final int SIN_CENTRAL = -1;

    // Datos del problema: no cambian durante la busqueda y los comparten todos los estados

    /// Centrales y clientes del escenario
    private static Centrales centrales;
    private static Clientes clientes;
    /// Consumo en MW del cliente i
    private static double[] consumo;
    /// MW que ha de producir la central j para servir al cliente i (consumo + perdida por distancia)
    private static double[][] consumoReal;
    /// Euros que paga el cliente i si se le sirve
    private static double[] ingreso;
    /// Euros de indemnizacion si no se sirve al cliente i (0 si es garantizado)
    private static double[] indemnizacion;
    /// Si el cliente i tiene servicio garantizado
    private static boolean[] garantizado;
    /// Tipo y produccion en MW de la central j
    private static int[] tipoCentral;
    private static double[] capacidad;
    /// Coste diario de la central j en marcha y parada
    private static double[] costeMarcha;
    private static double[] costeParada;
    /// Si es false, los clientes garantizados pueden quedar sin servir (experimento con penalizacion)
    private static boolean garantizadosObligatorios = true;

    // Datos del estado

    /// Central de cada cliente (SIN_CENTRAL si no se le sirve)
    private int[] asignacion;
    /// MW que produce cada central para sus clientes
    private double[] produccionUsada;
    /// Numero de clientes de cada central (esta en marcha si tiene alguno)
    private int[] clientesCentral;

    /*!\brief Genera un escenario aleatorio y su solucion inicial segun la estrategia indicada
     */
    public EnergiaBoard(int[] numCentrales, int numClientes, double[] propClientes, double propGarantizados,
                        int seed, int estrategia) throws Exception {
        centrales = new Centrales(numCentrales, seed);
        clientes = new Clientes(numClientes, propClientes, propGarantizados, seed);
        precalcularDatos();

        asignacion = new int[clientes.size()];
        Arrays.fill(asignacion, SIN_CENTRAL);
        produccionUsada = new double[centrales.size()];
        clientesCentral = new int[centrales.size()];

        if (estrategia == INICIAL_ORDENADA) generarInicialOrdenada();
        else if (estrategia == INICIAL_GREEDY) generarInicialGreedy();
    }

    /*!\brief Crea una copia del estado (los datos del problema no se copian, son compartidos)
     */
    public EnergiaBoard(EnergiaBoard b) {
        asignacion = b.asignacion.clone();
        produccionUsada = b.produccionUsada.clone();
        clientesCentral = b.clientesCentral.clone();
    }

    /*!\brief Indica si los operadores han de mantener servidos a todos los clientes garantizados
     */
    public static void setGarantizadosObligatorios(boolean obligatorios) {
        garantizadosObligatorios = obligatorios;
    }

    /*!\brief Calcula una sola vez los datos que usan todos los estados durante la busqueda
     */
    private static void precalcularDatos() throws Exception {
        int n = clientes.size();
        int m = centrales.size();

        tipoCentral = new int[m];
        capacidad = new double[m];
        costeMarcha = new double[m];
        costeParada = new double[m];
        for (int j = 0; j < m; j++) {
            Central c = centrales.get(j);
            tipoCentral[j] = c.getTipo();
            capacidad[j] = c.getProduccion();
            costeMarcha[j] = c.getProduccion() * VEnergia.getCosteProduccionMW(c.getTipo()) + VEnergia.getCosteMarcha(c.getTipo());
            costeParada[j] = VEnergia.getCosteParada(c.getTipo());
        }

        consumo = new double[n];
        consumoReal = new double[n][m];
        ingreso = new double[n];
        indemnizacion = new double[n];
        garantizado = new boolean[n];
        for (int i = 0; i < n; i++) {
            Cliente cl = clientes.get(i);
            consumo[i] = cl.getConsumo();
            garantizado[i] = (cl.getContrato() == Cliente.GARANTIZADO);
            if (garantizado[i]) {
                ingreso[i] = consumo[i] * VEnergia.getTarifaClienteGarantizada(cl.getTipo());
                indemnizacion[i] = 0;
            } else {
                ingreso[i] = consumo[i] * VEnergia.getTarifaClienteNoGarantizada(cl.getTipo());
                indemnizacion[i] = consumo[i] * VEnergia.getTarifaClientePenalizacion(cl.getTipo());
            }
            for (int j = 0; j < m; j++) {
                consumoReal[i][j] = consumo[i] * (1 + VEnergia.getPerdida(distancia(i, j)));
            }
        }
    }

    /*!\brief Distancia euclidea entre el cliente i y la central j
     */
    private static double distancia(int i, int j) {
        Cliente cl = clientes.get(i);
        Central c = centrales.get(j);
        double dx = cl.getCoordX() - c.getCoordX();
        double dy = cl.getCoordY() - c.getCoordY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    /*!\brief Solucion inicial: cada cliente garantizado a la primera central (por indice) donde cabe
     */
    private void generarInicialOrdenada() throws Exception {
        for (int i = 0; i < asignacion.length; i++) {
            if (garantizado[i]) {
                int j = 0;
                while (j < capacidad.length && !cabe(i, j)) j++;
                if (j == capacidad.length) throw new Exception("No hay capacidad para el cliente garantizado " + i);
                poner(i, j);
            }
        }
    }

    /*!\brief Solucion inicial: garantizados a la central mas cercana con espacio y despues
     *        no garantizados a la central en marcha mas cercana con espacio
     */
    private void generarInicialGreedy() throws Exception {
        for (int i = 0; i < asignacion.length; i++) {
            if (garantizado[i]) {
                int j = centralMasCercana(i, false);
                if (j == SIN_CENTRAL) throw new Exception("No hay capacidad para el cliente garantizado " + i);
                poner(i, j);
            }
        }
        for (int i = 0; i < asignacion.length; i++) {
            if (!garantizado[i]) {
                int j = centralMasCercana(i, true);
                if (j != SIN_CENTRAL) poner(i, j);
            }
        }
    }

    /*!\brief Central mas cercana al cliente i donde cabe (si soloEnMarcha, solo entre las que tienen clientes)
     */
    private int centralMasCercana(int i, boolean soloEnMarcha) {
        int mejor = SIN_CENTRAL;
        double mejorDistancia = Double.MAX_VALUE;
        for (int j = 0; j < capacidad.length; j++) {
            if (cabe(i, j) && (!soloEnMarcha || clientesCentral[j] > 0)) {
                double d = distancia(i, j);
                if (d < mejorDistancia) {
                    mejorDistancia = d;
                    mejor = j;
                }
            }
        }
        return mejor;
    }

    /*!\brief Indica si al cliente i le cabe en la produccion que le queda libre a la central j
     */
    private boolean cabe(int i, int j) {
        return produccionUsada[j] + consumoReal[i][j] <= capacidad[j];
    }

    /*!\brief Operador mover: asigna el cliente i a la central j (estuviera servido o no)
     */
    public boolean puedeMover(int i, int j) {
        return asignacion[i] != j && cabe(i, j);
    }

    public void mover(int i, int j) {
        quitar(i);
        poner(i, j);
    }

    /*!\brief Operador desasignar: deja de servir al cliente i (a un garantizado solo si no son obligatorios)
     */
    public boolean puedeDesasignar(int i) {
        return asignacion[i] != SIN_CENTRAL && (!garantizado[i] || !garantizadosObligatorios);
    }

    public void desasignar(int i) {
        quitar(i);
    }

    /*!\brief Operador intercambiar: los clientes i1 e i2 se intercambian la central (uno puede no estar servido)
     */
    public boolean puedeIntercambiar(int i1, int i2) {
        int c1 = asignacion[i1];
        int c2 = asignacion[i2];
        if (c1 == c2) return false;
        if (garantizadosObligatorios && ((c1 == SIN_CENTRAL && garantizado[i2]) || (c2 == SIN_CENTRAL && garantizado[i1]))) return false;
        if (c1 != SIN_CENTRAL && produccionUsada[c1] - consumoReal[i1][c1] + consumoReal[i2][c1] > capacidad[c1]) return false;
        if (c2 != SIN_CENTRAL && produccionUsada[c2] - consumoReal[i2][c2] + consumoReal[i1][c2] > capacidad[c2]) return false;
        return true;
    }

    public void intercambiar(int i1, int i2) {
        int c1 = asignacion[i1];
        int c2 = asignacion[i2];
        quitar(i1);
        quitar(i2);
        if (c2 != SIN_CENTRAL) poner(i1, c2);
        if (c1 != SIN_CENTRAL) poner(i2, c1);
    }

    /*!\brief Quita el cliente i de su central, si tiene
     */
    private void quitar(int i) {
        int j = asignacion[i];
        if (j != SIN_CENTRAL) {
            produccionUsada[j] -= consumoReal[i][j];
            clientesCentral[j]--;
            asignacion[i] = SIN_CENTRAL;
        }
    }

    /*!\brief Asigna el cliente i (sin central) a la central j
     */
    private void poner(int i, int j) {
        asignacion[i] = j;
        produccionUsada[j] += consumoReal[i][j];
        clientesCentral[j]++;
    }

    /*!\brief Beneficio de la solucion: ingresos - indemnizaciones - coste de las centrales en marcha y paradas
     */
    public double getBeneficio() {
        double beneficio = 0;
        for (int i = 0; i < asignacion.length; i++) {
            if (asignacion[i] != SIN_CENTRAL) beneficio += ingreso[i];
            else beneficio -= indemnizacion[i];
        }
        for (int j = 0; j < capacidad.length; j++) {
            if (clientesCentral[j] > 0) beneficio -= costeMarcha[j];
            else beneficio -= costeParada[j];
        }
        return beneficio;
    }

    public int getNumClientes() {
        return asignacion.length;
    }

    public int getNumCentrales() {
        return capacidad.length;
    }

    /*!\brief Numero de clientes servidos, garantizados o no garantizados segun el parametro
     */
    public int getClientesServidos(boolean garantizados) {
        int servidos = 0;
        for (int i = 0; i < asignacion.length; i++) {
            if (asignacion[i] != SIN_CENTRAL && garantizado[i] == garantizados) servidos++;
        }
        return servidos;
    }

    /*!\brief MW de demanda garantizada que no se sirve
     */
    public double getMWGarantizadosNoServidos() {
        double mw = 0;
        for (int i = 0; i < asignacion.length; i++) {
            if (asignacion[i] == SIN_CENTRAL && garantizado[i]) mw += consumo[i];
        }
        return mw;
    }

    /*!\brief MW que se pierden en el transporte
     */
    public double getMWPerdidos() {
        double mw = 0;
        for (int i = 0; i < asignacion.length; i++) {
            if (asignacion[i] != SIN_CENTRAL) mw += consumoReal[i][asignacion[i]] - consumo[i];
        }
        return mw;
    }

    /*!\brief Numero de centrales en marcha de un tipo (Central.CENTRALA, CENTRALB o CENTRALC)
     */
    public int getCentralesEnMarcha(int tipo) {
        int enMarcha = 0;
        for (int j = 0; j < clientesCentral.length; j++) {
            if (tipoCentral[j] == tipo && clientesCentral[j] > 0) enMarcha++;
        }
        return enMarcha;
    }

    /*!\brief MW que producen para sus clientes las centrales de un tipo (perdidas incluidas)
     */
    public double getMWAsignados(int tipo) {
        double mw = 0;
        for (int j = 0; j < produccionUsada.length; j++) {
            if (tipoCentral[j] == tipo) mw += produccionUsada[j];
        }
        return mw;
    }

    /*!\brief Resumen de la solucion
     */
    public String toString() {
        return "Beneficio=" + Math.round(getBeneficio())
                + " GarantizadosServidos=" + getClientesServidos(true)
                + " NoGarantizadosServidos=" + getClientesServidos(false)
                + " CentralesEnMarcha(A,B,C)=" + getCentralesEnMarcha(Central.CENTRALA) + ","
                + getCentralesEnMarcha(Central.CENTRALB) + "," + getCentralesEnMarcha(Central.CENTRALC);
    }
}
