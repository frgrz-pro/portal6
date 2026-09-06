---
name: root-session
description: Rituel de démarrage de session sur portal6 — charge l'index .docs/, identifie le(s) sujet(s) de la session, et applique la méthodo « notes de design vivantes » (créer/enrichir les docs, journal daté, questions ouvertes). Invoquer en début de CHAQUE session de travail, ou quand l'utilisateur dit « root », « on reprend », « nouvelle session ».
---

# root-session — rituel de session portal6

Méthodo de François : le travail de fond vit dans `.docs/` (notes de design
vivantes, un doc par sujet). Une session = enrichir ces docs, et seulement
ensuite/en parallèle produire du code.

## Étapes au démarrage

1. **Lire `.docs/README.md`** (l'index + les règles).
2. Identifier le(s) sujet(s) de la session à partir de la demande de
   l'utilisateur, et **lire les docs concernés** de l'index.
3. Relire en priorité les sections **Questions ouvertes** des docs concernés :
   c'est la todo héritée des sessions précédentes. Si la demande du jour en
   tranche une, la traiter (et la sortir de la liste).
4. S'il n'est pas évident de rattacher la demande à un doc existant, demander ou
   créer un nouveau doc (+ ligne dans l'index).

## Pendant la session

- Toute **décision actée** → corps du doc concerné (pas seulement dans la
  conversation).
- Toute **question soulevée non tranchée** → section « Questions ouvertes » du doc.
- Nouvelle info apprise (modèle de hardware, test réalisé, piste écartée) →
  intégrée au doc, avec le pourquoi si une piste est écartée.
- **Tout besoin matériel évoqué → une ligne dans `.docs/bom.md`** (le BOM
  central, tous projets) : statut (✅/🛒/⏸/❓), prix estimé, condition de
  déblocage. Un achat réalisé passe en ✅ ; un BOM détaillé de projet
  (ex. `hardware/bom.md`) reste la source du détail, le central agrège.

## En fin de session (ou après un gros jalon)

1. Ajouter/compléter l'entrée **Journal** datée (`### AAAA-MM-JJ`) dans chaque
   doc modifié : 2-3 lignes, ce qui a été décidé/appris/produit.
2. Mettre à jour l'index `.docs/README.md` si des docs sont apparus.
3. Proposer un commit (message en français, préfixé par le domaine, ex.
   `remote : cadrage v1 tab Lights`). **Ne jamais push** — GitKraken s'en charge.

## Garde-fous

- `docs/` (sans point) = archives datées du domaine musique : ne pas y écrire.
- Ne pas dupliquer dans `.docs/` ce que le code ou les README de domaine disent
  déjà — les docs portent le design, les décisions et les questions, pas la
  paraphrase du code.
