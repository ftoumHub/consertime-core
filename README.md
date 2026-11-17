# consertime-core

Le cœur métier de ConserTime, extrait du legacy (fork d'eHour) et rendu indépendant. C'est une bibliothèque Java 17 **sans aucune dépendance à l'exécution** : ni Spring, ni base de données, ni framework web. Elle doit pouvoir être reprise telle quelle par le remaster (Spring Boot 4 + jOOQ).

Le catalogue des règles (origine, écarts avec le legacy, points à valider) se trouve dans [REGLES.md](REGLES.md).

## Modules

```
consertime-core/
├── consertime-domain   le domaine pur (java.time, BigDecimal)
│   └── fr.conserto.consertime.domaine
│       ├── calendrier   jours fériés FR, Alsace-Moselle, jours ouvrés et ouvrables, périodes
│       ├── personnel    Collaborateur, TypeContrat, Role, Departement
│       ├── affaires     Client, Projet, Affectation, Budget (fixe ou flexible)
│       ├── temps        Jours, Saisie, FeuilleDeTemps, ControleCompletude
│       ├── conges       PeriodeConge, DemandeConge (cycle de vie), Notifications
│       ├── droits       acquisition et solde des CP (Code du travail, option Syntec)
│       ├── documents    CompteRenduActivite (CRA), FicheDeTemps (FDT), NoteDeFrais (NDF)
│       └── regle        CodeRegle + RegleMetierViolee
└── consertime-excel    l'adaptateur Apache POI 5 (XSSF) : ExportExcel.cra / fdt / ndf
```

Les choix de conception :

- L'immuabilité est la règle (`record`). Seuls `FeuilleDeTemps`, `DemandeConge` et `NoteDeFrais` ont un état, parce que leur cycle de vie est métier.
- Tout calcul qui dépend du « maintenant » reçoit une `java.time.Clock` : dépôt, décision et annulation d'un congé, droits acquis à date, relances, date d'édition des documents. Les tests figent le temps avec `Clock.fixed`.
- Toute violation lève une `RegleMetierViolee` qui porte un `CodeRegle`, stable et testable, plutôt qu'un message libre.
- Les durées sont exprimées en `Jours`, un `BigDecimal` en forme canonique (pas de `double`). Le legacy enregistrait déjà des jours dans la colonne `HOURS`.

## Exemple

```java
Clock horloge = Clock.system(ZoneId.of("Europe/Paris")); // fuseau imposé, cf. REGLES.md
CalendrierOuvre cal = CalendrierOuvre.france();

Collaborateur alice = Collaborateur.builder()
        .matricule(1002).nom("Alice", "Martin").email("alice@conserto.pro")
        .entreLe(LocalDate.of(2020, 1, 6)).contrat(TypeContrat.ETAM)
        .departement("DIG").build();

// Saisie du mois et documents
FeuilleDeTemps feuille = new FeuilleDeTemps(alice, YearMonth.of(2025, 4), cal);
Affectation mission = Affectation.simple(alice.matricule(), Projet.mission("ACME-EXT", "Refonte extranet", acme));
for (LocalDate jour : cal.joursOuvres(Periode.mois(YearMonth.of(2025, 4)))) {
    feuille.saisir(mission, jour, Jours.UN);
}
CompteRenduActivite cra = CompteRenduActivite.etablir(feuille, true, horloge);
byte[] xlsx = ExportExcel.cra(cra);

// Congé : dépôt puis validation par la référente du département
DemandeConge demande = DemandeConge.deposer(alice, TypeAbsence.CONGES_PAYES,
        new PeriodeConge(lundi, DemiJournee.MATIN, vendredi, DemiJournee.APRES_MIDI),
        "Vacances", demandesExistantes, cal, horloge);
// atomique : si l'imputation échoue, la demande reste en attente
demande.valider(rachel, digital, cal, horloge, feuille::integrer);

// Droits à CP
var calcul = new CalculDroitsCongesPayes(PolitiqueCongesPayes.codeDuTravail(ModeDecompte.OUVRES), cal);
SoldeCongesPayes solde = calcul.solde(alice, demandes, horloge);
```

## Construire et tester

```bash
mvn verify
```

Ce que lance `mvn verify` :

- les tests JUnit 5 (AssertJ, `@Nested`, `@ParameterizedTest`) ;
- les scénarios Cucumber écrits en Gherkin français (`src/test/resources/features/*.feature`), via le moteur JUnit Platform (`RunCucumberTest`).

Le rapport HTML Cucumber est écrit dans `consertime-domain/target/cucumber-report.html`.

Les fonctionnalités Gherkin :

- `calendrier.feature` : jours fériés, Pâques, jours ouvrés du mois ;
- `conges.feature` : durée, chevauchement, validation par le référent, annulation, relance ;
- `droits_conges_payes.feature` : acquisition selon la date d'embauche, arrondi, ancienneté, solde ;
- `saisie_et_documents.feature` : règles de saisie, complétude, CRA et FDT.

## État de la vérification

> La bibliothèque **n'a pas encore été construite avec un vrai Maven** : Maven Central n'était pas accessible dans l'environnement de développement.

Les vérifications faites :

- le code de production a été compilé avec `javac --release 17 -Xlint:all -Werror` ;
- les **195 vérifications** (140 tests unitaires et 55 scénarios Gherkin) ont été exécutées et sont vertes, avec un banc de test local qui reproduit JUnit 5, AssertJ et Cucumber ;
- les valeurs attendues (jours fériés, jours ouvrés, droits CP) ont été recalculées de façon indépendante en Python ;
- le module Excel n'a été que **compilé**, contre des signatures POI simulées : ses tests n'ont pas tourné.

Le premier `mvn verify` sur un poste connecté reste donc la preuve à obtenir. Les écarts d'API éventuels seraient mineurs (signatures AssertJ ou POI).
