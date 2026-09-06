/* Portal6 — vue de résolution des doublons (music-dedup.html), façon dupeGuru.
 *
 * Donnée : window.PORTAL6_DEDUP (data/dedup.js, non versionné). Décisions : P6Plan
 * (localStorage). Trois onglets : fichiers locaux, Spotify intra, Spotify inter.
 * Rien n'est exécuté ici — la validation produit un plan JSON pour les scripts.
 */
(function () {
  'use strict';

  const D = window.PORTAL6_DEDUP;
  if (!D) { document.getElementById('missing').hidden = false; return; }
  document.getElementById('content').hidden = false;

  const $ = (id) => document.getElementById(id);
  const { esc, num, human } = window.P6Plan;
  const PL_GROUPS = (() => { try { return JSON.parse(localStorage.getItem('portal6.music'))?.groups || {}; } catch { return {}; } })();
  const PL_BY_NAME = Object.fromEntries(((window.PORTAL6?.music?.db?.playlists) || []).map((p) => [p.name, p.id]));
  const groupOfPlaylist = (name) => Object.keys(PL_GROUPS).find((g) => PL_GROUPS[g].includes(PL_BY_NAME[name])) || '';
  const isArchive = (name) => /\((All|all)\)\s*$/.test(name) || /\b(19|20)\d\d\b/.test(name);

  // ------------------------------------------------------------------ plan
  const plan = window.P6Plan.mount({
    storeKey: 'portal6.plan.dedup',
    title: 'Dédup',
    filename: 'dedup_plan.json',
    kinds: {
      quarantine: { label: 'Déplacer vers _a_trier', tone: 'warn', help: 'Jamais de suppression directe : le fichier est déplacé, réversible.' },
      delete:     { label: 'Supprimer définitivement', tone: 'bad', help: 'Réservé aux copies déjà passées par la quarantaine.' },
      ignore:     { label: 'Ignorer (faux positif)', tone: '', help: 'Le groupe ne sera plus proposé.' },
      ref:        { label: 'Changer la référence', tone: '', help: 'La copie gardée n\'est pas celle que le script avait classée première.' },
      intra:      { label: 'Spotify — retirer les doublons intra', tone: 'warn', help: 'Garde une occurrence par playlist.' },
      inter:      { label: 'Spotify — retirer d\'une playlist', tone: 'warn', help: 'Le titre reste dans les playlists non barrées.' },
      spot_ignore:{ label: 'Spotify — ignorer', tone: '', help: '' },
    },
    command: (p) => {
      const hasLocal = p.items.some((i) => ['quarantine', 'delete', 'ignore', 'ref'].includes(i.kind));
      const hasSpot = p.items.some((i) => ['intra', 'inter', 'spot_ignore'].includes(i.kind));
      return [
        hasLocal && `python plugin/etl/music/local/apply_dedup_plan.py data/music/dedup_plan.${p.id}.json --dry-run`,
        hasSpot && `python plugin/etl/music/spotify/apply_dedup_plan.py data/music/dedup_plan.${p.id}.json --dry-run`,
      ].filter(Boolean).join('\n');
    },
    onChange: () => { renderLocal(); renderIntra(); renderInter(); },
  });

  // ----------------------------------------------------------------- onglets
  function showTab() {
    const tab = (location.hash || '#local').slice(1);
    document.querySelectorAll('#tabs a').forEach((a) => {
      const on = a.dataset.tab === tab;
      a.toggleAttribute('aria-current', on);
      $('pane-' + a.dataset.tab).hidden = !on;
    });
  }
  window.addEventListener('hashchange', showTab);

  // ================================================================== LOCAL
  const L = D.local || { groups: [] };
  const GROUPS = L.groups;
  // référence choisie par groupe (par défaut : celle du script)
  const refState = (() => { try { return JSON.parse(localStorage.getItem('portal6.dedup.refs')) || {}; } catch { return {}; } })();
  const saveRefs = () => { try { localStorage.setItem('portal6.dedup.refs', JSON.stringify(refState)); } catch { /* */ } };
  const refOf = (g) => refState[g.id] || g.items.find((i) => i.ref)?.id;
  const topOf = (folder) => folder.split('/').slice(0, 2).join('/');

  $('n-local').textContent = num(GROUPS.length);
  const dupItems = GROUPS.reduce((a, g) => a + g.items.length - 1, 0);
  const dupBytes = GROUPS.reduce((a, g) => a + g.items.filter((i) => i.id !== refOf(g)).reduce((b, i) => b + i.size, 0), 0);

  $('local-banner').innerHTML = L.applied
    ? `<div class="note warn"><p><strong>Ce rapport a déjà été appliqué</strong> (${esc(L.mtime)}) :
         les copies non-référence sont dans <code>M:\\_a_trier</code>, et le rescan du 2026-08-20 23:36
         n'a plus trouvé de doublon. Les décisions prises ici visent donc <em>_a_trier</em> :
         <strong>supprimer</strong> = purger, <strong>ignorer</strong> = restaurer (faux positif).
         Un nouveau scan remplacera ce rapport.</p></div>`
    : `<div class="note"><p>Rapport courant : <code>${esc(L.source)}</code> (${esc(L.mtime)}).</p></div>`;

  $('local-kpis').innerHTML = [
    { n: num(GROUPS.length), l: 'groupes de doublons' },
    { n: num(dupItems), l: 'copies en trop' },
    { n: human(dupBytes), l: 'récupérables', tone: 'ok' },
    { n: num(GROUPS.filter((g) => new Set(g.items.map((i) => i.folder)).size === 1).length), l: 'groupes dans un même dossier' },
    { n: num(GROUPS.filter((g) => g.items.find((i) => i.id === refOf(g))?.folder.startsWith('music/workspace')).length), l: 'références dans le workspace', tone: 'warn' },
  ].map((s) => `<div class="stat ${s.tone || ''}"><div class="n">${s.n}</div><div class="l">${s.l}</div></div>`).join('');

  const PAGE = 150;
  let lShown = PAGE;

  function localFiltered() {
    const q = $('l-q').value.trim().toLowerCase();
    const f = $('l-f').value;
    return GROUPS.filter((g) => {
      if (q && !(g.artist + ' ' + g.title + ' ' + g.items.map((i) => i.file).join(' ')).toLowerCase().includes(q)) return false;
      const marked = g.items.some((i) => plan.get(i.id)) || plan.get('ref:' + g.id);
      if (f === 'marked' && !marked) return false;
      if (f === 'unmarked' && marked) return false;
      if (f === 'samefolder' && new Set(g.items.map((i) => i.folder)).size !== 1) return false;
      if (f === 'cross' && new Set(g.items.map((i) => topOf(i.folder))).size < 2) return false;
      if (f === 'refws' && !g.items.find((i) => i.id === refOf(g))?.folder.startsWith('music/workspace')) return false;
      return true;
    });
  }

  function renderLocal() {
    const list = localFiltered();
    const shown = list.slice(0, lShown);
    $('l-rows').innerHTML = shown.map((g) => {
      const ref = refOf(g);
      const refItem = g.items.find((i) => i.id === ref);
      const extra = g.items.filter((i) => i.id !== ref).reduce((b, i) => b + i.size, 0);
      const ignored = g.items.some((i) => plan.get(i.id)?.kind === 'ignore');
      return `<tr class="grp" data-g="${esc(g.id)}"><td colspan="7">
          ${esc(g.artist)} — ${esc(g.title)}
          <span class="meta">${g.items.length} fichiers · ${human(extra)} en trop
            ${ignored ? '· <span class="pill">ignoré</span>' : ''}
            ${refItem && !refItem.ref ? '· <span class="pill">référence modifiée</span>' : ''}</span>
        </td></tr>`
        + g.items.map((i) => {
          const d = plan.get(i.id);
          const isRef = i.id === ref;
          const cls = ['item', isRef ? 'ref' : '', d ? 'marked' : '', d?.kind === 'delete' ? 'del' : '', d?.kind === 'ignore' ? 'ignored' : ''].join(' ');
          return `<tr class="${cls}" data-g="${esc(g.id)}" data-i="${esc(i.id)}">
            <td><input type="radio" name="ref-${esc(g.id)}" ${isRef ? 'checked' : ''} title="Définir comme référence"></td>
            <td class="file mono" title="${esc(i.path)}">${esc(i.file)}</td>
            <td class="folder" title="${esc(i.folder)}">${esc(i.folder)}</td>
            <td class="num">${i.bitrate ?? '—'}</td>
            <td class="num">${human(i.size)}</td>
            <td><code>${esc(i.ext)}</code></td>
            <td>${isRef ? '<span class="muted">référence</span>' : `
              <select class="act" aria-label="Action">
                <option value="" ${!d ? 'selected' : ''}>—</option>
                <option value="quarantine" ${d?.kind === 'quarantine' ? 'selected' : ''}>Quarantaine</option>
                <option value="delete" ${d?.kind === 'delete' ? 'selected' : ''}>Supprimer</option>
                <option value="ignore" ${d?.kind === 'ignore' ? 'selected' : ''}>Ignorer</option>
              </select>`}</td>
          </tr>`;
        }).join('');
    }).join('');
    $('l-more').innerHTML = list.length > lShown
      ? `<button class="btn ghost small" id="l-more-btn">Afficher ${num(Math.min(PAGE, list.length - lShown))} groupes de plus (${num(list.length - lShown)} restants)</button>`
      : `<span class="muted">${num(list.length)} groupe${list.length > 1 ? 's' : ''} affiché${list.length > 1 ? 's' : ''}</span>`;
  }

  function setLocal(g, item, kind) {
    if (!kind) { plan.clear(item.id); return; }
    plan.set(item.id, kind, {
      label: `${g.artist} — ${g.title} · ${item.file}`,
      detail: item.path,
      meta: { path: item.path, group: g.id, bytes: kind === 'ignore' ? 0 : item.size },
    });
  }

  $('l-rows').addEventListener('change', (e) => {
    const tr = e.target.closest('tr'); if (!tr) return;
    const g = GROUPS.find((x) => x.id === tr.dataset.g);
    const item = g.items.find((x) => x.id === tr.dataset.i);
    if (e.target.classList.contains('act')) {
      setLocal(g, item, e.target.value);
    } else if (e.target.type === 'radio') {
      refState[g.id] = item.id; saveRefs();
      plan.clear(item.id);
      if (item.ref) plan.clear('ref:' + g.id);
      else plan.set('ref:' + g.id, 'ref', { label: `${g.artist} — ${g.title} → ${item.file}`, detail: item.path, meta: { path: item.path, group: g.id } });
    }
    renderLocal();
  });
  $('l-more').addEventListener('click', (e) => { if (e.target.id === 'l-more-btn') { lShown += PAGE; renderLocal(); } });
  ['l-q', 'l-f'].forEach((id) => $(id).addEventListener('input', () => { lShown = PAGE; renderLocal(); }));

  document.querySelectorAll('[data-preset]').forEach((b) => b.addEventListener('click', () => {
    const list = localFiltered();
    plan.bulk((pending) => {
      list.forEach((g) => {
        const ref = refOf(g);
        g.items.filter((i) => i.id !== ref).forEach((i) => {
          if (b.dataset.preset === 'none') { delete pending[i.id]; return; }
          if (b.dataset.preset === 'ws' && !i.folder.startsWith('music/workspace')) return;
          pending[i.id] = { kind: 'quarantine', label: `${g.artist} — ${g.title} · ${i.file}`, detail: i.path, meta: { path: i.path, group: g.id, bytes: i.size }, at: Date.now() };
        });
      });
    });
    renderLocal();
  }));

  // ========================================================== SPOTIFY INTRA
  const INTRA = (D.spotify && D.spotify.intra) || [];
  $('n-intra').textContent = num(INTRA.length);
  const plSel = $('i-pl');
  [...new Set(INTRA.map((r) => r.playlists[0]))].sort().forEach((p) => plSel.insertAdjacentHTML('beforeend', `<option>${esc(p)}</option>`));

  function intraFiltered() {
    const q = $('i-q').value.trim().toLowerCase();
    const p = plSel.value;
    return INTRA.filter((r) => (!p || r.playlists[0] === p)
      && (!q || (r.playlists.join(' ') + ' ' + r.artist + ' ' + r.track).toLowerCase().includes(q)));
  }
  function renderIntra() {
    $('i-rows').innerHTML = intraFiltered().map((r) => {
      const d = plan.get(r.id);
      return `<tr class="item ${d ? 'marked' : ''} ${d?.kind === 'spot_ignore' ? 'ignored' : ''}" data-i="${esc(r.id)}">
        <td>${esc(r.playlists[0])}</td><td>${esc(r.artist)}</td><td>${esc(r.track)}</td>
        <td class="num">${r.occurrences}</td>
        <td><div class="seg">
          <button type="button" data-k="intra" class="${d?.kind === 'intra' ? 'on warn' : ''}">Dédoublonner</button>
          <button type="button" data-k="spot_ignore" class="${d?.kind === 'spot_ignore' ? 'on ok' : ''}">Ignorer</button>
        </div></td></tr>`;
    }).join('');
  }
  $('i-rows').addEventListener('click', (e) => {
    const b = e.target.closest('button[data-k]'); if (!b) return;
    const r = INTRA.find((x) => x.id === b.closest('tr').dataset.i);
    if (plan.get(r.id)?.kind === b.dataset.k) plan.clear(r.id);
    else plan.set(r.id, b.dataset.k, { label: `${r.playlists[0]} · ${r.artist} — ${r.track}`, detail: `${r.occurrences} occurrences → 1`, meta: { playlist: r.playlists[0], artist: r.artist, track: r.track } });
    renderIntra();
  });
  ['i-q', 'i-pl'].forEach((id) => $(id).addEventListener('input', renderIntra));
  document.querySelectorAll('[data-ipreset]').forEach((b) => b.addEventListener('click', () => {
    const list = intraFiltered();
    plan.bulk((pending) => list.forEach((r) => {
      if (b.dataset.ipreset === 'none') delete pending[r.id];
      else pending[r.id] = { kind: 'intra', label: `${r.playlists[0]} · ${r.artist} — ${r.track}`, detail: `${r.occurrences} occurrences → 1`, meta: { playlist: r.playlists[0], artist: r.artist, track: r.track }, at: Date.now() };
    }));
    renderIntra();
  }));

  // ========================================================== SPOTIFY INTER
  const INTER = (D.spotify && D.spotify.inter) || [];
  $('n-inter').textContent = num(INTER.length);
  const xg = $('x-group');
  Object.keys(PL_GROUPS).sort().forEach((g) => xg.insertAdjacentHTML('beforeend', `<option>${esc(g)}</option>`));
  let xShown = PAGE;

  const livePlaylists = (r) => $('x-arch').checked ? r.playlists.filter((p) => !isArchive(p)) : r.playlists;

  function interFiltered() {
    const q = $('x-q').value.trim().toLowerCase();
    const min = +$('x-min').value;
    const grp = xg.value;
    return INTER.filter((r) => {
      const live = livePlaylists(r);
      if (live.length < min) return false;
      if (grp && !live.some((p) => groupOfPlaylist(p) === grp)) return false;
      if (q && !(r.artist + ' ' + r.track + ' ' + r.playlists.join(' ')).toLowerCase().includes(q)) return false;
      return true;
    });
  }
  function renderInter() {
    const list = interFiltered();
    $('x-rows').innerHTML = list.slice(0, xShown).map((r) => {
      const d = plan.get(r.id);
      const dropped = new Set(d?.kind === 'inter' ? d.meta.remove : []);
      return `<tr class="item ${d ? 'marked' : ''} ${d?.kind === 'spot_ignore' ? 'ignored' : ''}" data-i="${esc(r.id)}">
        <td><strong>${esc(r.artist)}</strong><div class="sub">${esc(r.track)}</div></td>
        <td>${r.playlists.map((p) => {
          const arch = isArchive(p) && $('x-arch').checked;
          const g = groupOfPlaylist(p);
          return `<span class="chip ${arch ? 'archive' : dropped.has(p) ? 'drop' : 'keep'}" data-p="${esc(p)}" ${arch ? 'title="archive annuelle — non touchée"' : ''}>${esc(p)}${g ? `<span class="g">${esc(g)}</span>` : ''}</span>`;
        }).join('')}</td>
        <td class="num">${r.occurrences}</td>
        <td><div class="seg">
          <button type="button" data-k="spot_ignore" class="${d?.kind === 'spot_ignore' ? 'on ok' : ''}">Ignorer</button>
        </div></td></tr>`;
    }).join('');
    $('x-more').innerHTML = list.length > xShown
      ? `<button class="btn ghost small" id="x-more-btn">Afficher ${num(Math.min(PAGE, list.length - xShown))} titres de plus (${num(list.length - xShown)} restants)</button>`
      : `<span class="muted">${num(list.length)} titre${list.length > 1 ? 's' : ''} affiché${list.length > 1 ? 's' : ''}</span>`;
  }
  function setInter(r, remove) {
    if (!remove.length) { plan.clear(r.id); return; }
    const keep = r.playlists.filter((p) => !remove.includes(p));
    plan.set(r.id, 'inter', {
      label: `${r.artist} — ${r.track}`,
      detail: `retirer de : ${remove.join(', ')} · garder : ${keep.join(', ')}`,
      meta: { artist: r.artist, track: r.track, remove, keep },
    });
  }
  $('x-rows').addEventListener('click', (e) => {
    const tr = e.target.closest('tr'); if (!tr) return;
    const r = INTER.find((x) => x.id === tr.dataset.i);
    const chip = e.target.closest('.chip');
    if (chip && !chip.classList.contains('archive')) {
      const d = plan.get(r.id);
      const cur = new Set(d?.kind === 'inter' ? d.meta.remove : []);
      cur.has(chip.dataset.p) ? cur.delete(chip.dataset.p) : cur.add(chip.dataset.p);
      setInter(r, [...cur]);
    } else if (e.target.closest('button[data-k]')) {
      if (plan.get(r.id)?.kind === 'spot_ignore') plan.clear(r.id);
      else plan.set(r.id, 'spot_ignore', { label: `${r.artist} — ${r.track}`, detail: r.playlists.join(', '), meta: { artist: r.artist, track: r.track } });
    } else return;
    renderInter();
  });
  $('x-more').addEventListener('click', (e) => { if (e.target.id === 'x-more-btn') { xShown += PAGE; renderInter(); } });
  ['x-q', 'x-min', 'x-group', 'x-arch'].forEach((id) => $(id).addEventListener('input', () => { xShown = PAGE; renderInter(); }));
  document.querySelectorAll('[data-xpreset]').forEach((b) => b.addEventListener('click', () => {
    const list = interFiltered();
    plan.bulk((pending) => list.forEach((r) => {
      if (b.dataset.xpreset === 'none') { delete pending[r.id]; return; }
      const live = livePlaylists(r);
      const remove = live.slice(1);
      if (!remove.length) return;
      const keep = r.playlists.filter((p) => !remove.includes(p));
      pending[r.id] = { kind: 'inter', label: `${r.artist} — ${r.track}`, detail: `retirer de : ${remove.join(', ')} · garder : ${keep.join(', ')}`, meta: { artist: r.artist, track: r.track, remove, keep }, at: Date.now() };
    }));
    renderInter();
  }));

  // ------------------------------------------------------------------- init
  showTab(); renderLocal(); renderIntra(); renderInter();
  $('generated').textContent = `Détail généré le ${D.generated} · rapport local ${L.source || '—'} · doublons Spotify : extract_spotify.xlsx (onglet « doublons »).`;
})();
