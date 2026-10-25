# Experiment 3: paràmetres de Simulated Annealing

Aquest document explica quins valors provem de cada paràmetre de Simulated Annealing (SA) a l'experiment 3, i per què. No conté resultats: els valors definitius sortiran de les execucions. Hi ha una taula per apuntar-los a l'apartat 9.

## Resum

| Paràmetre | Valors que provem | Fase | Motiu principal |
|---|---|---|---|
| `k` | 1, 5, 25, 125, 625, 3.125 i 15.625 | 1 (`fase=kl`) | Els valors del professor (1–125), més la mateixa progressió (×5) fins que SA accepta gairebé qualsevol empitjorament |
| `lambda` | 1, 0,01 i 0,0001 | 1 (`fase=kl`) | Els valors del professor: refredament molt ràpid, mitjà i lent |
| `steps` | Fase 1: 500.000 en totes les configuracions. Fase 2: 1, 2, 5, 10 i 20 vegades t0 | 1 i 2 (`fase=steps`) | Ha de ser "bastant més gran" que t0, la iteració a partir de la qual SA ja no accepta estats pitjors |
| `stiter` | Fases 1 i 2: 100. Fase 3: 1, 10, 100, 1.000, 10.000 i `steps` | 3 (`fase=stiter`) | El professor permet deixar-lo fix i provar-ne alguna variació al final |

## 1. Què ens demanen

- **Enunciat, experiment 3 (apartat 4.6):** trobar els paràmetres de SA que donen millor resultat. Cal fer servir el mateix escenari i la mateixa heurística, i els operadors i la solució inicial triats als experiments 1 i 2.
- **Enunciat, apartat 7.6, i el professor:** cal ajustar els quatre paràmetres.
  - `k` i `λ`: començar per valors extrems (per exemple, k = 1, 5, 25, 125 i λ = 1, 0,01, 0,0001) i anar acotant.
  - `steps`: no té un valor fix, perquè depèn de `k` i `λ`. Cal calcular en quina iteració la probabilitat d'acceptar un estat pitjor arriba a 0 i fer-ne servir un nombre bastant més gran.
  - `stiter`: segons el problema, té més o menys efecte. Es pot deixar fix, provar-ne alguna variació i explicar-ho a l'informe. Ha de dividir `steps`.
- **Rúbrica:** la nota màxima en aquest punt demana ajustar els paràmetres "explorando sistemáticamente los valores de sus parámetros".

## 2. Com funciona el SA d'AIMA

Ho hem comprovat al codi d'AIMA (classes `SimulatedAnnealingSearch` i `Scheduler`):

- **Temperatura:** T = k·e^(−λ·t), on t és la iteració arrodonida cap avall a un múltiple de `stiter`. La temperatura baixa, doncs, a esglaons: es manté igual durant `stiter` iteracions seguides.
- **Acceptació:** a cada iteració es tria un successor a l'atzar.
  - Si millora el benefici, s'accepta sempre.
  - Si no el canvia (ΔE = 0), s'accepta amb probabilitat 0,5.
  - Si l'empitjora en |ΔE| euros, s'accepta amb probabilitat P = 1 / (1 + e^(|ΔE|/T)). L'enunciat escriu e^(ΔE/T), però quan la probabilitat és petita les dues fórmules donen pràcticament el mateix. Amb la d'AIMA, P mai passa de 0,5.
- **Quan s'atura:** després de `steps` iteracions, o abans si la temperatura arriba a valer exactament 0 (apartat 2.1).
- **Què retorna:** el millor estat que ha vist, que mai és pitjor que l'inicial.

### 2.1 Per què SA pot fer menys iteracions de les que li demanem

Al bucle principal del SA d'AIMA hi ha això:

```java
double temp = scheduler.getTemp(step);
if (temp == 0.0) {
    break;      // s'atura encara que no hagi fet totes les steps
}
```

Matemàticament, T = k·e^(−λ·t) mai val 0: només es fa cada vegada més petita. Però l'ordinador guarda els nombres amb una precisió limitada, i el nombre positiu més petit que Java pot representar és d'aproximadament 10^(−324):

- e^(−745) ≈ 5·10^(−324) encara hi cap, just al límit.
- e^(−746) és més petit que aquest límit, i Java l'arrodoneix a exactament 0.

