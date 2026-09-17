# Design – Framework de communication Broker / Channel

## Objectif

Le framework permet d'établir des **canaux de communication point-à-point** entre des tâches (`Task`) via des **brokers**. Les canaux sont des flux d'octets **FIFO**, **full-duplex** et **sans perte**. Les opérations `accept()` et `connect()` réalisent un **rendez-vous symétrique** : la première opération arrivée attend la seconde avant que le canal ne soit créé.

L'architecture suit un modèle **orienté threads**, où chaque `Task` est associée à un `Broker`.

---

# Architecture générale

Le système repose sur quatre composants principaux :

- **BrokerManager** : registre central des brokers (Singleton).
- **Broker** : intermédiaire permettant d'établir une connexion.
- **RDV (Rendez-vous)** : synchronise les opérations `accept()` et `connect()` sur chaque port.
- **Channel** : extrémité d'un canal bidirectionnel utilisant des buffers circulaires.

## Diagramme de conception

```text
                     ┌────────────────────┐
                     │   BrokerManager    │
                     │--------------------│
                     │ - brokers : Map    │
                     │--------------------│
                     │ + add(Broker)      │
                     │ + get(name)        │
                     │ + self()           │
                     └─────────┬──────────┘
                               │
                 référence      │
                               ▼
        ┌──────────────────────────────────┐
        │             Broker               │
        │----------------------------------│
        │ - name                           │
        │ - manager : BrokerManager        │
        │ - rendezVous : Map<Port,RDV>     │
        │----------------------------------│
        │ + accept(port)                   │
        │ + connect(name,port)             │
        └───────────────┬──────────────────┘
                        │ crée un rendez-vous
                        ▼
                 ┌───────────────┐
                 │      RDV      │
                 │---------------│
                 │ accept en attente │
                 │ file de connect   │
                 └───────┬────────┘
                         │
               crée deux extrémités
                         ▼
          ┌────────────────────────────┐
          │          Channel           │
          │----------------------------│
          │ in : CircularBuffer        │
          │ out : CircularBuffer       │
          │ peer : Channel             │
          │ disconnected               │
          │----------------------------│
          │ read()                     │
          │ write()                    │
          │ disconnect()               │
          └────────────────────────────┘
```

---

# BrokerManager

## Rôle

`BrokerManager` est un **Singleton** chargé de gérer tous les brokers présents dans la JVM.

Son objectif est d'éviter de transmettre une référence du gestionnaire dans toutes les classes. Une méthode statique `self()` permet de récupérer l'unique instance.

## Interface

```java
class BrokerManager {
    static BrokerManager self();
    void add(Broker b);
    Broker get(String name);
}
```

## Fonctionnement

Lorsqu'un broker est créé :

1. il récupère `BrokerManager.self()`;
2. il s'enregistre avec `add()`;
3. les autres brokers peuvent ensuite le retrouver avec `get(name)`.

Cette étape est indispensable avant qu'un `connect(name, port)` puisse retrouver le broker distant.

---

# Broker

## Structure

Chaque broker possède :

- un nom unique ;
- une référence vers le `BrokerManager` ;
- une collection de rendez-vous (`RDV`), un par port.

```text
Broker
 ├── Port 8080 → RDV
 ├── Port 8081 → RDV
 └── Port 9000 → RDV
```

---

# RDV (Rendez-vous)

Le rendez-vous synchronise les appels `accept()` et `connect()`.

Chaque port possède un unique objet `RDV`.

## Structure

Le `RDV` contient :

- un éventuel `accept()` en attente ;
- une file d'attente des `connect()`.

```text
RDV(port)

accept en attente
       │
       ▼

connect1
connect2
connect3
```

## Règles

- un seul `accept()` est autorisé par port ;
- plusieurs `connect()` peuvent attendre ;
- lorsqu'un `accept()` rencontre un `connect()`, ils sont appariés puis retirés des listes.

Cette organisation respecte le rendez-vous symétrique de la spécification.

---

# Flow d'exécution du rendez-vous

## Cas 1 — `accept()` appelé en premier

```text
Task B
   │
   │ accept(8080)
   ▼
RDV(port 8080)
   │
   │ attente
   │
Task A
   │
   │ connect("B",8080)
   ▼
Appariement
   │
   ▼
Deux Channel sont créés
```

## Cas 2 — `connect()` appelé en premier

