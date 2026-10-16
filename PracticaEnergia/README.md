# Pràctica de Cerca Local: assignació de clients a centrals elèctriques

Resolució amb Hill Climbing i Simulated Annealing (framework AIMA) del problema d'assignar
clients a centrals per maximitzar el benefici de la companyia.

## Estructura

```
lib/
  AIMA.jar                framework AIMA (Problem, SearchAgent, HillClimbingSearch, SimulatedAnnealingSearch...)
  CentralEnergia.jar      classes del domini (IA.Energia: Central, Centrales, Cliente, Clientes, VEnergia)
PracticaEnergia/
  src/IA/probEnergia/     el nostre codi
```

| Classe | Què fa |
|---|---|
| `EnergiaBoard` | Estat: escenari, solucions inicials, operadors i càlcul del benefici |
| `EnergiaSuccessorFunction` | Successors per a Hill Climbing (tots els del conjunt d'operadors) |
| `EnergiaSuccessorFunctionSA` | Successor per a Simulated Annealing (un operador i paràmetres a l'atzar) |
| `EnergiaHeuristicFunction` | Heurística: el benefici canviat de signe |
| `EnergiaHeuristicFunctionPenalizacion` | Heurística de l'experiment 5: benefici amb penalització per MW garantit no servit |
| `EnergiaGoalTest` | Sempre fals: en cerca local no hi ha estat final conegut |
| `EnergiaDemo` | Programa principal: demo, traces i experiments |

## Requisits

JDK 11 o superior (`javac` i `java` al PATH). Els dos jars estan compilats per a Java 11.

## Compilar

Des de la carpeta que conté `lib/` i `PracticaEnergia/`:

```
javac -cp "lib/*" -d PracticaEnergia/bin PracticaEnergia/src/IA/probEnergia/*.java
```

Si no surt cap missatge, ha anat bé. Cal tornar a compilar cada cop que es canvia el codi.

## Executar

Windows (PowerShell):
```
java -cp "lib/*;PracticaEnergia/bin" IA.probEnergia.EnergiaDemo [mode] [opcio=valor ...]
```
Linux / macOS (el separador del classpath és `:` en lloc de `;`):
```
java -cp "lib/*:PracticaEnergia/bin" IA.probEnergia.EnergiaDemo [mode] [opcio=valor ...]
```

| Mode | Què fa |
|---|---|
| `demo` (per defecte) | Executa HC i SA sobre l'escenari de l'enunciat i en mostra el resum |
| `traza` | Benefici de cada pas de HC i SA, en format CSV (per fer gràfiques) |
| `exp1` ... `exp6` | Experiments de l'enunciat (vegeu l'apartat *Experiments*) |
| qualsevol altre | Mostra l'ajuda |

| Opció | Per defecte | Significat |
|---|---|---|
| `seed` | `1` | Llavor de l'escenari (modes `demo` i `traza`) |
| `init` | `greedy` | Solució inicial: `ordenada`, `greedy` o `vacia` (aquesta última, només per a l'experiment 5) |
| `ops` | `A` | Operadors: `A` = moure + desassignar, `B` = A + intercanviar |
| `steps` | `100000` | Iteracions de SA |
| `stiter` | `100` | Iteracions de SA amb la mateixa temperatura (ha de dividir `steps`) |
| `k` | `1000` | Paràmetre k de la temperatura de SA (enter) |
| `lambda` | `0.0001` | Paràmetre λ de la temperatura de SA |
| `trazaSA` | `no` | `si`: mostra cada estat pitjor que accepta SA |
| `reps` | `10` | Repeticions de cada configuració als experiments (llavors 1..reps) |

L'escenari és sempre el de l'enunciat (5 centrals A, 10 de B i 25 de C; 1000 clients amb un 25% de XG, un 30% de MG i un 45% de G; un 75% garantits), excepte als experiments 4 i 6, que el modifiquen. La mateixa llavor genera sempre les mateixes dades.

## Com funcionen els algorismes (resum)

- **Estat**: per a cada client, la central que el serveix o cap. Per a cada central es guarden els MW ocupats i el nombre de clients; està en marxa si en té algun.
- **Solucions inicials**:
  - *ordenada*: cada client garantit va a la primera central, per índex, on hi càpiga. Els no garantits queden sense servir.
  - *greedy*: cada client garantit va a la central més propera on hi càpiga. Després, cada no garantit va a la central en marxa més propera on hi càpiga.
- **Operadors**:
  - *moure* un client a una altra central; serveix també per assignar-ne un que no estava servit.
  - *desassignar* un client no garantit.
  - *intercanviar* les centrals de dos clients (només al conjunt B).
  - Cap operador posa més MW en una central dels que pot produir.
  - Cap operador deixa un client garantit sense servir, excepte a l'experiment 5, on es permet i es penalitza a l'heurística.
- **Heurística**: AIMA minimitza, per tant h = −benefici, amb:
  - benefici = ingressos dels clients servits − indemnitzacions (50 €/MW per cada no garantit sense servir) − cost de les centrals en marxa (producció × cost per MW + cost fix) − cost de les centrals aturades;
  - el client paga el seu consum, però la central ha de produir consum × (1 + pèrdua), segons la distància.
- **Hill Climbing**: a cada pas genera tots els successors i va al millor, però només si és *estrictament* millor que l'estat actual. Si no n'hi ha cap, s'atura.
- **Simulated Annealing**: fa `steps` iteracions, i a cada una genera un sol successor a l'atzar.
  - Si el successor millora, l'accepta. Si és igual (ΔE = 0), l'accepta amb probabilitat 0,5.
  - Si empitjora, l'accepta amb probabilitat e^(ΔE/T) / (1 + e^(ΔE/T)), on ΔE és la diferència de benefici en euros (negativa).
  - La temperatura és T = k·e^(−λ·t) i es recalcula cada `stiter` iteracions.
  - Al final retorna la millor solució que ha vist, que mai pot ser pitjor que la inicial.

## Seguir una execució

### 1. Resum de HC i SA (`demo`)

```
java -cp "lib/*;PracticaEnergia/bin" IA.probEnergia.EnergiaDemo
```
```
Solucion inicial: Beneficio=1349270 GarantizadosServidos=763 NoGarantizadosServidos=237 CentralesEnMarcha(A,B,C)=5,10,25

Hill Climbing  -->
Solucion final: Beneficio=1349270 GarantizadosServidos=763 NoGarantizadosServidos=237 CentralesEnMarcha(A,B,C)=5,10,25
Pasos: 0  Nodos expandidos: 1  Tiempo: 36 ms

Simulated Annealing (steps=100000 stiter=100 k=1000 lambda=1.0E-4)  -->
Solucion final: Beneficio=1349270 ...
Pasos aceptados hasta la mejor solucion: 0  Tiempo: 343 ms
```
- **Beneficio**: benefici en euros.
- **GarantizadosServidos / NoGarantizadosServidos**: clients servits de cada tipus de contracte.
- **CentralesEnMarcha(A,B,C)**: centrals en marxa de cada tipus.
- **Pasos**:
  - a HC, quants operadors ha aplicat;
  - a SA, quants moviments acceptats porten de la solució inicial a la millor que ha trobat.
- **Nodos expandidos**: quants cops s'han generat successors. A HC és el nombre de passos + 1.
- **Tiempo**: només la cerca, sense comptar la generació de l'escenari.

### 2. Veure els moviments de HC

Amb `init=ordenada`, HC troba millores i en llista cadascuna (són les accions del camí de la solució):
```
java -cp "lib/*;PracticaEnergia/bin" IA.probEnergia.EnergiaDemo demo init=ordenada
```
```
Hill Climbing  -->
Mover cliente 494 a central 19
Mover cliente 64 a central 9
Mover cliente 562 a central 11
Solucion final: Beneficio=1116741 GarantizadosServidos=763 NoGarantizadosServidos=3 CentralesEnMarcha(A,B,C)=5,10,5
Pasos: 3  Nodos expandidos: 4  Tiempo: 280 ms
```

### 3. Evolució del benefici pas a pas (`traza`)

```
java -cp "lib/*;PracticaEnergia/bin" IA.probEnergia.EnergiaDemo traza init=ordenada
```
```
algoritmo;paso;beneficio
HC;0;1113958
HC;1;1115580
HC;2;1116169
HC;3;1116741
SA;0;1113958
...
```
- El pas 0 és la solució inicial.
- A HC, el benefici només pot pujar.
- A SA hi ha els estats pels quals ha passat fins a arribar a la millor solució. Pot baixar de tant en tant, perquè accepta estats pitjors. Si SA no ha millorat mai la inicial, només hi apareix el pas 0.
- Per fer-ne una gràfica, guardeu la sortida en un fitxer (vegeu *Experiments*) i obriu-lo amb un full de càlcul.

### 4. Les decisions de SA (`trazaSA=si`)

Mostra una línia per a cada estat pitjor que SA accepta. Convé fer-ho amb poques iteracions:
```
java -cp "lib/*;PracticaEnergia/bin" IA.probEnergia.EnergiaDemo demo steps=2000 stiter=100 k=1000 lambda=0.001 trazaSA=si
```
```
Pr Acep=0.8329248902434814 Delta E=-1606.5 Temp= 1000.0
Pr Acep=0.9203051255445325 Delta E=-2446.5 Temp= 1000.0
...
Pr Acep=0.976491014484287 Delta E=-616.0 Temp= 165.29888822158654
```
- **Delta E**: quants euros de benefici es perden amb el moviment.
- **Temp**: la temperatura en aquella iteració. Es veu com baixa.
- **Pr Acep**: tot i el nom, és la probabilitat de *rebutjar* el moviment. La d'acceptar-lo és `1 − Pr Acep`. A la primera línia, per exemple, perdre 1.606 € amb T = 1000 s'accepta amb probabilitat 0,17.

Com més gran és `k`, més estats pitjors s'accepten al principi. Com més gran és `lambda`, abans deixa d'acceptar-ne.

### 5. Provar altres escenaris i paràmetres

```
java -cp "lib/*;PracticaEnergia/bin" IA.probEnergia.EnergiaDemo demo seed=5
java -cp "lib/*;PracticaEnergia/bin" IA.probEnergia.EnergiaDemo demo init=ordenada k=10000 lambda=0.001
java -cp "lib/*;PracticaEnergia/bin" IA.probEnergia.EnergiaDemo demo ops=B
```

## Què hauríeu de veure

Valors de referència amb `seed=1`. HC i les solucions inicials són deterministes; SA no, perquè depèn de l'atzar.

- **Solució ordenada**: 1.113.958 €. Serveix els 763 garantits i cap no garantit, amb 20 centrals en marxa. Ignora les distàncies, i per això les pèrdues són altes.
- **Solució greedy**: 1.349.270 €. Serveix els 1000 clients i té les 40 centrals en marxa.
- **HC des de la greedy fa 0 passos.** Cap successor és estrictament millor:
  - moure un client entre dues centrals en marxa no canvia el benefici (és una meseta);
  - apagar una central obligaria a moure tots els seus clients un per un, i cap d'aquests passos millora el benefici;
  - encendre una central costa milers d'euros en un sol pas.
- **HC des de l'ordenada fa 3 passos** (1.116.741 €). Col·loca 3 clients no garantits en els forats que queden lliures i s'atura: encendre una central per a un sol client fa perdre diners.
- **SA mai retorna una solució pitjor que la inicial.** Des de l'ordenada pot millorar més que HC, perquè de tant en tant accepta encendre una central i després l'omple. Amb `seed=1` i els paràmetres per defecte, per exemple, arriba a uns 1.122.000 €. Des de la greedy, rarament millora.

## Experiments

Cada mode `expN` escriu una fila CSV, separada per `;`, per a cada execució. Al final de cada configuració hi afegeix una línia que comença per `#`, amb la mitjana i la desviació de les repeticions.

| Mode | Què compara |
|---|---|
| `exp1` | Conjunts d'operadors A i B amb HC (per defecte des de la solució ordenada) |
| `exp2` | Solucions inicials ordenada i greedy amb HC |
| `exp3` | Paràmetres de SA: totes les combinacions de les llistes `steps`, `stiter`, `k` i `lambda` (p. ex. `k=1,100,10000`), amb HC com a referència |
| `exp4` | Temps de HC augmentant els clients (`clientes=250,500,...`) i les centrals (`centrales=40,80,...`) |
| `exp5` | Penalització per MW garantit no servit (`penal=0,100,...`), partint de la solució buida, amb HC i SA |
| `exp6` | Centrals de tipus C multiplicades per 1, 2 i 3 (`factorC=1,2,3`), amb HC i SA |

Per guardar la sortida en un fitxer:
```
java -cp "lib/*;PracticaEnergia/bin" IA.probEnergia.EnergiaDemo exp1 | Out-File -Encoding ascii exp1.csv    (PowerShell)
java -cp "lib/*:PracticaEnergia/bin" IA.probEnergia.EnergiaDemo exp1 > exp1.csv                             (Linux / macOS)
```
A PowerShell, `>` desa el fitxer en UTF-16, que els fulls de càlcul llegeixen malament; per això s'usa `Out-File -Encoding ascii`.

Columnes del CSV:

| Columna | Significat |
|---|---|
| `experimento`, `configuracion`, `semilla`, `algoritmo` | Què s'ha executat |
| `beneficio_inicial`, `beneficio` | Benefici de la solució inicial i de la final (€) |
| `tiempo_ms` | Temps de la cerca |
| `pasos`, `nodos_expandidos` | Com a l'apartat 1 de *Seguir una execució* |
| `garantizados_servidos`, `no_garantizados_servidos` | Clients servits |
| `MW_garantizados_no_servidos` | Demanda garantida sense servir (només pot ser > 0 a l'experiment 5) |
| `centrales_A`, `centrales_B`, `centrales_C` | Centrals en marxa de cada tipus |
| `MW_A`, `MW_B`, `MW_C` | MW que produeixen per als clients les centrals de cada tipus (pèrdues incloses) |
| `MW_perdidos` | MW perduts en el transport |

Abans de cada experiment es fa una cerca prèvia que no es mesura, perquè la primera execució de Java sempre és més lenta i falsejaria els temps.
