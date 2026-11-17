# language: fr
Fonctionnalité: Droits à congés payés selon la date d'embauche
  Règle complétée (absente du legacy). Code du travail : 2,5 jours ouvrables par mois de travail
  effectif (L3141-3), un mois équivaut à 24 jours ouvrables (L3141-4), arrondi à l'entier supérieur
  (L3141-7). Période de référence du 1er juin au 31 mai.

  Plan du scénario: Droits acquis sur la période de référence 2024-2025
    Étant donné un collaborateur embauché le <embauche>
    Et un décompte en jours "<mode>"
    Quand on calcule ses droits au 2025-06-01
    Alors il a acquis <bruts> jours avant arrondi
    Et ses droits retenus sont de <retenus> jours

    Exemples:
      | embauche   | mode      | bruts | retenus |
      | 2020-01-06 | ouvrables | 30.00 | 30      |
      | 2020-01-06 | ouvrés    | 25.00 | 25      |
      | 2024-09-16 | ouvrables | 21.35 | 22      |
      | 2024-09-16 | ouvrés    | 17.81 | 18      |
      | 2024-09-02 | ouvrables | 22.50 | 23      |
      | 2025-03-01 | ouvrés    | 6.25  | 7       |

  Scénario: Acquisition en cours, mesurée à la date du jour
    Étant donné un collaborateur embauché le 2024-09-16
    Et un décompte en jours "ouvrables"
    Et nous sommes le 2024-12-15
    Quand on calcule ses droits à la date du jour
    Alors il a acquis 6.35 jours avant arrondi
    Et la période de référence n'est pas close

  Scénario: Le mois en cours est acquis le dernier jour du mois
    Étant donné un collaborateur embauché le 2024-09-16
    Et un décompte en jours "ouvrables"
    Et nous sommes le 2024-12-31
    Quand on calcule ses droits à la date du jour
    Alors il a acquis 8.85 jours avant arrondi

  Scénario: Un stagiaire n'acquiert pas de congés payés
    Étant donné un stagiaire embauché le 2024-06-01
    Et un décompte en jours "ouvrés"
    Quand on calcule ses droits au 2025-06-01
    Alors ses droits retenus sont de 0 jours

  Plan du scénario: Congés d'ancienneté de la convention Syntec
    Étant donné un collaborateur embauché le <embauche>
    Et un décompte en jours "ouvrés" avec les congés d'ancienneté Syntec
    Quand on calcule ses droits au 2025-06-01
    Alors ses droits totaux sont de <total> jours

    Exemples:
      | embauche   | total |
      | 2020-06-02 | 25    |
      | 2020-06-01 | 26    |
      | 2015-01-05 | 27    |
      | 2000-01-03 | 29    |
