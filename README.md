# NumidiaPath - Plateforme Touristique  

**NumidiaPath** (TravelShare) est une application Android native conçue pour transformer l'expérience de partage de voyages. Grâce à l'intégration de l'intelligence artificielle et d'une architecture cloud temps réel, elle automatise la découverte et l'organisation des souvenirs de voyage.

---

##  Fonctionnalités Clés

- **Authentification Flexible :** Connexion via Firebase Auth (Email/MDP) ou **Mode Anonyme** pour une exploration immédiate.
- **Publication Assistée par IA :** Analyse locale des images via **Google ML Kit** pour générer automatiquement des tags sémantiques (ex: #Plage, #Montagne).
- **Géo-Intelligence :** Cartographie interactive basée sur **OSMDroid** permettant de visualiser les posts et de lancer des itinéraires via Deep Linking vers Google Maps/Waze.
- **Interactions Sociales :** Système de "Follow", gestion de groupes communautaires, likes et commentaires en temps réel.
- **Mode Offline & Performance :** Gestion du cache avec **Glide** et synchronisation asynchrone avec Firestore.

---

##  Stack Technologique

- **Langage :** Java 11 / Android SDK (Min API 24)
- **Architecture :** Pattern MVA (Modèle-Vue-Adaptateur) avec Single-Activity Strategy.
- **Backend (BaaS) :** Firebase (Firestore, Cloud Storage, Authentication).
- **IA locale :** Google ML Kit (Image Labeling).
- **Cartographie :** OSMDroid (OpenStreetMap).
- **UI/UX :** Material Design 3, View Binding.

---

##  Conception & Architecture

### Architecture Logicielle
L'application respecte la séparation des préoccupations (**Separation of Concerns**) via un découpage en trois couches :
1. **Couche Présentation :** Fragments modulaires et View Binding.
2. **Couche Logique :** Services de traitement (IA, Géo-calcul).
3. **Couche Data :** Modèles POJO et `FirestoreHelper` pour l'abstraction des données.



### Modèle de Données (NoSQL)
La base de données est structurée pour optimiser les performances de lecture et la scalabilité :
- **Users :** Profils et préférences.
- **Posts :** Données multimédias géolocalisées et taguées par l'IA.
- **Groups :** Espaces d'échanges communautaires.

---

##  Installation & Configuration

1. **Cloner le projet :**
   ```bash
   git clone https://github.com/Amir2899/NumidiaPath
2. **Configuration Firebase :**
    - Créez un projet sur la console Firebase.
    - Ajoutez une application Android et téléchargez le fichier    google-services.json.
    - Placez-le dans le répertoire /app.
3. **Build :**     
    - Ouvrez le projet dans Android Studio.
    - Synchronisez Gradle et lancez l'application sur un émulateur ou un appareil physique.
