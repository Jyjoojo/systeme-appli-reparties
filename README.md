# Noms et Prénoms : KOUEHI Ange Joel Nathan (INFO 5)

# Spécification des classes `Broker`, `Channel` et `Task`

Cette spécification décrit le rôle, les préconditions, les postconditions et les invariants des classes abstraites fournies. Elle définit le contrat de chaque méthode indépendamment de son implémentation.

---

## Classe abstraite `Broker`

Représente un courtier de communication chargé d'établir des connexions entre des tâches au moyen de canaux (`Channel`).

### Constructeur

```java
Broker(String name);
```

**Rôle**

Crée un broker identifié par `name`.

**Précondition**

- `name != null`

**Postcondition**

- Le broker est créé et peut accepter ou initier des connexions.

### `accept(int port)`

```java
Channel accept(int port);
```

**Rôle**

Met le broker en attente d'une connexion entrante sur le port indiqué.

**Précondition**

- `port >= 0`

**Postcondition**

- Retourne un `Channel` représentant la connexion établie.
- Le canal retourné est connecté.

### `connect(String name, int port)`

```java
Channel connect(String name, int port);
```

**Rôle**

Établit une connexion avec le broker nommé `name` sur le port `port`.

**Préconditions**

- `name != null`
- `port >= 0`

**Postcondition**

- Retourne un `Channel` connecté au broker distant.

---

## Classe abstraite `Channel`

Représente un canal de communication bidirectionnel entre deux tâches.

### `read(byte[] bytes, int offset, int length)`

```java
int read(byte[] bytes, int offset, int length);
```

**Rôle**

Lit jusqu'à `length` octets dans le tableau `bytes` à partir de `offset`.

**Préconditions**

- `bytes != null`
- `offset >= 0`
- `length >= 0`
- `offset + length <= bytes.length`
- Le canal n'est pas déconnecté.

**Postconditions**

- Retourne le nombre d'octets effectivement lus.
- Les octets lus sont copiés dans `bytes[offset ... offset + n - 1]`.

### `write(byte[] bytes, int offset, int length)`

```java
int write(byte[] bytes, int offset, int length);
```

**Rôle**

Écrit jusqu'à `length` octets depuis `bytes` à partir de `offset`.

**Préconditions**

- `bytes != null`
- `offset >= 0`
- `length >= 0`
- `offset + length <= bytes.length`
- Le canal n'est pas déconnecté.

**Postcondition**

- Retourne le nombre d'octets effectivement écrits.

### `disconnect()`

```java
void disconnect();
```

**Rôle**

Ferme définitivement le canal.

**Postcondition**

- `disconnected()` renvoie ensuite `true`.

### `disconnected()`

```java
boolean disconnected();
```

**Rôle**

Indique si le canal est fermé.

**Postcondition**

- Retourne `true` si le canal est déconnecté, sinon `false`.

---

## Classe abstraite `Task`

Représente une tâche d'exécution (`Thread`) associée à un `Broker`.

### Constructeur

```java
Task(Broker b, Runnable r);
```

**Rôle**

Crée une tâche exécutant le comportement défini par `r` et associée au broker `b`.

**Préconditions**

- `b != null`
- `r != null`

**Postconditions**

- La tâche est créée.
- Elle est associée au broker `b`.

### `getBroker()`

```java
static Broker getBroker();
```

**Rôle**

Retourne le broker associé à la tâche courante.

**Précondition**

- La méthode est appelée dans le contexte d'une `Task`.

**Postcondition**

- Retourne le `Broker` associé à la tâche courante.

---

# Invariants

| Classe | Invariant |
|--------|-----------|
| `Broker` | Possède une identité (`name`) valide pendant toute sa durée de vie. |
| `Channel` | Un canal est soit connecté, soit définitivement déconnecté. |
| `Task` | Chaque tâche est associée à un unique `Broker`. |

---

## Résumé des responsabilités

| Classe | Responsabilité principale |
|--------|---------------------------|
| `Broker` | Établir et accepter des connexions entre tâches. |
| `Channel` | Assurer une communication bidirectionnelle par lecture et écriture d'octets. |
| `Task` | Exécuter une tâche (`Thread`) tout en étant associée à un `Broker`. |

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
              ┌─────────────┐
              │ read()      │
              │ write()     │
              │ disconnect()│
              │ disconnected() │
              └─────────────┘
```

## Description des classes

### `Broker`

Le `Broker` est responsable de la création et de la gestion des connexions.

#### Responsabilités

- Identifier un point de communication.
- Attendre une connexion entrante (`accept`).
- Établir une connexion vers un autre broker (`connect`).
- Retourner un `Channel` représentant la connexion.

### `Channel`

Le `Channel` encapsule les échanges de données entre deux extrémités d'une connexion.

#### Responsabilités

- Lire des données (`read`).
- Écrire des données (`write`).
- Fermer la connexion (`disconnect`).
- Indiquer l'état de la connexion (`disconnected`).

Le canal représente une communication bidirectionnelle entre deux tâches.

### `Task`

`Task` hérite de `Thread` et représente une unité d'exécution.

#### Responsabilités

- Exécuter un traitement (`Runnable`).
- Conserver une référence vers le `Broker` associé.
- Permettre l'accès au broker courant via `getBroker()`.

## Relations entre les classes

| Relation | Description |
|----------|-------------|
| `Task` hérite de `Thread` | Une tâche est exécutée comme un thread. |
| `Task` utilise `Broker` | Chaque tâche est associée à un broker. |
| `Broker` crée des `Channel` | Les méthodes `accept()` et `connect()` retournent un canal. |
| `Channel` relie deux tâches | Il transporte les données entre les deux extrémités de la connexion. |

## Choix de conception

- **Abstraction** : Les trois classes sont abstraites afin de permettre plusieurs implémentations (TCP, mémoire partagée, simulation, etc.).
- **Séparation des responsabilités** : La gestion des connexions (`Broker`) est indépendante des échanges de données (`Channel`) et de l'exécution (`Task`).
- **Faible couplage** : Les tâches manipulent uniquement l'interface du broker et du canal, sans dépendre de leur implémentation concrète.
