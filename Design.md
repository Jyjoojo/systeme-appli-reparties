# Design des classes `Broker`, `Channel` et `Task`

## Vue d'ensemble

L'architecture repose sur trois classes abstraites qui séparent les responsabilités entre la gestion des connexions, la communication et l'exécution des tâches.

- **`Broker`** : gère l'établissement des connexions entre les tâches.
- **`Channel`** : représente un canal de communication bidirectionnel.
- **`Task`** : exécute un traitement dans un thread tout en étant associé à un `Broker`.

## Diagramme de conception

```text
                 Thread
                    ▲
                    │
                 Task
                    │
     utilise        │ getBroker()
                    ▼
                 Broker
               /         \
      accept()            connect()
               \         /
                ▼       ▼
                 Channel
              ┌───────────────┐
              │ read()        │
              │ write()       │
              │ disconnect()  │
              │ disconnected()│
              └───────────────┘
```

## Description des classes

### `Broker`

Le `Broker` est responsable de la création et de la gestion des connexions.

#### Responsabilités

- Identifier un point de communication.
- Attendre une connexion entrante (`accept`).
- Établir une connexion vers un autre broker (`connect`).
- Retourner un `Channel` représentant la connexion.

#### Comportement

- **`accept(int port)`** est une méthode **bloquante** : le thread appelant attend jusqu'à ce qu'une autre tâche établisse une connexion sur le port concerné.
- **`connect(String name, int port)`** recherche le broker distant et s'associe avec un éventuel `accept()` en attente afin de créer les deux extrémités du canal.

---

### `Channel`

Le `Channel` encapsule les échanges de données entre deux extrémités d'une connexion.

#### Responsabilités

- Lire des données (`read`).
- Écrire des données (`write`).
- Fermer la connexion (`disconnect`).
- Indiquer l'état de la connexion (`disconnected`).

#### Comportement

- **`read(byte[], int, int)`** est une méthode **bloquante** : si aucune donnée n'est disponible dans le canal, le thread lecteur attend jusqu'à l'arrivée de nouvelles données ou à la fermeture du canal.
- **`write(byte[], int, int)`** dépose les octets dans le tampon du canal et réveille les lecteurs éventuellement en attente.
- **`disconnect()`** ferme définitivement le canal et réveille les threads bloqués afin qu'ils puissent terminer proprement.
- **`disconnected()`** permet de connaître l'état actuel du canal.

---

### `Task`

`Task` hérite de `Thread` et représente une unité d'exécution.

#### Responsabilités

- Exécuter un traitement (`Runnable`).
- Conserver une référence vers le `Broker` associé.
- Permettre l'accès au broker courant via `getBroker()`.

---

## Synchronisation : modèle lecteur-écrivain

Le `Channel` constitue une ressource partagée entre plusieurs tâches. Il est donc pertinent d'utiliser le modèle **lecteur-écrivain**, car plusieurs tâches peuvent simultanément lire ou envoyer des octets à travers un même canal.

### Principe

- Les **écrivains** utilisent `write()` pour déposer des données dans le tampon partagé.
- Les **lecteurs** utilisent `read()` pour récupérer les données disponibles.
- L'accès au tampon doit être protégé afin d'éviter les accès concurrents incohérents.
- Lorsqu'aucune donnée n'est disponible, les lecteurs sont mis en attente.
- Lorsqu'un écrivain ajoute des données, les lecteurs concernés sont réveillés.

### Gestion des blocages

| Méthode | Comportement |
|---------|-------------|
| `accept()` | Bloque jusqu'à l'arrivée d'une connexion. |
| `connect()` | Attend la disponibilité du broker distant si nécessaire. |
| `read()` | Bloque lorsque le tampon est vide. |
| `write()` | Réveille les lecteurs après l'écriture. |
| `disconnect()` | Réveille les threads bloqués avant la fermeture définitive. |

### Cycle d'un canal

```text
          read() sans données
Connecté ----------------------> Lecture bloquée
    ▲                                 │
    │                                 │ write()
    └─────────────────────────────────┘
                 disconnect()
                     │
                     ▼
               Déconnecté
```

---

## Relations entre les classes

| Relation | Description |
|----------|-------------|
| `Task` hérite de `Thread` | Une tâche est exécutée comme un thread. |
| `Task` utilise `Broker` | Chaque tâche est associée à un broker. |
| `Broker` crée des `Channel` | Les méthodes `accept()` et `connect()` retournent un canal. |
| `Channel` relie plusieurs tâches | Il transporte les données entre les extrémités de la connexion. |

---
## Diagramme d'objets

Le `Broker` intervient uniquement lors de l'établissement de la connexion. Une fois celle-ci créée, les deux tâches communiquent à travers un **unique `Channel` bidirectionnel**, capable de transférer des octets dans les deux sens.

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
              ┌────────────────┐
              │    Channel     │
              │────────────────│
              │ read(...)      │
              │ write(...)     │
              │ disconnect()   │
              └────────────────┘
```

Chaque `Task` possède son propre `Broker`, mais les deux tâches utilisent le même canal logique pour envoyer et recevoir des octets.

---

## Flow d'exécution

Le scénario suivant décrit le déroulement d'une communication typique entre deux tâches.

### 1. Initialisation

- `Task A` et `Task B` sont créées.
- Chaque tâche est associée à son `Broker`.

### 2. Attente de connexion

- `Task B` appelle `Broker.accept(port)`.
- Cette opération est **bloquante** jusqu'à l'arrivée d'une connexion.

### 3. Établissement de la connexion

- `Task A` appelle `Broker.connect(name, port)`.
- Le `Broker` associe cet appel au `accept()` en attente.
- Un `Channel` bidirectionnel est créé et partagé entre les deux tâches.

### 4. Échange de données

- Une tâche appelle `write()` pour envoyer des octets.
- Les données sont placées dans le tampon du canal.
- Une tâche bloquée sur `read()` est réveillée et récupère les octets disponibles.

### 5. Fermeture

- Une tâche appelle `disconnect()`.
- Le canal est marqué comme fermé.
- Les opérations `read()` ou `write()` en attente sont débloquées afin de terminer proprement.

---

## Diagramme de séquence

```text
Task B                 Broker B        Broker A          Task A
  │                       │               │                │
  │── accept(port) ─────► │               │                │
  │    (bloqué)           │               │                │
  │                       │ ◄──────────── connect() ───────│
  │                       │               │                │
  │◄────── Channel bidirectionnel établi ────────────────►│
  │                       │               │                │
  │── read() ───────────► │               │                │
  │   (attente si vide)   │               │                │
  │                       │               │◄─ write(bytes) │
  │◄────────── données ───────────────────────────────────│
  │                       │               │                │
  │────────── disconnect() ──────────────────────────────►│
```

---

## Synchronisation du `Channel`

Le `Channel` est une ressource partagée entre plusieurs tâches et permet une communication **bidirectionnelle**. Chaque tâche peut envoyer des octets avec `write()` et recevoir les octets envoyés par l'autre extrémité avec `read()`.

Le modèle **lecteur-écrivain** est adapté à cette conception :

- les écrivains déposent les données dans le tampon partagé via `write()`,
- les lecteurs récupèrent les données via `read()`,
- l'accès au tampon est synchronisé afin d'éviter les accès concurrents incohérents,
- `read()` bloque lorsque le tampon est vide,
- `write()` réveille les lecteurs en attente,
- `disconnect()` réveille les opérations bloquées avant de fermer définitivement le canal.
