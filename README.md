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
