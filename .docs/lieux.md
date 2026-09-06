# Lieux — le référentiel de points (ex-Mapix)

Doc vivant de l'espace Lieux. Le design complet (5 use cases, lots A–E, architecture
KMP/MapLibre) est figé dans [2026-08-20-espace-lieux-refinement.md](2026-08-20-espace-lieux-refinement.md)
et le Lot A dans [2026-08-20-lot-a-walkthrough.md](2026-08-20-lot-a-walkthrough.md)
(archives, on n'y touche plus). Ici : l'état courant, les décisions nouvelles, la todo.

## Questions ouvertes

- [ ] **Les KMZ ont été poussés sur GitHub (dépôt public)** dans `.raw/` le 2026-09-06
  (commit `a4c34e0`, déjà sur `origin/main`). Ils sont maintenant sortis du suivi et
  rangés dans le vault, mais ils **restent dans l'historique public**. Deux options :
  (a) réécrire l'historique (`git filter-repo --path .raw --invert-paths`) puis force-push
  via GitKraken ; (b) assumer (cartes de voyage, pas de coordonnées de domicile ?) — **à
  vérifier avant de choisir** : `Paris.kmz` (BD / Board Game) et les « Accomodations »
  contiennent-ils des adresses personnelles ?
- [ ] Les **listes enregistrées (Saved, CSV)** ne sont pas encore là : seulement les cartes
  My Maps. Takeout « Enregistrés » à demander, ou on démarre le Lot A avec My Maps seuls ?
- [ ] 6 calques « Untitled layer » (≈ 200 points) : les nommer dans My Maps avant
  ré-export, ou les nommer côté ETL (`<carte> / sans nom N`) ?
- [ ] Stratégie de sauvegarde de `places_state.db` (registre d'IDs, non reconstructible)
  — toujours ouverte depuis le Lot A.

## État au 2026-09-06

**Le vault réel est arrivé (My Maps).** 12 cartes KMZ exportées de Google My Maps,
déposées dans `data/places/` (gitignoré). Inventaire (`npm run places:inventory`) passé
**sans modification du parser** :

| Carte | Points | Calques |
|---|---|---|
| Region : Asia | 495 | 9 (dont 1 sans nom : 142 pts) |
| Copy of Japan Map - Camgo rental car | 494 | 10 |
| Region : West Africa | 246 | 10 |
| Region : Middle East | 218 | 10 |
| Region : South East Asia | 193 | 8 |
| Region : Southern Africa | 190 | 8 |
| Region : Central Asia | 141 | 9 |
| Ancient Roads | 127 | 7 |
| Region : Oceania | 62 | 5 |
| Legende | 32 | 5 |
| Region : Europe | 20 | 2 (tous sans nom) |
| Paris | 19 | 2 |
| **Total** | **2 237** | 0 tracé |

Observations utiles pour la suite :
- Taxonomie de calques **quasi commune** entre les cartes « Region » (Cities & Villages,
  Outdoors Activities, Touristic Sites, Historical Sites, Religious Sites, Restaurants &
  Cafe, Accomodations, Transportation, Travel Steps) avec des variantes orthographiques
  (`Outdoor` / `Outdoors`, `Historic Sties`, `Restaurant & Cafe`). → une **table de
  normalisation des catégories** s'impose (Lot A bis), ce sera la base des couches UC1.
- « Travel Steps » = étapes de voyage : c'est déjà la matière des itinéraires (Lot D).
- Aucun tracé : la non-ingestion des LineString du Lot A ne coûte rien.
- « Legende » est une carte-modèle (icônes) — probablement à exclure de l'ingestion.

## Décisions

- **Vault = `data/places/`** (déjà acté au refinement, réaffirmé) : `.raw/` était une
  zone de dépôt ; il est maintenant gitignoré pour que ça ne se reproduise pas.
- Lot A se clôt sur My Maps seuls si les CSV Saved tardent : la passe de résolution
  (étape 4) n'est utile que pour les CSV, elle attendra.

## Prochaines étapes (Lot A, clôture)

1. `npm run places:build` sur le vault réel → `places.db` + registre d'IDs ; vérifier les
   fusions inter-cartes (un même lieu dans « Region : Asia » et « Japan Map » doit fusionner
   par clé géo).
2. Normalisation des catégories de calques (table `categories` + mapping), exclusion de
   « Legende ».
3. Quand les CSV Saved arrivent : inventaire → passe de résolution (Photon vs Nominatim
   sur volumétrie réelle).
4. Puis Lot B : spike MapLibre/PMTiles (critère : 1 000 points clusterisés desktop + Android).

## Journal

### 2026-09-06
Vault My Maps réel déposé (12 KMZ, 2 237 points, 0 tracé) ; inventaire OK sans toucher
au parser. KMZ sortis du suivi git (`.raw/` → `data/places/`, `.raw/` gitignoré) — mais
déjà poussés sur le dépôt public : question ouverte sur la réécriture d'historique.
Repéré : taxonomie de calques commune à normaliser, 6 calques sans nom.
