# Catalogue des règles de gestion

Chaque règle a une **origine** :

- **Legacy** : extraite du code ConserTime ; le fichier source est cité.
- **Écart** : comportement du legacy corrigé volontairement. L'ancien comportement est décrit.
- **Complétée** : absente du legacy, ajoutée lors de l'extraction. **À valider par le métier.**

Les codes `XXX_YYY` sont ceux de `CodeRegle`, renvoyés dans `RegleMetierViolee`.

## 1. Calendrier

| # | Règle | Origine | Code / classe | Tests |
|---|---|---|---|---|
| CAL-1 | 11 jours fériés en métropole : 1er janvier, lundi de Pâques, 1er mai, 8 mai, Ascension, lundi de Pentecôte, 14 juillet, 15 août, 1er novembre, 11 novembre, 25 décembre | Legacy `DateUtil.isJourFerie` (l. 66-121) | `CalendrierFrance` | `CalendrierFranceTest`, `calendrier.feature` |
| CAL-2 | Pâques est calculée en `java.time` (algorithme de Meeus/Jones/Butcher). Le legacy utilisait la classe tierce `BigDate`, de 2 645 lignes et sous licence non OSI | Écart technique | `CalendrierFrance.dimancheDePaques` | 10 dates de référence de 2000 à 2285 |
| CAL-3 | Ascension = Pâques + 39 jours ; lundi de Pentecôte = Pâques + 50 jours | Legacy (lundi de Pâques + 38 et + 49) | `CalendrierFrance` | contrôle 2000-2040 |
| CAL-4 | Alsace-Moselle : Vendredi saint et 26 décembre en plus | Complétée | `CalendrierFrance.Zone` | `CalendrierFranceTest` |
| CAL-5 | Lundi de Pentecôte travaillé, en option (journée de solidarité) | Complétée | `new CalendrierFrance(zone, false)` | `CalendrierFranceTest` |
| CAL-6 | Jour ouvré = lundi à vendredi, hors fériés | Legacy `createDateSequenceJoursOuvres` | `CalendrierOuvre.estOuvre` | `CalendrierOuvreTest` |
| CAL-7 | Jour ouvrable = lundi à samedi, hors fériés (décompte légal des CP) | Complétée | `CalendrierOuvre.estOuvrable` | `CalendrierOuvreTest` |
| CAL-8 | Les périodes incluent leurs deux bornes. Le legacy excluait parfois la fin (`createDateSequence`) | Écart | `Periode`, `CAL_PERIODE_INVALIDE` | `CalendrierOuvreTest` |
| CAL-9 | Numéro de semaine ISO 8601, la semaine commençant le lundi | Legacy (premier jour de semaine paramétré) | `CalendrierOuvre.numeroSemaine` | `CalendrierOuvreTest` |

## 2. Saisie des temps

| # | Règle | Origine | Code | Tests |
|---|---|---|---|---|
| TPS-1 | Le temps est saisi en jours, par demi-journée (0,5 ou 1 par projet et par jour) | Legacy : la colonne `HOURS` contient en fait des jours ; la validation d'un congé écrit 1.0 ou 0.5 | `SAISIE_QUANTITE_INVALIDE` | `FeuilleDeTempsTest`, `saisie_et_documents.feature` |
| TPS-2 | Un jour ne peut pas dépasser 1 jour saisi, tous projets confondus. Le legacy laissait un congé validé s'ajouter à une journée déjà saisie | Écart | `SAISIE_JOUR_DEPASSE` | idem |
| TPS-3 | Une nouvelle saisie du même projet le même jour remplace la précédente | Legacy `TimesheetPersisterImpl.persistEntries` | `FeuilleDeTemps.saisir` | `FeuilleDeTempsTest` |
| TPS-4 | On ne saisit que sur ses propres affectations, ouvertes à la date saisie | Legacy (statuts BEFORE_START / AFTER_DEADLINE) | `SAISIE_HORS_AFFECTATION` | `FeuilleDeTempsTest` |
| TPS-5 | Pas de saisie avant l'entrée ni après la sortie du collaborateur | Complétée | `SAISIE_HORS_PRESENCE` | `FeuilleDeTempsTest` |
| TPS-6 | Les saisies le week-end ou un jour férié sont acceptées mais signalées | Legacy (acceptées) + signalement complété | `saisiesHorsJoursOuvres()` | `FeuilleDeTempsTest` |
| TPS-7 | Intégrer un lot de saisies (un congé validé, par exemple) se fait en tout ou rien | Complétée | `FeuilleDeTemps.integrer` | `FeuilleDeTempsTest` |