```text
Task A
   │
   │ connect("B",8080)
   ▼
RDV(port 8080)
   │
   │ file d'attente
   │
Task B
   │
   │ accept(8080)
   ▼
Appariement
   │
   ▼
Deux Channel sont créés
```

Dans les deux cas :

- `accept()` est bloquante ;
- `connect()` est également bloquante tant que le rendez-vous n'a pas lieu.

Si le broker distant n'existe pas, `connect()` retourne `null`.

---

# Channel

## Principe

Le canal est **full-duplex** : chaque extrémité peut simultanément lire et écrire des octets.

Le design repose sur **deux objets `Channel`**, chacun représentant une extrémité de la communication.

## Constructeurs

### Premier constructeur

```java
Channel()
```

Initialise deux buffers circulaires indépendants.

```text
Channel A

 in
 out
```

### Second constructeur

```java
Channel(Channel c)
```

Le second canal partage les buffers du premier de manière croisée.

```text
Avant :

Channel A

 inA
 outA

Après création :

Channel A              Channel B

 inA  ◄──────────── outB
 outA ────────────► inB
```

Autrement dit :

- `B.in = A.out`
- `B.out = A.in`

Les deux objets représentent donc les deux extrémités d'un même canal logique.

---

# Diagramme d'objets

Une fois la connexion établie, les brokers ne transportent plus les données. Les deux tâches communiquent directement à travers les deux extrémités du canal.

```text
                 Établissement de la connexion

┌──────────────┐                      ┌──────────────┐
│    Task A    │                      │    Task B    │
│   (Thread)   │                      │   (Thread)   │
└──────┬───────┘                      └──────┬───────┘
       │                                     │
       ▼                                     ▼
┌──────────────┐                      ┌──────────────┐
│   Broker A   │◄────────────────────►│   Broker B   │
└──────┬───────┘                      └──────┬───────┘
       └──────────────┬──────────────────────┘
                      ▼
          ┌────────────────────────┐
          │  Channel A   Channel B │
          │ (deux extrémités liées)│
          └────────────────────────┘
```

---

# Buffers circulaires

Chaque extrémité possède :

- un buffer d'entrée (`in`) ;
- un buffer de sortie (`out`).

```text
Task A                 Task B

write()                read()

   │                     ▲
   ▼                     │
 outA ───────────────► inB


Task B                 Task A

write()                read()

   │                     ▲
   ▼                     │
 outB ───────────────► inA
```

Le buffer circulaire garantit :

- un ordre FIFO ;
- une écriture progressive ;
- une lecture progressive.

---

# Synchronisation

Le `Channel` est une ressource partagée. Le modèle **lecteur-écrivain** est particulièrement adapté, car plusieurs tâches peuvent partager une même extrémité du canal.

## Règles de concurrence

Autorisé :

- un lecteur et un écrivain simultanément sur une même extrémité ;
- les deux extrémités peuvent lire et écrire en parallèle.

Interdit :

- deux lectures concurrentes sur la même extrémité ;
- deux écritures concurrentes sur la même extrémité.

Les tâches qui partagent une extrémité doivent donc se synchroniser.

---

# Gestion des blocages

| Opération | Comportement |
|-----------|-------------|
| `accept()` | attend un `connect()` |
| `connect()` | attend un `accept()` |
| `read()` | attend lorsque `in` est vide |
| `write()` | attend lorsque `out` est plein |
| `disconnect()` | réveille les opérations bloquées |

Un point important de la spécification est que :

- une lecture bloquée ne doit jamais empêcher une écriture de progresser ;
- une écriture bloquée ne doit jamais empêcher une lecture de progresser.

---

# Flow d'exécution complet

Le déroulement typique d'une communication est le suivant.

## 1. Création

- création des `Task` ;
- création des `Broker` ;
- enregistrement des brokers dans `BrokerManager`.

## 2. Connexion

- `Task B` appelle `accept(port)` ;
- `Task A` appelle `connect(name, port)` ;
- le `RDV` associe les deux appels ;
- deux objets `Channel` sont créés.

## 3. Communication

- `write()` dépose des octets dans `out` ;
- les octets apparaissent dans `in` du canal distant ;
- `read()` les récupère dans l'ordre FIFO.

## 4. Déconnexion

- une extrémité appelle `disconnect()` ;
- les opérations bloquées sont réveillées ;
- les derniers octets déjà écrits restent lisibles avant la fermeture complète.
