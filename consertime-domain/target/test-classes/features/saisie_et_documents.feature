# language: fr
Fonctionnalité: Saisie des temps, CRA et feuille de temps
  Règles extraites de TimesheetPersisterImpl, TimesheetExportCriteriaPanel et des exports
  ExportCra* / ExportFdt*. Le CRA ne montre que le facturable ; la FDT montre tout.

  Contexte:
    Étant donné la feuille de temps d'Alice Martin pour le mois 2025-04

  Scénario: Avril complet, CRA et FDT
    Étant donné qu'Alice a saisi les périodes suivantes :
      | du         | au         | projet           | jours |
      | 2025-04-01 | 2025-04-14 | Refonte extranet | 1     |
      | 2025-04-15 | 2025-04-15 | Intercontrat     | 1     |
      | 2025-04-16 | 2025-04-21 | Refonte extranet | 1     |
      | 2025-04-22 | 2025-04-25 | Congés payés     | 1     |
      | 2025-04-28 | 2025-04-30 | Refonte extranet | 1     |
    Alors la feuille est complète avec 21 jours ouvrés attendus
    Quand on établit le compte rendu d'activité
    Alors le CRA compte 16 jours facturés pour le client "ACME Assurances"
    Et la ligne du 2025-04-21 est de type "FERIE"
    Et la ligne du 2025-04-14 indique "(1.0) - Refonte extranet"
    Quand on établit la feuille de temps mensuelle
    Alors la FDT contient les totaux suivants :
      | projet           | total |
      | Refonte extranet | 16.0  |
      | Intercontrat     | 1.0   |
      | Congés payés     | 4.0   |
    Et le total du mois est de 21.0 jours

  Scénario: Un mois incomplet ne peut pas être exporté
    Étant donné qu'Alice a saisi les périodes suivantes :
      | du         | au         | projet           | jours |
      | 2025-04-01 | 2025-04-18 | Refonte extranet | 1     |
      | 2025-04-22 | 2025-04-22 | Refonte extranet | 0.5   |
    Quand on établit le compte rendu d'activité
    Alors l'export est refusé avec 21 jours ouvrés attendus et 14 jours saisis

  Scénario: Plus d'un jour sur une même date est refusé
    Étant donné qu'Alice a saisi les périodes suivantes :
      | du         | au         | projet           | jours |
      | 2025-04-02 | 2025-04-02 | Refonte extranet | 1     |
    Quand Alice saisit 0.5 jour sur "Intercontrat" le 2025-04-02
    Alors la saisie est rejetée pour la règle "SAISIE_JOUR_DEPASSE"

  Plan du scénario: Quantités de saisie
    Quand Alice saisit <jours> jour sur "Refonte extranet" le 2025-04-03
    Alors la saisie est <résultat>

    Exemples:
      | jours | résultat                                          |
      | 1     | acceptée                                          |
      | 0.5   | acceptée                                          |
      | 0.25  | rejetée pour la règle "SAISIE_QUANTITE_INVALIDE"  |
      | 2     | rejetée pour la règle "SAISIE_QUANTITE_INVALIDE"  |