## 3. Budgets d'affectation

| # | Règle | Origine | Classe | Tests |
|---|---|---|---|---|
| BUD-1 | Budget fixe : en dessous, dans le budget ; égal, budget atteint (accepté) ; au-delà, refusé | Legacy `ProjectAssignmentStatusServiceImpl.addFixedAssignmentStatus` | `Affectation.evaluer` | `AffectationTest` |
| BUD-2 | Budget flexible : en dessous, dans le budget ; jusqu'à budget + dépassement, dépassement en cours ; égal au maximum, accepté ; au-delà, refusé | Legacy `addFlexAssignmentStatus` | idem | idem |
| BUD-3 | Le chef de projet est prévenu quand une saisie change le statut du budget, si l'affectation le prévoit | Legacy `TimesheetPersisterImpl.validateAndPersist` | `doitPrevenirChefDeProjet` | idem |

## 4. Congés

| # | Règle | Origine | Code | Tests |
|---|---|---|---|---|
| CNG-1 | Une demande porte sur un projet d'absence (CP, RTT, absence exceptionnelle) du client CONSERTO | Legacy `CongeSelectionPanel` (client « CONSERTO » / « CONS » en dur) | `CONGE_PROJET_NON_ABSENCE` | `DemandeCongeTest` |
| CNG-2 | Les bornes sont à la demi-journée : début le matin ou l'après-midi, fin à midi ou le soir. Le legacy stockait des heures (8 h, 14 h, 12 h, 18 h) | Legacy `ajusteDateDebutEtDateFin` | `PeriodeConge` | `PeriodeCongeTest` |
| CNG-3 | Durée = nombre de jours ouvrés de la période ; un début l'après-midi ou une fin à midi retire 0,5 au jour concerné | Legacy `updateDateDemande` | `PeriodeConge.nombreDeJours` | `PeriodeCongeTest`, `conges.feature` |
| CNG-3b | **Le dernier jour compte.** Le legacy parcourait la période fin exclue : du lundi au vendredi faisait 4 jours, et lundi après-midi → mardi midi faisait 0 jour, donc la demande était refusée | **Écart** | idem | idem |
| CNG-4 | Une période sans jour ouvré est refusée | Legacy (week-end ou férié aux deux bornes → « conges.pb.dates ») | `CONGE_SANS_JOUR_OUVRE` | idem |
| CNG-5 | Le même jour, on ne peut pas commencer l'après-midi et finir le matin | Complétée (implicite dans le legacy) | `CONGE_DATES_INVALIDES` | idem |
| CNG-6 | Pas de chevauchement avec une demande en attente ou validée du même collaborateur. Matin et après-midi d'un même jour restent compatibles | Legacy `DemandeConge.queries.hbm.xml` (état différent de REFUSE) | `CONGE_CHEVAUCHEMENT` | idem |
| CNG-7 | La demande doit tenir dans la période de présence du collaborateur | Complétée | `CONGE_HORS_PRESENCE` | `DemandeCongeTest` |
| CNG-8 | Seuls un référent **du département du demandeur** ou un administrateur décident. Le legacy laissait un référent voir et valider tous les départements (constat SEC-015) | **Écart** | `CONGE_DECIDEUR_NON_HABILITE`, `estVisiblePar` | idem |
| CNG-9 | On ne décide pas de sa propre demande | Complétée | `CONGE_AUTO_DECISION` | idem |
| CNG-10 | La validation produit une saisie d'absence par jour ouvré : 0,5 le premier jour si le congé commence l'après-midi, 0,5 le dernier s'il finit à midi, 1 sinon | Legacy `CongesAdminPage` (colonne « ok ») | `DemandeConge.valider` | idem |
| CNG-10b | **Le dernier jour est imputé.** Le legacy (`CongesAdminPage:203-247` et `createDateSequenceJoursOuvres`) bouclait tant que la date restait avant l'heure de fin. Sur un congé du lundi 14 h au mercredi 12 h, il écrivait lundi 0,5 et mardi 0,5, rien le mercredi, au lieu de 0,5 / 1 / 0,5. Par ailleurs, le `NB_JOURS` stocké (fin exclue, cf. CNG-3b) ne correspondait pas aux saisies écrites (fin incluse) | **Écart** | idem | `DemandeCongeTest.validation` |
| CNG-11 | La validation est atomique : `valider(..., feuille::integrer)` remet d'abord les saisies ; si l'imputation échoue (journée déjà pleine, par exemple), la demande reste en attente. Le legacy passait la demande à VALIDE puis avalait l'erreur d'imputation | **Écart** | `DemandeConge.valider(…, Consumer)` | `DemandeCongeTest.validationAtomique` |
| CNG-12 | Transitions : EN_ATTENTE → VALIDEE, REFUSEE ou ANNULEE ; VALIDEE → ANNULEE. Aucune autre | Legacy (3 états) + annulation complétée | `CONGE_TRANSITION_INTERDITE` | idem |
| CNG-13 | Annulation par le demandeur ou un administrateur ; un congé validé seulement avant son premier jour, selon l'horloge | Complétée | `CONGE_ANNULATION_NON_AUTORISEE`, `CONGE_ANNULATION_TARDIVE` | `DemandeCongeTest`, `conges.feature` |
| CNG-13b | **Capacité retirée au référent.** Dans le legacy (`CongesAdminPage:309-320`, colonne « nok »), le référent pouvait supprimer n'importe quelle demande, y compris un congé validé déjà commencé ou passé, saisies comprises. Cette révocation n'est pas reprise : seuls le demandeur ou un administrateur annulent, et seulement avant le premier jour | **Écart à arbitrer** | — | — |
| CNG-14 | La date de dépôt et la date de décision viennent de l'horloge (`Clock`) | Legacy (`DATE_DEMANDE`) | `DemandeConge` | idem |
| NOT-1 | La nouvelle demande est notifiée aux référents du département ; si le département n'en a aucun, à tous les référents, avec un signalement de configuration incomplète | Legacy `sendMailForHolidays` | `Notifications.nouvelleDemande` | `NotificationsTest` |
| NOT-1b | Le demandeur est retiré des destinataires. S'il est le seul référent de son département, la demande part aux autres référents, sans signalement. Le legacy lui envoyait sa propre demande | Complétée (cohérent avec CNG-9) | idem | `NotificationsTest` |
| NOT-2 | La réponse est envoyée au demandeur | Legacy `mailDemandeCongeResponse` | `Notifications.reponse` | idem |
| NOT-3 | Une demande en attente depuis N jours ouvrés est relancée | Complétée | `Notifications.aRelancer` | idem, `conges.feature` |