Passa com amb una calculadora que mostra 0 quan el resultat té massa decimals. Per tant, quan λ·t passa d'uns 745, la temperatura val 0 i AIMA surt del bucle. Com que la temperatura només canvia cada `stiter` iteracions, l'aturada cau en un múltiple de `stiter`:

| λ | λ·t arriba a 746 a la iteració… | Amb `stiter` = 100 i `steps` = 500.000, SA fa realment… |
|---|---|---|
| 1 | 746 | 800 iteracions |
| 0,01 | 74.600 | 74.600 iteracions |
| 0,0001 | ~7.460.000 | les 500.000 |

Conseqüències:

- **Temps:** al primer exp3 es demanaven 100.000 iteracions, però amb λ = 0,01 SA en feia 74.600. Per això aquelles execucions trigaven menys: 181–268 ms, davant de 288–382 ms amb λ = 0,0001.
- **Comparació entre valors de λ:** no tots fan el mateix nombre d'iteracions. Amb λ = 1, SA només en fa 800, no 500.000. Si dona pitjors resultats, part del motiu és aquest, i també explica per què és tan ràpid. Cal dir-ho a l'informe.
- **Quines iteracions es perden:** les que falten serien totes de la fase final, quan SA ja només accepta millores, perquè t0 és com a molt 1.200 amb λ = 0,01 i 100 amb λ = 1 (apartat 4). El que es perd és, doncs, part d'aquesta fase final.

El programa calcula quantes iteracions farà realment cada configuració i ho escriu a la línia `#` (`iteraciones_reales`), perquè no s'hagi de calcular a mà.

## 3. Quant valen els ΔE en aquest problema

La probabilitat d'acceptar un estat pitjor depèn de |ΔE|/T. Per tant, `k`, que és la temperatura inicial, s'ha de triar segons els ΔE del problema. Aquí ΔE és la variació del benefici, en euros:

