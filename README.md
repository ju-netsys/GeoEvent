# GeoEvent Kotlin

Application Android native permettant de placer, consulter, modifier et supprimer des événements géolocalisés sur une carte OpenStreetMap, avec synchronisation en temps réel via Firebase.

---

## Fonctionnalités

- **Authentification** — Inscription avec politique de mot de passe (8+ car., majuscule, minuscule, chiffre, caractère spécial), indicateurs visuels rouge/vert en temps réel. Connexion email/mot de passe via Firebase Auth.
- **Carte interactive** — Affichage OpenStreetMap via osmdroid. Tap sur la carte → créer un événement. Tap sur un marqueur → bulle avec titre, description et auteur.
- **Modification d'événements** — Bouton crayon dans la bulle marqueur, visible uniquement pour le propriétaire. Lance l'écran d'édition avec les champs pré-remplis.
- **Suppression d'événements** — Bouton poubelle dans la bulle marqueur (propriétaire uniquement). Suppression temps réel reflétée sur toutes les sessions connectées.
- **Navigation bas de page** — Onglet **Carte** et onglet **Liste**. La liste affiche tous les événements (nom, description, coordonnées) ; cliquer un événement téléporte sur la carte.
- **Recherche de lieu** — Barre de recherche par code postal via l'API zippopotam.us (format `75001` ou `us/10001`).
- **Auteur visible** — L'email de l'auteur s'affiche dans la bulle de chaque événement sur la carte.

---

## Architecture

```
fr.itii.geoevent_kotlin
├── p1/       → Couche données & DI
│   ├── Event.kt
│   ├── EventRepository.kt
│   ├── FirestoreEventRepository.kt
│   ├── FirestoreDataSource.kt
│   └── ServiceLocator.kt
├── p2/       → Authentification
│   ├── AuthViewModel.kt
│   ├── LoginActivity.kt
│   └── RegisterActivity.kt
├── p3/       → Carte & Événements
│   ├── MapService.kt + MapState
│   ├── OsmMapService.kt
│   ├── EventAdapter.kt
│   ├── MainActivity.kt
│   ├── MapViewModel.kt
│   ├── EventViewModel.kt
│   ├── AddEventActivity.kt
│   └── EventDetailActivity.kt
└── common/   → Utilitaires partagés
    ├── UiState.kt
    └── ViewModelFactory.kt
```

**Pattern MVVM + Repository** recommandé par Google. Les ViewModels ne connaissent que l'interface `EventRepository`, jamais Firestore directement. La carte est abstraite derrière `MapService`, ce qui permet de changer de moteur cartographique sans modifier `MainActivity`.

---

## Technologies utilisées

| Technologie | Rôle | Justification |
|---|---|---|
| **Firebase Firestore** | Base de données cloud | Synchronisation temps réel multi-utilisateurs sans backend personnalisé |
| **Firebase Authentication** | Gestion des sessions | Intégration native Firestore (Security Rules sur `request.auth.uid`) |
| **osmdroid (OpenStreetMap)** | Carte interactive | Open source, sans clé API, sans quota, sans coût |
| **FusedLocationProvider** | GPS | API Google Play Services recommandée pour la localisation Android |
| **zippopotam.us** | Géocodage postal | API publique, sans clé, légère |

---

## Prérequis

- Android Studio Hedgehog ou supérieur
- JDK 17
- Un projet Firebase avec **Firestore** et **Authentication** activés
- Fichier `google-services.json` placé dans `app/`

---

## Installation

```bash
git clone https://github.com/votre-repo/GeoEventKotlin.git
cd GeoEventKotlin
# Copier google-services.json dans app/
./gradlew assembleDebug
```

---

## Configuration Firebase

1. Créer un projet sur [console.firebase.google.com](https://console.firebase.google.com)
2. Activer **Authentication** → Email/Password
3. Activer **Firestore Database** en mode production
4. Ajouter les règles Firestore suivantes :

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /events/{eventId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null;
      allow update, delete: if request.auth != null
                            && request.auth.uid == resource.data.userId;
    }
  }
}
```

5. Télécharger `google-services.json` et le placer dans `app/`

---

## Documentation technique

Le fichier `GeoEvent_Documentation_Technique.docx` à la racine du projet contient :
- L'architecture complète MVVM + Repository
- La justification des choix techniques (Firestore, osmdroid, Firebase Auth)
- Le détail de chaque fichier par package (P1, P2, P3, common)
- Les flux de données (lecture, création, modification, suppression, navigation)

---

## Branches

- `master` — branche principale stable
- `feature/p2-map` — développement actif

---

## Mascotte

🦛 GeoEvent est accompagné d'un hippopotame violet — **Hipspot** reste le nom de code interne.
