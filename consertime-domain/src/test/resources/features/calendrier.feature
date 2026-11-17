# language: fr
Fonctionnalité: Calendrier des jours fériés et des jours ouvrés
  Les jours fériés du legacy (DateUtil.isJourFerie) sont recalculés avec java.time.
  Les jours ouvrés servent au contrôle des saisies, au CRA et au décompte des congés.

  Plan du scénario: Date de Pâques
    Alors Pâques <année> tombe le <date>

    Exemples:
      | année | date       |
      | 2024  | 2024-03-31 |
      | 2025  | 2025-04-20 |
      | 2026  | 2026-04-05 |
      | 2027  | 2027-03-28 |

  Plan du scénario: Jours fériés de 2025 en métropole
    Alors le <date> est un jour férié "<libellé>"

    Exemples:
      | date       | libellé            |
      | 2025-01-01 | Jour de l'an       |
      | 2025-04-21 | Lundi de Pâques    |
      | 2025-05-01 | Fête du Travail    |
      | 2025-05-08 | Victoire 1945      |
      | 2025-05-29 | Ascension          |
      | 2025-06-09 | Lundi de Pentecôte |
      | 2025-07-14 | Fête nationale     |
      | 2025-08-15 | Assomption         |
      | 2025-11-01 | Toussaint          |
      | 2025-11-11 | Armistice 1918     |
      | 2025-12-25 | Noël               |

  Scénario: Une année compte 11 jours fériés en métropole et 13 en Alsace-Moselle
    Alors l'année 2025 compte 11 jours fériés en métropole
    Et l'année 2025 compte 13 jours fériés en Alsace-Moselle

  Plan du scénario: Jours ouvrés et jours ouvrables d'un mois
    Alors le mois <mois> compte <ouvrés> jours ouvrés et <ouvrables> jours ouvrables

    Exemples:
      | mois    | ouvrés | ouvrables |
      | 2025-04 | 21     | 25        |
      | 2025-05 | 19     | 24        |
      | 2025-06 | 20     | 24        |
