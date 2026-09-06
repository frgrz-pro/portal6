/* Portal6 — vue mediacenter (music.html) : playlists × plateformes, groupes, sync.
 *
 * Donnée : window.PORTAL6 (data/manifest.js). État utilisateur (groupes, maître par
 * playlist) : localStorage, clé `portal6.music`, exportable en JSON.
 * Les actions (push, dédup) ne font qu'afficher la commande et les prérequis :
 * rien n'est écrit tant que la sync n'a pas de socle (IDs Spotify, lib iTunes).
 */
(function () {
  'use strict';

  const D = window.PORTAL6;
  if (!D || !D.music || !D.music.db) {
    document.getElementById('missing').hidden = false;
    return;
  }
  document.getElementById('content').hidden = false;

  const M = D.music;
  const PL = M.db.playlists || [];
  const STORE_KEY = 'portal6.music';

  const $ = (id) => document.getElementById(id);
  const num = (n) => (n == null ? '—' : n.toLocaleString('fr-FR'));
  const pct = (n, d) => (d ? Math.round((n / d) * 100) : 0);
  const esc = (s) => String(s).replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));

  // ------------------------------------------------------------- état local
  // { groups: {name: [playlistId]}, master: {playlistId: 'local'|'spotify'|'itunes'} }
  function load() {
    try { return JSON.parse(localStorage.getItem(STORE_KEY)) || { groups: {}, master: {} }; }
    catch { return { groups: {}, master: {} }; }
  }
  function save() {
    try { localStorage.setItem(STORE_KEY, JSON.stringify(S)); } catch { /* stockage indisponible */ }
  }
  const S = load();
  S.groups = S.groups || {};
  S.master = S.master || {};

  const groupOf = (id) => Object.keys(S.groups).find((g) => S.groups[g].includes(id)) || '';

  // ---------------------------------------------------------------- statut
  // sync : tous les titres Spotify ont un fichier ; partial : une partie ; missing : aucun.
  function statusOf(p) {
    if (!p.spotify) return 'empty';
    if (p.local >= p.spotify) return 'sync';
    if (p.local > 0) return 'partial';
    return 'missing';
  }
  const STATUS = {
    sync: ['✓ synchronisée', 'ok'],
    partial: ['◐ partielle', 'warn'],
    missing: ['✗ absente en local', 'bad'],
    empty: ['— vide', ''],
  };

  // ------------------------------------------------------------------ KPIs
  const by = { sync: 0, partial: 0, missing: 0, empty: 0 };
  PL.forEach((p) => by[statusOf(p)]++);
  const totalSpotify = PL.reduce((a, p) => a + (p.spotify || 0), 0);
  const totalLocal = PL.reduce((a, p) => a + (p.local || 0), 0);

  $('kpis').innerHTML = [
    { n: num(PL.length), l: 'playlists (source : Spotify)' },
    { n: num(by.sync), l: 'synchronisées local ↔ Spotify', tone: 'ok' },
    { n: num(by.partial), l: 'partielles', tone: 'warn' },
    { n: num(by.missing), l: 'sans aucun fichier local', tone: 'bad' },
    { n: `${pct(totalLocal, totalSpotify)} %`, l: `des ${num(totalSpotify)} titres en playlist ont un fichier` },
    { n: '0', l: 'playlists iTunes (bibliothèque à créer)', tone: 'bad' },
  ].map((s) => `<div class="stat ${s.tone || ''}"><div class="n">${s.n}</div><div class="l">${s.l}</div></div>`).join('');

  // --------------------------------------------------------------- doublons
  const sd = M.spotify_dupes || {};
  $('dedup').innerHTML = `
    <div class="card">
      <span class="tag">local</span>
      <h3>Fichiers <code>M:\\music</code></h3>
      <p>${num(M.local_quarantined)} doublons déjà déplacés vers <code>_a_trier</code>
         le 2026-08-20. Rescan : ${num(M.local_dupes_pending || 0)} restant.</p>
      <p style="margin-top:10px"><a class="btn" href="music-dedup.html#local">Résoudre les doublons</a></p>
    </div>
    <div class="card">
      <span class="tag">spotify</span>
      <h3>Playlists Spotify</h3>
      <p>${num(sd.intra)} titres en double <em>dans</em> une même playlist,
         ${num(sd.inter)} présents dans plusieurs playlists (onglet « doublons » de l'export).</p>
      <p style="margin-top:10px"><a class="btn" href="music-dedup.html#intra">Nettoyer Spotify</a></p>
    </div>
    <div class="card">
      <span class="tag">propositions</span>
      <h3>Découpes &amp; déplacements</h3>
      <p>${num(M.proposals?.splits || 0)} sous-playlists proposées pour ${num(M.proposals?.monoliths || 0)} monolithes,
         ${num(M.proposals?.moves || 0)} titres hors profil à déplacer (analyses de juillet 2026).</p>
      <p style="margin-top:10px"><a class="btn" href="music-proposals.html">Revoir les propositions</a></p>
    </div>
    <div class="card disabled">
      <span class="tag">itunes</span>
      <h3>Bibliothèque iTunes</h3>
      <p>Pas encore créée. Elle sera <em>construite</em> depuis le référentiel, donc sans doublon par construction.</p>
    </div>`;

  // ------------------------------------------------------------------ table
  const rowsEl = $('rows');
  const selected = new Set();

  function render() {
    const q = $('q').value.trim().toLowerCase();
    const fg = $('f-group').value;
    const fs = $('f-status').value;

    const list = PL.filter((p) => {
      if (q && !(p.name + ' ' + p.theme + ' ' + p.genres).toLowerCase().includes(q)) return false;
      if (fg && groupOf(p.id) !== fg) return false;
      if (fs && statusOf(p) !== fs) return false;
      return true;
    });

    rowsEl.innerHTML = list.map((p) => {
      const st = statusOf(p);
      const [label, tone] = STATUS[st];
      const master = S.master[p.id] || 'spotify';
      const g = groupOf(p.id);
      return `<tr data-id="${p.id}">
        <td><input type="checkbox" class="sel" ${selected.has(p.id) ? 'checked' : ''}></td>
        <td><strong>${esc(p.name)}</strong>
            ${p.theme ? `<div class="sub">${esc(p.theme)}</div>` : ''}</td>
        <td>${g ? `<span class="pill">${esc(g)}</span>` : '<span class="muted">—</span>'}</td>
        <td class="num plat">${num(p.spotify)}</td>
        <td class="num plat">${num(p.local)}<div class="sub">${pct(p.local, p.spotify)} %</div></td>
        <td class="num plat muted">—</td>
        <td><span class="pill ${tone}">${label}</span></td>
        <td>
          <select class="master" aria-label="Maître">
            <option value="spotify" ${master === 'spotify' ? 'selected' : ''}>Spotify</option>
            <option value="local" ${master === 'local' ? 'selected' : ''}>Local</option>
            <option value="itunes" ${master === 'itunes' ? 'selected' : ''}>iTunes</option>
          </select>
        </td>
        <td><button class="btn small" data-action="push">Push</button></td>
      </tr>`;
    }).join('');

    $('count').textContent = `${list.length} / ${PL.length} playlists affichées`;
    $('btn-assign').disabled = selected.size === 0;
  }

  function refreshGroupSelect() {
    const sel = $('f-group');
    const cur = sel.value;
    sel.innerHTML = '<option value="">Tous les groupes</option>'
      + Object.keys(S.groups).sort().map((g) => `<option value="${esc(g)}">${esc(g)}</option>`).join('');
    sel.value = cur;
  }

  // ------------------------------------------------------------- événements
  ['q', 'f-group', 'f-status'].forEach((id) => $(id).addEventListener('input', render));

  rowsEl.addEventListener('change', (e) => {
    const tr = e.target.closest('tr');
    if (!tr) return;
    const id = tr.dataset.id;
    if (e.target.classList.contains('sel')) {
      e.target.checked ? selected.add(id) : selected.delete(id);
      $('btn-assign').disabled = selected.size === 0;
    } else if (e.target.classList.contains('master')) {
      S.master[id] = e.target.value;
      save();
    }
  });

  $('check-all').addEventListener('change', (e) => {
    rowsEl.querySelectorAll('tr').forEach((tr) => {
      e.target.checked ? selected.add(tr.dataset.id) : selected.delete(tr.dataset.id);
    });
    render();
  });

  $('btn-group').addEventListener('click', () => {
    const name = prompt('Nom du groupe :');
    if (!name || !name.trim()) return;
    S.groups[name.trim()] = S.groups[name.trim()] || [];
    save(); refreshGroupSelect(); render();
  });

  $('btn-assign').addEventListener('click', () => {
    const names = Object.keys(S.groups).sort();
    if (!names.length) { alert('Crée d\'abord un groupe.'); return; }
    const name = prompt(`Groupe cible (${names.join(', ')}) — vide pour retirer du groupe :`);
    if (name === null) return;
    const target = name.trim();
    if (target && !S.groups[target]) S.groups[target] = [];
    selected.forEach((id) => {
      Object.values(S.groups).forEach((arr) => { const i = arr.indexOf(id); if (i >= 0) arr.splice(i, 1); });
      if (target) S.groups[target].push(id);
    });
    selected.clear();
    save(); refreshGroupSelect(); render();
  });

  $('btn-export').addEventListener('click', () => {
    const blob = new Blob([JSON.stringify(S, null, 2)], { type: 'application/json' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = 'portal6-music-groups.json';
    a.click();
    URL.revokeObjectURL(a.href);
  });

  $('import-file').addEventListener('change', (e) => {
    const f = e.target.files[0];
    if (!f) return;
    f.text().then((t) => {
      const data = JSON.parse(t);
      S.groups = data.groups || {};
      S.master = data.master || {};
      save(); refreshGroupSelect(); render();
    }).catch(() => alert('Fichier JSON invalide.'));
    e.target.value = '';
  });

  // ---------------------------------------------------------------- actions
  document.body.addEventListener('click', (e) => {
    const btn = e.target.closest('[data-action]');
    if (!btn) return;
    const tr = btn.closest('tr');
    const p = tr ? PL.find((x) => x.id === tr.dataset.id) : null;
    openPanel(btn.dataset.action, p);
  });

  function openPanel(action, p) {
    const title = $('panel-title');
    const body = $('panel-body');

    if (action === 'push' && p) {
      const master = S.master[p.id] || 'spotify';
      const targets = ['spotify', 'local', 'itunes'].filter((t) => t !== master);
      title.textContent = `Push « ${p.name} » — ${master} → ${targets.join(' + ')}`;
      body.innerHTML = `
        <p>${num(p.spotify)} titres sur Spotify, ${num(p.local)} avec un fichier local
           (${pct(p.local, p.spotify)} %).</p>
        <h4>Prérequis</h4>
        <ul class="checks">
          <li class="bad">IDs Spotify des titres — <strong>absents</strong> : l'export ne conserve
              que artiste/titre/album (<code>item_row()</code> dans <code>export_library.py</code>
              jette <code>track.id</code>). Ré-exporter avec les IDs → <code>platform_refs</code>.</li>
          <li class="${p.local >= p.spotify ? 'ok' : 'warn'}">Fichiers locaux —
              ${p.spotify - p.local > 0 ? `${num(p.spotify - p.local)} titres sans fichier` : 'complets'}.</li>
          <li class="bad">Bibliothèque iTunes — à créer (aucune référence connue).</li>
          <li class="warn">Résolution des conflits — à définir : que fait-on d'un titre
              présent sur la cible mais pas sur le maître ?</li>
        </ul>
        <h4>Ce que lancera le push (à venir)</h4>
        <pre>python plugin/etl/music/sync/push.py --playlist "${esc(p.id)}" --master ${master} --to ${targets.join(',')} --dry-run</pre>
        <p class="hint">Rien n'a été exécuté. Le script n'existe pas encore — voir
           <code>.docs/musique.md</code>, section « Modèle de sync ».</p>`;
    } else {
      return;
    }
    $('panel').showModal();
  }

  // ------------------------------------------------------------------- init
  refreshGroupSelect();
  render();
  $('generated').textContent = `Données arrêtées au ${D.generated} — base ${M.db.mtime || '?'} (antérieure à la quarantaine, à reconstruire).`;
})();