| Moviment | ΔE |
|---|---|
| Moure un client entre dues centrals que ja estan en marxa | 0 € |
| Deixar sense servir un client no garantit (es perd la tarifa i es paga una indemnització de 50 €/MW) | G: de −550 a −1.100 €. MG: de −900 a −2.250 €. XG: de −1.750 a −7.000 € |
| Engegar una central aturada (cost en marxa − cost aturada) | C: de −5.000 a −18.500 €. B: de −13.000 a −25.000 €. A: de −17.500 a −42.500 € |
| Moure l'únic client d'una central a una central aturada semblant (una s'apaga i l'altra s'engega) | La diferència de cost entre les dues centrals, que pot ser de pocs euros |

Els empitjoraments habituals són de milers d'euros. A l'exemple de l'enunciat (el TSP), en canvi, el cost total és d'uns centenars d'unitats (figures 7.7 a 7.12), i les figures 7.5 i 7.6 estudien ΔE = 1. Per això aquí els valors de `k` de l'exemple es queden curts (apartat 6.1).

## 4. t0: la iteració en què la probabilitat d'acceptar un estat pitjor arriba a 0

Per calcular t0 cal decidir dues coses: quin empitjorament fem servir i què vol dir que la probabilitat "arriba a 0".

### 4.1 Per què cal triar un empitjorament

La probabilitat d'acceptar un estat pitjor, P = 1 / (1 + e^(|ΔE|/T)), depèn de quant pitjor és:

- Un estat molt pitjor (|ΔE| gran) es deixa d'acceptar aviat, quan la temperatura encara és alta.
- Un estat només una mica pitjor (|ΔE| petit) s'accepta fins que la temperatura és molt baixa.

Exemple amb k = 125 i λ = 0,0001:

| Iteració | T | P d'acceptar perdre 550 € | P d'acceptar perdre 1 € |
|---|---|---|---|
| 0 | 125 | 0,012 | 0,50 |
| 10.000 | 46 | 0,000006 | 0,49 |
| 50.000 | 0,84 | ≈ 0 | 0,23 |
| 67.700 | 0,14 | ≈ 0 | 0,0009 |

Per tant, la pregunta "en quina iteració arriba a 0?" té respostes molt diferents:
- per a empitjoraments de 550 € (deixar sense servir el client més petit), cap a la iteració 4.600;
- per a empitjoraments d'1 €, cap a la 67.700.

### 4.2 Les dues decisions

- **Empitjorament de referència: ΔE = 1 €.**
  - Hi ha moviments que empitjoren només uns pocs euros (apartat 3). Per exemple, treure l'únic client d'una central i posar-lo en una altra central aturada de cost semblant.
  - Si féssim servir 550 €, després de la iteració 4.600 SA encara acceptaria aquests empitjoraments petits. No seria cert que, a partir de t0, només pot millorar.
  - Amb 1 €, a partir de t0 SA ja no accepta pràcticament cap estat pitjor. És el que demana l'enunciat: "hace falta que el algoritmo tenga un periodo en el que solo se pueda mejorar la solución".
  - És l'opció conservadora: és la que garanteix més segur aquesta fase final. El preu és que dona el t0 més gran, i per tant més iteracions i més temps.
  - A més, 1 € és la unitat del benefici, i és el mateix ΔE que fan servir les figures 7.5 i 7.6 de l'enunciat.
- **"Probabilitat 0": menys de 0,001**, és a dir, menys d'una vegada cada 1.000. Matemàticament, la probabilitat mai val exactament 0.

Per fer servir altres valors, només cal canviar les constants `DELTA_E_REFERENCIA` i `PROBABILIDAD_CERO` a EnergiaDemo.java.

### 4.3 El càlcul

Amb aquests valors:

- Un estat 1 € pitjor s'accepta amb probabilitat inferior a 0,001 quan la temperatura és més baixa que T0 = 1 / ln(999) ≈ 0,145.
- La temperatura baixa de T0 a la iteració t0 = ln(k / T0) / λ, arrodonida cap amunt a un múltiple de `stiter`.

| t0 (amb `stiter` = 100) | λ = 1 | λ = 0,01 | λ = 0,0001 |
|---|---|---|---|
| k = 1 | 100 | 200 | 19.400 |
| k = 5 | 100 | 400 | 35.500 |
| k = 25 | 100 | 600 | 51.600 |
| k = 125 | 100 | 700 | 67.700 |
| k = 625 | 100 | 900 | 83.800 |
| k = 3.125 | 100 | 1.000 | 99.800 |
| k = 15.625 | 100 | 1.200 | 115.900 |

El programa calcula t0 i l'escriu abans de cada configuració, en una línia com aquesta:
```
# steps=500000 stiter=100 k=125 lambda=0.0001: t0=67700 steps/t0=7.4 iteraciones_reales=500000
```
- `steps/t0`: quantes vegades t0 són les iteracions de la configuració.
- `iteraciones_reales`: les iteracions que fa realment AIMA, que poden ser menys que `steps` (apartat 2).

## 5. Ordre de les fases

1. **Fase 1: `k` i `λ`** (`fase=kl`). Van primer perquè t0, i per tant `steps`, depenen d'ells.
2. **Afinar `k` i `λ`** (opcional): es repeteix la fase 1 amb valors intermedis al voltant dels millors.
3. **Fase 2: `steps`** (`fase=steps`), amb els `k` i `λ` triats.
4. **Fase 3: `stiter`** (`fase=stiter`), amb `k`, `λ` i `steps` triats. És una prova petita al final, perquè el professor diu que es pot deixar fix.

Variem un paràmetre cada vegada i el fixem abans de passar al següent, que és com l'enunciat proposa fer els experiments ("ir fijando"). Provar totes les combinacions alhora donaria 7 × 3 × 5 × 6 = 630 configuracions.

## 6. Decisions per paràmetre

### 6.1 `k`: 1, 5, 25, 125, 625, 3.125 i 15.625

Inclou els quatre valors del professor i continua la mateixa progressió (cada valor és 5 vegades l'anterior) fins a 15.625. Cal allargar-la perquè, amb els ΔE d'aquest problema, k = 125 no és un extrem. La taula mostra la probabilitat d'acceptar diversos empitjoraments al principi de la cerca, quan T = k:

| k | Deixar un G d'1 MW (−550 €) | Deixar un XG de 10 MW (−3.500 €) | Engegar la C més petita (−5.000 €) | Engegar l'A més gran (−42.500 €) |
|---|---|---|---|---|
| 1, 5 i 25 | ≈ 0 | ≈ 0 | ≈ 0 | ≈ 0 |
| 125 | 0,012 | ≈ 0 | ≈ 0 | ≈ 0 |
| 625 | 0,29 | 0,004 | 0,0003 | ≈ 0 |
| 3.125 | 0,46 | 0,25 | 0,17 | ≈ 0 |
| 15.625 | 0,49 | 0,44 | 0,42 | 0,06 |

- Amb k ≤ 125, SA no engegaria mai una central ni deixaria sense servir un client gran: es comportaria gairebé com HC. Aquest és l'extrem fred.
- Amb k = 15.625, al principi accepta gairebé qualsevol empitjorament, a prop del màxim de 0,5. Aquest és l'extrem calent. Un `k` més gran ja no canviaria gaire res i només allargaria t0.

### 6.2 `λ`: 1, 0,01 i 0,0001

Són els valors del professor, separats per un factor 100: refredament molt ràpid, mitjà i lent. En aquesta implementació, amb `stiter` = 100, signifiquen el següent:

- **λ = 1:** T = k només durant les 100 primeres iteracions. Després la temperatura és pràcticament 0, i AIMA s'atura a la iteració 800. Equival a 100 iteracions en què accepta empitjoraments i 700 en què només accepta millores i moviments neutres.
- **λ = 0,01:** t0 està entre 200 i 1.200. AIMA s'atura a la iteració 74.600, encara que `steps` sigui més gran.
- **λ = 0,0001:** t0 està entre 19.400 i 115.900. És l'extrem lent.

No hi afegim un λ més petit, com 0,00001, perquè t0 seria 10 vegades més gran (fins a 1,16 milions d'iteracions) i cada execució trigaria desenes de segons. Si λ = 0,0001 resulta ser el millor, es pot provar per sota en el pas d'afinar (6.4).

### 6.3 `steps` a la fase 1: 500.000 en totes les configuracions

- Fem com a l'exemple 7.6.1 de l'enunciat: "usaremos un número de iteraciones grande para asegurarnos de que lleguemos a converger".
- 500.000 és més de 4 vegades el t0 més gran de la graella (115.900). Així, totes les configuracions tenen un període llarg en què només poden millorar, com demana el professor.
- Totes tenen el mateix nombre d'iteracions, de manera que les diferències vénen de `k` i `λ`. L'excepció són λ = 1 i λ = 0,01: fan menys iteracions (800 i 74.600) perquè AIMA s'atura quan la temperatura arriba a 0 (apartat 2.1). La columna `iteraciones_reales` ho mostra, i cal explicar-ho a l'informe.
- Temps estimat a partir del vostre primer exp3 (uns 0,35 s cada 100.000 iteracions): uns 15 s per llavor. Són uns 2,5 minuts amb `reps=10` i uns 7 o 8 minuts amb `reps=30`.

### 6.4 Afinar `k` i `λ` (opcional)

Si a la fase 1 hi ha una configuració que destaca, proveu valors intermedis al voltant seu.

- Com que els valors creixen multiplicant, els punts intermedis també es calculen multiplicant. Per exemple: entre 25 i 125, uns 55 (√(25·125)); entre 125 i 625, uns 280; entre 0,01 i 0,0001, 0,001.
- Si baixeu de λ = 0,0001, pugeu `steps` perquè `steps/t0` continuï sent com a mínim 4.
- Si cap configuració és significativament millor que les altres, no cal afinar. Aquesta ja és una conclusió de l'experiment. Per saber-ho, compareu llavor a llavor (apartat 6.5). La desviació de la línia `#` no serveix per a això: mesura com canvia el benefici d'un escenari a un altre, i aquesta variació afecta per igual totes les configuracions.

### 6.5 `steps` a la fase 2: 1, 2, 5, 10 i 20 vegades t0

El professor diu que cal fer servir un nombre "bastant més gran" que t0. Aquesta fase mesura quant més gran:

- **×1:** la cerca acaba just quan la probabilitat arriba a 0. Segons l'enunciat no n'hi ha prou, perquè "hace falta que el algoritmo tenga un periodo en el que solo se pueda mejorar la solución". És l'extrem inferior, i esperem que doni pitjors resultats.
- **×2:** el període en què només pot millorar és tan llarg com el de refredament.
- **×5, ×10 i ×20:** períodes cada vegada més llargs.

**Criteri per triar:** el factor més petit a partir del qual el benefici ja no millora de manera significativa, tenint en compte que el temps creix proporcionalment a `steps`.

- Totes les configuracions fan servir les mateixes llavors, així que es poden comparar per parelles, llavor a llavor. Hi ha dues maneres, i totes dues surten a l'exemple 7.5.1 de l'enunciat:
  - la t de Student aparellada, que mira si la diferència mitjana és significativa;
  - la prova binomial, que compta en quantes llavors guanya cadascuna, sense comptar els empats.
- Si a ×20 encara millora, afegiu-hi factors més grans (`factores=20,50,100`), sense passar de les 745/λ iteracions que permet AIMA.
- `stiter` continua fix a 100.
- factor × t0 no sol ser un nombre rodó (per exemple, 5 × 67.700 = 338.500), i a la fase 3 `stiter` ha de dividir `steps`. Abans de la fase 3, arrodoniu cap amunt el valor triat a un nombre rodó proper, com 350.000, perquè es puguin provar tots els valors de `stiter`.

### 6.6 `stiter`: 100 a les fases 1 i 2, i 1, 10, 100, 1.000, 10.000 i `steps` a la fase 3

- **Per què el fixem a 100:** és el valor de les demos del professor (`ProbTSPDemo` fa `new SimulatedAnnealingSearch(2000, 100, 5, 0.001)`), i el professor permet deixar-lo fix.
- **Què canvia:** la temperatura es manté igual durant `stiter` iteracions. El que compta és el producte λ·`stiter`, que indica quant hauria baixat la temperatura dins d'un esglaó (un factor e^(−λ·stiter)):

| `stiter` | Baixada per esglaó amb λ = 0,0001 | Baixada per esglaó amb λ = 0,01 |
|---|---|---|
| 1 | 0,01 % | 1 % |
| 10 | 0,1 % | 10 % |
| 100 | 1 % | 63 % |
| 1.000 | 10 % | ~100 % |
| 10.000 | 63 % | ~100 % |

- Si λ·`stiter` és molt menor que 1, l'escala és pràcticament una corba suau i no esperem diferències. Només quan λ·`stiter` s'acosta a 1 la temperatura baixa en pocs esglaons grans.
- **`stiter` = `steps`** és l'altre extrem: la temperatura no baixa mai (T = k durant tota l'execució). Serveix per veure si el refredament aporta alguna cosa.
- El programa només prova els valors que divideixen `steps`, i per a la resta escriu un avís.
- **Per interpretar la fase 1:** amb `stiter` = 100, λ = 0,01 ja baixa a esglaons grans (un 63 % cada esglaó) i λ = 1 només té un esglaó calent. Si aquestes configuracions es comporten de manera estranya, aquesta pot ser una de les explicacions.

## 7. Com executar cada fase

Primer cal recompilar:
```
javac -cp "lib/*" -d PracticaEnergia/bin PracticaEnergia/src/IA/probEnergia/*.java
```

Fase 1 i, si cal, afinar. Els valors de la segona comanda són un exemple per al cas que la millor configuració fos k = 125 i λ = 0,0001:
```
java -cp "lib/*;PracticaEnergia/bin" IA.probEnergia.EnergiaDemo exp3 fase=kl reps=30 | Out-File -Encoding ascii exp3_kl.csv
java -cp "lib/*;PracticaEnergia/bin" IA.probEnergia.EnergiaDemo exp3 fase=kl k=55,125,280 lambda=0.001,0.0001 reps=30 | Out-File -Encoding ascii exp3_afinar.csv
```

Fase 2, amb els `k` i `λ` triats (substituïu K i L):
```
java -cp "lib/*;PracticaEnergia/bin" IA.probEnergia.EnergiaDemo exp3 fase=steps k=K lambda=L reps=30 | Out-File -Encoding ascii exp3_steps.csv
```

Fase 3, amb el `steps` triat (substituïu també S):
```
java -cp "lib/*;PracticaEnergia/bin" IA.probEnergia.EnergiaDemo exp3 fase=stiter k=K lambda=L steps=S reps=30 | Out-File -Encoding ascii exp3_stiter.csv
```

- A Linux o macOS, canvieu `;` per `:` al `-cp`, i `| Out-File -Encoding ascii` per `>`.
- Pel que fa a `reps`, l'enunciat demana com a mínim 10 repeticions. Amb 30, les mitjanes i les proves estadístiques són més fiables.
- Quan acabeu, poseu els valors triats com a valors per defecte a [EnergiaDemo.java:74-77](src/IA/probEnergia/EnergiaDemo.java#L74-L77). Així els faran servir `demo`, `exp5` i `exp6`.

## 8. Què esperem (hipòtesis per a l'informe)

- **Diferències petites:** partint de la solució greedy, les diferències entre configuracions seran petites.
  - Ja es va veure al primer exp3: les diferències entre configuracions eren de centenars d'euros, sobre un benefici d'1,35 milions.
  - La greedy deixa poc marge. Amb la llavor 1, per exemple, ja serveix els 1.000 clients, i moure'ls entre centrals en marxa no canvia el benefici. HC només va fer algun pas en 1 de les 30 llavors.
- **`k` petit (1–125):** SA gairebé no accepta empitjoraments, però sí moviments neutres (amb probabilitat 0,5).
  - Es comporta com un HC que pot avançar per les zones on el benefici no canvia. Per això pot quedar una mica per sobre de HC, que s'hi atura.
  - Al primer exp3, k = 1 i k = 10 van quedar entre 60 i 250 € per sobre de HC, una diferència molt inferior a la desviació.
- **`k` gran (3.125–15.625):** SA s'allunya molt de la greedy, perquè engega centrals i deixa clients sense servir, i necessita un període llarg de millores per tornar-hi. Al primer exp3, amb k = 10.000, SA no va millorar la solució inicial en cap de les 90 execucions.
- **λ = 1:** a l'exemple de l'enunciat (figura 7.11), és el pitjor valor per a tots els `k`. Aquí, partint de la greedy, potser no es notarà, perquè cap configuració millora gaire.
- **`stiter`:** no tindrà efecte mentre λ·`stiter` sigui molt menor que 1.
- **Experiment addicional:** per veure millor l'efecte dels paràmetres, es pot repetir l'experiment amb `init=ordenada`, perquè des de l'ordenada hi ha molt més marge de millora.

## 9. Valors escollits

Resultats a `exp3_kl.csv` (fase 1), `exp3_steps.csv` (fase 2) i `exp3_stiter.csv` (fase 3), amb 30 llavors. "Guanya / empata / perd" compta, llavor a llavor, en quantes llavors una configuració treu més, igual o menys benefici que l'altra.

| Paràmetre | Valor | Fase on es tria | Motiu |
|---|---|---|---|
| `k` | 25 | 1 | És el millor benefici mitjà de la graella (1.356.027 €, amb λ = 0,0001). No és significativament millor que k = 5 (guanya / empata / perd: 4 / 20 / 6) ni que k = 1 (6 / 20 / 4). Amb k ≥ 3.125, SA no millora mai la solució inicial. |
| `lambda` | 0,0001 | 1 | Les tres millors configuracions tenen λ = 0,0001. Amb λ = 1, SA només fa 800 iteracions i no millora mai la solució inicial, sigui quin sigui el valor de `k`. |
| `steps` | 260.000 | 2 | Correspon a ×5 t0 (258.000), arrodonit perquè el divideixin tots els valors de `stiter`. És el primer factor que millora HC de manera significativa (11 / 18 / 1, prova binomial p = 0,006). ×10 i ×20 no milloren ×5 (×20 contra ×5: 4 / 21 / 5) i triguen 2 i 5 vegades més. |
| `stiter` | 100 | 3 | Cap valor (de 1 a 10.000, ni tan sols sense refredament) és significativament diferent de 100. Les diferències (~300 €) són de la mateixa mida que les que surten repetint la mateixa configuració (274 €). Es manté el valor de les fases 1 i 2, que és el de les demos del professor. |