## 5. Droits à congés payés (complétés, à valider)

| # | Règle | Référence | Classe | Tests |
|---|---|---|---|---|
| CP-1 | Période de référence du 1er juin au 31 mai ; les droits se prennent l'année suivante | C. trav. R3141-4 | `PeriodeReference` | `CalculDroitsCongesPayesTest` |
| CP-2 | 2,5 jours ouvrables par mois (30 par an), ou l'équivalent de 25 jours ouvrés | L3141-3 | `ModeDecompte` | idem |
| CP-3 | Un mois de présence partielle compte en entier s'il atteint 24 jours ouvrables (20 ouvrés), sinon au prorata | L3141-4 | `fractionDuMois` | idem, `droits_conges_payes.feature` |
| CP-4 | Seuls les mois terminés sont acquis, plus le mois du départ. Le calcul se fait « à la date du jour » via `Clock` | Complétée | `enCours(clock)` | idem |
| CP-5 | Arrondi à l'entier supérieur une fois la période close | L3141-7 | `CalculDroitsCongesPayes` | idem |
| CP-6 | Congés d'ancienneté Syntec : +1 jour à 5 ans, +2 à 10 ans, +3 à 15 ans, +4 à 20 ans, appréciés au 1er juin | Convention Syntec, art. 23 (**à confirmer**) | `PolitiqueCongesPayes.syntec` | idem |
| CP-7 | Les stagiaires n'acquièrent pas de CP | Complétée | `TypeContrat.acquiertCongesPayes` | idem |
| CP-8 | Solde = droits de la période close − jours de CP validés dont le premier jour tombe dans la période de prise | Complétée | `solde(...)` | idem |
| CP-9 | En jours ouvrables, les jours ouvrables qui précèdent la reprise sont décomptés : un congé du lundi au vendredi, avec reprise le lundi, coûte 6 jours (le samedi compte) | Jurisprudence constante (Cass. soc.) | `joursOuvrablesDecomptes` | `CalculDroitsCongesPayesTest.solde` |

