# language: fr
Fonctionnalité: Demandes de congés
  Règles extraites du legacy (DemandeCongeFormPanel, CongesAdminPage, requêtes DemandeConge.queries)
  et corrigées là où il se trompait : dernier jour oublié dans le décompte, référent qui voyait
  tous les départements, absence d'annulation.

  Contexte:
    Étant donné les collaborateurs suivants :
      | prénom | nom      | matricule | département | entrée     | rôles               |
      | Rachel | Referent | 1001      | DIG         | 2018-09-03 | REFERENT,CONSULTANT |
      | Alice  | Martin   | 1002      | DIG         | 2020-01-06 | CONSULTANT          |
      | Bruno  | Petit    | 1003      | DIG         | 2021-03-01 | CONSULTANT          |
      | Chloé  | Durand   | 1004      | CLD         | 2024-09-02 | CONSULTANT          |
    Et le département "DIG" a pour référente Rachel
    Et le département "CLD" n'a pas de référent
    Et nous sommes le 2025-05-05 à 14:00

  Plan du scénario: Nombre de jours d'une demande
    Quand Alice demande un congé "CP" du <début> <depuis> au <fin> <jusqu'à>
    Alors la demande compte <jours> jours

    Exemples:
      | début      | depuis     | fin        | jusqu'à | jours | commentaire                               |
      | 2025-06-16 | matin      | 2025-06-20 | soir    | 5     | semaine complète, le legacy en comptait 4 |
      | 2025-06-16 | après-midi | 2025-06-17 | midi    | 1     | le legacy comptait 0 et refusait          |
      | 2025-06-13 | après-midi | 2025-06-16 | midi    | 1     | week-end ignoré                           |
      | 2025-05-29 | matin      | 2025-05-30 | soir    | 1     | Ascension fériée                          |
      | 2025-06-09 | matin      | 2025-06-13 | soir    | 4     | lundi de Pentecôte férié                  |
      | 2025-06-02 | après-midi | 2025-06-02 | soir    | 0.5   | demi-journée                              |

  Scénario: Une période sans jour ouvré est refusée
    Quand Alice demande un congé "CP" du 2025-06-14 matin au 2025-06-15 soir
    Alors l'opération est rejetée pour la règle "CONGE_SANS_JOUR_OUVRE"

  Scénario: Une demande qui chevauche une demande en attente est refusée
    Étant donné qu'Alice a demandé un congé "RTT" du 2025-05-30 matin au 2025-05-30 soir
    Quand Alice demande un congé "CP" du 2025-05-26 matin au 2025-05-30 soir
    Alors l'opération est rejetée pour la règle "CONGE_CHEVAUCHEMENT"

  Scénario: Le matin et l'après-midi d'un même jour peuvent faire l'objet de deux demandes
    Étant donné qu'Alice a demandé un congé "RTT" du 2025-06-02 matin au 2025-06-02 midi
    Quand Alice demande un congé "CP" du 2025-06-02 après-midi au 2025-06-02 soir
    Alors la demande compte 0.5 jours
    Et la demande est "En attente"

  Scénario: La référente du département valide et les saisies d'absence sont produites
    Étant donné qu'Alice a demandé un congé "CP" du 2025-06-16 après-midi au 2025-06-18 midi
    Quand Rachel valide la demande
    Alors la demande est "Validée"
    Et les saisies d'absence suivantes sont produites :
      | date       | jours | projet |
      | 2025-06-16 | 0.5   | CP     |
      | 2025-06-17 | 1.0   | CP     |
      | 2025-06-18 | 0.5   | CP     |

  Scénario: Un référent d'un autre département ne peut pas décider
    Étant donné que Chloé a demandé un congé "CP" du 2025-06-02 matin au 2025-06-06 soir
    Quand Rachel valide la demande
    Alors l'opération est rejetée pour la règle "CONGE_DECIDEUR_NON_HABILITE"
    Et la demande est "En attente"

  Scénario: Un consultant ne peut pas décider
    Étant donné qu'Alice a demandé un congé "RTT" du 2025-05-30 matin au 2025-05-30 soir
    Quand Bruno refuse la demande
    Alors l'opération est rejetée pour la règle "CONGE_DECIDEUR_NON_HABILITE"

  Scénario: Département sans référent, tous les référents sont prévenus
    Quand Chloé demande un congé "CP" du 2025-06-02 matin au 2025-06-06 soir
    Alors les destinataires de la demande sont "Rachel"
    Et la configuration est signalée incomplète

  Scénario: Annuler la veille un congé validé
    Étant donné qu'Alice a demandé un congé "RTT" du 2025-05-30 matin au 2025-05-30 soir
    Et que Rachel a validé la demande
    Et nous sommes le 2025-05-29 à 18:00
    Quand Alice annule la demande
    Alors la demande est "Annulée"
    Et le nombre de saisies à retirer est 1

  Scénario: Un congé validé ne s'annule plus une fois commencé
    Étant donné qu'Alice a demandé un congé "RTT" du 2025-05-30 matin au 2025-05-30 soir
    Et que Rachel a validé la demande
    Et nous sommes le 2025-05-30 à 08:00
    Quand Alice annule la demande
    Alors l'opération est rejetée pour la règle "CONGE_ANNULATION_TARDIVE"
    Et la demande est "Validée"

  Scénario: Relance des demandes sans réponse après 3 jours ouvrés
    Étant donné nous sommes le 2025-05-28 à 10:00
    Et qu'Alice a demandé un congé "CP" du 2025-07-07 matin au 2025-07-11 soir
    Quand nous sommes le 2025-06-02 à 09:00
    Alors la demande n'est pas à relancer après 3 jours ouvrés
    Quand nous sommes le 2025-06-03 à 09:00
    Alors la demande est à relancer après 3 jours ouvrés
