/* Portal6 — rendu de la page Données (music-data.html) à partir de window.PORTAL6 (data/manifest.js). */
(function () {
  'use strict';

  const D = window.PORTAL6;
  if (!D || !D.music) {
    document.getElementById('missing').hidden = false;
    return;
  }
  document.getElementById('content').hidden = false;

  const M = D.music;
  const db = M.db || {};
  const scan = M.scan || {};
  const pre = M.scan_pre || {};
  const tbl = db.tables || {};

  const $ = (id) => document.getElementById(id);
  const num = (n) => (n == null ? '—' : n.toLocaleString('fr-FR'));
  const pct = (n, d) => (d ? Math.round((n / d) * 1000) / 10 : 0);
  const el = (tag, html) => { const e = document.createElement(tag); e.innerHTML = html; return e; };
  // Les descriptions du manifeste utilisent des `backticks` façon markdown.
  const md = (s) => String(s).replace(/`([^`]+)`/g, '<code>$1</code>');
  const bar = (p) => `<span class="bar"><i style="width:${Math.min(p, 100)}%"></i></span>`;

  function stats(target, items) {
    $(target).innerHTML = items.map((s) =>
      `<div class="stat ${s.tone || ''}"><div class="n">${s.n}</div><div class="l">${s.l}</div></div>`
    ).join('');
  }

  function rows(target, list) {
    const tb = $(target);
    tb.innerHTML = '';
    list.forEach((cells) => tb.appendChild(el('tr', cells.join(''))));
  }

  // ---------------------------------------------------------------- chiffres clés
  const spotifyTracks = db.enrichment_total || 0;
  const both = (db.cross && db.cross.both) || 0;

  stats('kpis', [
    { n: num(scan.files), l: 'fichiers audio scannés' },
    { n: scan.size_h || '—', l: 'de bibliothèque locale' },
    { n: num(spotifyTracks), l: 'titres Spotify extraits' },
    { n: num(tbl.playlists), l: 'playlists' },
    { n: num(both), l: `titres présents des deux côtés (${pct(both, spotifyTracks)} % du Spotify)`, tone: 'ok' },
    { n: num(scan.no_artist), l: `fichiers sans tag artiste (${pct(scan.no_artist, scan.files)} %)`, tone: 'bad' },
    { n: num(tbl.platform_refs), l: 'références plateforme (sync impossible)', tone: 'bad' },
    { n: M.exports && M.exports.length ? num(M.exports.length) : '0', l: 'fichier d\'historique d\'écoute', tone: 'warn' },
  ]);

  // ---------------------------------------------------------------------- alertes
  const alerts = [];

  if (db.mtime && scan.files != null && tbl.files !== scan.files) {
    alerts.push(['bad',
      `<p><strong>La base est périmée.</strong> <code>music.db</code> date du ${db.mtime}
       et contient ${num(tbl.files)} fichiers, alors que le scan courant en compte
       ${num(scan.files)}. Elle a été construite <em>avant</em> la quarantaine des doublons :
       tous les chiffres ci-dessous côté DB décrivent l'état d'avant.</p>
       <p>Correctif : relancer <code>build_db.py</code>.</p>`]);
  }

  const preFolders = new Map(pre.folders || []);
  const curFolders = new Map(scan.folders || []);
  const lost = [...preFolders].filter(([k]) => !curFolders.has(k));
  if (lost.length) {
    const total = lost.reduce((a, [, v]) => a + v, 0);
    alerts.push(['warn',
      `<p><strong>Le scan courant a rétréci le périmètre.</strong> Il ne couvre plus que
       <code>m:/music</code> : ${num(total)} fichiers présents dans le scan initial ont disparu
       de l'inventaire — ${lost.map(([k, v]) => `<code>m:/${k}</code> (${num(v)})`).join(', ')}.</p>
       <p>Ce ne sont pas des fichiers perdus sur le disque, mais des fichiers que la base
       ne connaîtra plus après rebuild. À trancher : les réintégrer (rescanner
       <code>/mnt/m</code>) ou acter qu'ils sont hors référentiel.</p>`]);
  }

  if (scan.no_artist && pct(scan.no_artist, scan.files) > 40) {
    alerts.push(['warn',
      `<p><strong>Un fichier sur deux n'a pas d'artiste taggué</strong>
       (${num(scan.no_artist)} sur ${num(scan.files)}). Le matching local ↔ Spotify se fait
       sur artiste + titre normalisés : c'est <em>la</em> raison du faible recoupement,
       pas un défaut de la bibliothèque.</p>`]);
  }

  if (M.fingerprint) {
    const f = M.fingerprint;
    alerts.push(['warn',
      `<p><strong>Fingerprint arrêté à ${pct(f.done, f.total)} %</strong>
       (${num(f.done)} / ${num(f.total)} fichiers). C'est la piste qui réparerait les tags
       manquants — le cache est conservé, la reprise coûte ~8 h de calcul.</p>`]);
  }

  $('alerts').innerHTML = alerts.map(([tone, html]) =>
    `<div class="note ${tone}" style="margin-bottom:12px">${html}</div>`).join('')
    || '<div class="note"><p>Rien à signaler.</p></div>';

  // ------------------------------------------------------------------------ vault
  const kindTone = { source: 'source', archive: '', 'dérivé': '', cache: '' };
  rows('vault', (M.vault || []).map((f) => [
    `<td><code>${f.name}</code></td>`,
    `<td>${md(f.desc)}</td>`,
    `<td><span class="pill ${kindTone[f.kind] || ''}">${f.kind}</span></td>`,
    `<td class="num">${f.present ? f.size_h : '<span class="pill bad">absent</span>'}</td>`,
    `<td class="num">${f.lines != null ? num(f.lines) : '—'}</td>`,
    `<td class="mono">${f.mtime || '—'}</td>`,
  ]));

  // ---------------------------------------------------------------------- dossiers
  rows('folders', (scan.folders || []).map(([name, n]) => [
    `<td><code>m:/${name}</code></td>`,
    `<td class="num">${num(n)}</td>`,
    `<td class="num">${pct(n, scan.files)} %</td>`,
    `<td>${bar(pct(n, scan.files))}</td>`,
  ]));

  // -------------------------------------------------------------- qualité des tags
  const tagged = scan.files - scan.no_tags;
  stats('tagq', [
    { n: `${pct(tagged, scan.files)} %`, l: `tags lus (${num(tagged)} fichiers)` },
    { n: `${pct(scan.files - scan.no_artist, scan.files)} %`, l: 'ont un artiste', tone: 'bad' },
    { n: `${pct(scan.files - scan.no_album, scan.files)} %`, l: 'ont un album', tone: 'warn' },
    { n: num(scan.no_tags), l: 'fichiers illisibles par mutagen' },
  ]);

  // ----------------------------------------------------------------------- formats
  rows('exts', (scan.extensions || []).map(([e, n]) => [
    `<td><code>${e}</code></td>`,
    `<td class="num">${num(n)}</td>`,
    `<td class="num">${pct(n, scan.files)} %</td>`,
  ]));

  // ------------------------------------------------------------------ scan vs scan
  const diff = (label, a, b, fmt) => {
    const d = (b || 0) - (a || 0);
    const f = fmt || num;
    return [
      `<td>${label}</td>`,
      `<td class="num">${f(a)}</td>`,
      `<td class="num">${f(b)}</td>`,
      `<td class="num ${d < 0 ? 'mono' : 'mono'}">${d > 0 ? '+' : ''}${num(d)}</td>`,
    ];
  };
  rows('scandiff', [
    diff('Fichiers', pre.files, scan.files),
    diff('Sans tag artiste', pre.no_artist, scan.no_artist),
    diff('Sans tags lus', pre.no_tags, scan.no_tags),
    [`<td>Volume</td><td class="num">${pre.size_h || '—'}</td><td class="num">${scan.size_h || '—'}</td>`,
     `<td class="num mono">${pre.size && scan.size ? '-' + Math.round((pre.size - scan.size) / 1e9) + ' Go' : '—'}</td>`],
  ]);

  // ------------------------------------------------------------------------ tables
  const tableDesc = {
    tracks: 'Titre canonique, dédupliqué par artiste+titre normalisés',
    files: 'Fichier physique sur disque, rattaché ou non à un titre',
    playlists: 'Playlists Spotify, avec thématique et genres dominants',
    playlist_tracks: 'Appartenance titre ↔ playlist',
    enrichment: 'Genres (Last.fm), pays (MusicBrainz), features audio (ReccoBeats)',
    platform_refs: 'IDs externes spotify / youtube / itunes — socle de la future sync',
  };
  rows('tables', Object.entries(tbl).map(([name, n]) => [
    `<td><code>${name}</code></td>`,
    `<td class="num">${num(n)}</td>`,
    `<td>${tableDesc[name] || ''}${n === 0 ? ' <span class="pill bad">vide</span>' : ''}</td>`,
  ]));

  // -------------------------------------------------------------------- recoupement
  const c = db.cross || {};
  stats('cross', [
    { n: num(c.both), l: 'titres Spotify avec un fichier local', tone: 'ok' },
    { n: num(c.spotify_only), l: 'titres Spotify sans fichier (à acheter / télécharger)', tone: 'warn' },
    { n: num(c.local_only), l: 'fichiers locaux hors Spotify (le gros du disque)' },
    { n: `${pct(c.both, spotifyTracks)} %`, l: 'du catalogue Spotify couvert en local' },
    { n: num(db.files_matched), l: 'fichiers rattachés à un titre' },
    { n: num(tbl.files - db.files_matched), l: 'fichiers orphelins', tone: 'warn' },
  ]);

  // ----------------------------------------------------------------- enrichissement
  const srcOf = {
    genres: 'Last.fm', country: 'MusicBrainz', mood: 'ReccoBeats', energy: 'ReccoBeats',
    valence: 'ReccoBeats', danceability: 'ReccoBeats', tempo: 'ReccoBeats',
    acousticness: 'ReccoBeats', instrumentalness: 'ReccoBeats', camelot: 'ReccoBeats',
  };
  rows('enrich', (db.enrichment || []).map(([col, n]) => {
    const p = pct(n, db.enrichment_total);
    return [
      `<td><code>${col}</code></td>`,
      `<td class="num">${num(n)}</td>`,
      `<td class="num">${p} %</td>`,
      `<td>${bar(p)}</td>`,
      `<td>${srcOf[col] || ''}</td>`,
    ];
  }));

  // --------------------------------------------------------------------- playlists
  rows('playlists', (db.top_playlists || []).map(([name, n]) => [
    `<td>${name}</td>`,
    `<td class="num">${num(n)}</td>`,
  ]));

  // ------------------------------------------------------------------------- trous
  rows('gaps', [
    [`<td>Historique d'écoute Spotify</td>`,
     `<td><span class="pill bad">absent</span> <code>exports/</code> est vide</td>`,
     `<td>Pas de classement par temps d'écoute → pas de liste d'achats vinyle/CD</td>`],
    [`<td>Références plateforme (<code>platform_refs</code>)</td>`,
     `<td><span class="pill bad">0 ligne</span></td>`,
     `<td>Aucune sync iTunes / Spotify / YouTube possible en l'état</td>`],
    [`<td>Features audio (ReccoBeats)</td>`,
     `<td><span class="pill warn">moisson interrompue</span></td>`,
     `<td>Pas de programmation éditoriale par énergie/humeur pour la web-radio</td>`],
    [`<td>Fingerprint acoustique</td>`,
     `<td><span class="pill warn">${M.fingerprint ? pct(M.fingerprint.done, M.fingerprint.total) : 0} %</span></td>`,
     `<td>Les fichiers sans tags restent non identifiables → non matchables</td>`],
    [`<td>Sauvegarde du vault <code>data/</code></td>`,
     `<td><span class="pill bad">aucune</span></td>`,
     `<td>Non versionné, présent sur cette seule machine — perte = tout à refaire</td>`],
  ]);

  $('generated').textContent = `Données arrêtées au ${D.generated}. `
    + `Base lue : plugin/db/music.db (${db.size_h || '?'}, ${db.mtime || '?'}).`;
})();