Non couverts :
- absences non assimilées à du travail effectif ;
- arrêts maladie (loi du 22 avril 2024) ;
- fractionnement ;
- report des droits.

## 6. Documents

| # | Règle | Origine | Classe | Tests |
|---|---|---|---|---|
| DOC-1 | Export CRA / FDT : chaque jour ouvré de la présence doit totaliser 1 jour. Le legacy comparait un **nombre de lignes** au nombre de jours ouvrés, et perdait la date d'entrée quand une date de sortie existait | Legacy `checkNbJoursOuvresSaisies` + **écart** | `ControleCompletude`, `EXPORT_SAISIE_INCOMPLETE` | `FeuilleDeTempsTest`, `saisie_et_documents.feature` |
| DOC-2 | CRA : une ligne par jour, jours non travaillés grisés, seuls les projets facturables, format « (1.0) - Projet » séparé par « / », total « Nombre de jours facturés », signatures consultant et responsable client | Legacy `ExportCra*` | `CompteRenduActivite` | `DocumentsTest` |
| DOC-3 | Client du CRA = client du premier projet **facturable** du mois. Le legacy prenait le premier projet saisi, absences comprises | **Écart** | idem | idem |
| DOC-4 | FDT : une colonne par jour, une ligne par projet (absences comprises), totaux par projet et total du mois, matricule, signature du responsable Conserto | Legacy `ExportFdt*` | `FicheDeTemps` | idem |
| DOC-5 | Les totaux FDT gardent les demi-journées. Le legacy tronquait avec `intValue()` | **Écart** | idem | idem |
| DOC-6 | NDF : en-tête (matricule, nom, date de soumission, mission, approbateur, service, période) et colonnes date, description, km, indemnité, TTC, TVA, HT | Legacy `ExportNdfSheet` | `NoteDeFrais` | idem |
| DOC-7 | NDF : indemnité = km × taux ; HT = TTC − TVA ; total à verser = indemnités + TTC ; montants positifs ; TVA ≤ TTC ; lignes dans le mois. Le legacy ne produisait qu'un modèle vide | Complétée | `NDF_LIGNE_INVALIDE` | idem |

## Points à valider avec le métier

1. Unité de saisie en jours : à confirmer sur la base de production (`completeDayHours`).
2. Mode de décompte des CP (ouvrables ou ouvrés) et convention applicable (Syntec ?).
3. Taux kilométrique (barème fiscal ou taux Conserto).
4. Délai de relance des demandes en attente.
5. Règle d'annulation d'un congé validé, et droit de l'administrateur à annuler.
6. Faut-il bloquer les saisies le week-end et les jours fériés, ou seulement les signaler comme aujourd'hui ?
7. Faut-il redonner au référent la révocation d'un congé validé (CNG-13b) ?

## Convention technique : le fuseau de l'horloge

Les règles « à la date du jour » utilisent `LocalDate.now(clock)` : annulation tardive, relances, droits acquis à date. **L'horloge injectée doit donc être en `Europe/Paris`.** Avec une horloge UTC, on pourrait encore annuler un congé à 0 h 30 heure de Paris, le jour même de son début. En production, utiliser `Clock.system(ZoneId.of("Europe/Paris"))`.
