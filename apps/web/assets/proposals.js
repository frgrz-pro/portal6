/* Portal6 — propositions de playlists (music-proposals.html).
 *
 * Donnée : window.PORTAL6_PROPOSALS (data/proposals.js, non versionné) = onglets
 * « Monolithes » / « Monolithes détail » / « Cohérence » du Sheet Spotify.
 * Décisions : P6Plan. Les rejets et renommages sont un état d'écran (localStorage),
 * pas des actions du plan — le plan ne contient que ce qu'il y a à faire.
 */
(function () {
  'use strict';

  const D = window.PORTAL6_PROPOSALS;
  if (!D) { document.getElementById('missing').hidden = false; return; }
  document.getElementById('content').hidden = false;

  const $ = (id) => document.getElementById(id);
  const { esc, num } = window.P6Plan;
  const ALL_PLAYLISTS = ((window.PORTAL6?.music?.db?.playlists) || []).map((p) => p.name).sort((a, b) => a.localeCompare(b, 'fr'));

  const UI_KEY = 'portal6.proposals.ui';
  const UI = (() => { try { return JSON.parse(localStorage.getItem(UI_KEY)) || {}; } catch { return {}; } })();
  UI.rejected = UI.rejected || {};   // subId → true
  UI.names = UI.names || {};         // subId → nom renommé
  UI.open = UI.open || {};           // origin → true (carte dépliée)
  const saveUI = () => { try { localStorage.setItem(UI_KEY, JSON.stringify(UI)); } catch { /* */ } };

  const plan = window.P6Plan.mount({
    storeKey: 'portal6.plan.playlists',
    title: 'Playlists',
    filename: 'playlist_plan.json',
    kinds: {
      create: { label: 'Créer une playlist', tone: 'ok', help: 'Nouvelle playlist Spotify remplie avec les titres listés ; la playlist d\'origine n\'est pas touchée.' },
      move:   { label: 'Déplacer un titre', tone: 'warn', help: 'Retiré de la playlist d\'origine, ajouté à la cible.' },
      remove: { label: 'Retirer un titre', tone: 'bad', help: 'Retiré sans destination.' },
    },
    command: (p) => `python plugin/etl/music/spotify/apply_playlist_plan.py data/music/playlist_plan.${p.id}.json --dry-run`,
    onChange: () => { renderSplits(); renderMoves(); },
  });

  function showTab() {
    const tab = (location.hash || '#splits').slice(1);
    document.querySelectorAll('#tabs a').forEach((a) => {
      const on = a.dataset.tab === tab;
      a.toggleAttribute('aria-current', on);
      $('pane-' + a.dataset.tab).hidden = !on;
    });
  }
  window.addEventListener('hashchange', showTab);

  // ================================================================= SPLITS
  const SPLITS = D.splits || [];
  const allSubs = SPLITS.flatMap((s) => s.subs);
  $('n-splits').textContent = num(allSubs.length);

  const nameOf = (sub) => UI.names[sub.id] || sub.name;
  const stateOf = (sub) => (plan.get(sub.id) ? 'accepted' : UI.rejected[sub.id] ? 'rejected' : 'pending');

  function accept(sub, origin) {
    delete UI.rejected[sub.id]; saveUI();
    plan.set(sub.id, 'create', {
      label: nameOf(sub),
      detail: `${sub.tracks.length} titres · ${sub.minutes} min · depuis « ${origin.origin} »`,
      meta: { origin: origin.origin, name: nameOf(sub), theme: sub.theme, tracks: sub.tracks.map((t) => ({ artist: t.artist, track: t.track, album: t.album })) },
    });
  }

  function renderKpis() {
    const acc = allSubs.filter((s) => stateOf(s) === 'accepted').length;
    const rej = allSubs.filter((s) => stateOf(s) === 'rejected').length;
    $('s-kpis').innerHTML = [
      { n: num(SPLITS.length), l: 'playlists monolithiques (≥ 120 titres)' },
      { n: num(allSubs.length), l: 'sous-playlists proposées' },
      { n: num(acc), l: 'acceptées', tone: 'ok' },
      { n: num(rej), l: 'rejetées', tone: 'bad' },
      { n: num(allSubs.length - acc - rej), l: 'à décider', tone: 'warn' },
    ].map((s) => `<div class="stat ${s.tone || ''}"><div class="n">${s.n}</div><div class="l">${s.l}</div></div>`).join('');
  }

  function splitsFiltered() {
    const q = $('s-q').value.trim().toLowerCase();
    const f = $('s-f').value;
    return SPLITS.map((o) => ({
      ...o,
      subs: o.subs.filter((s) => (!f || stateOf(s) === f)
        && (!q || (o.origin + ' ' + nameOf(s) + ' ' + s.theme + ' ' + s.stars).toLowerCase().includes(q))),
    })).filter((o) => o.subs.length);
  }

  function renderSplits() {
    renderKpis();
    const list = splitsFiltered();
    $('s-list').innerHTML = list.map((o) => {
      const acc = o.subs.filter((s) => stateOf(s) === 'accepted').length;
      const open = UI.open[o.origin] !== false;
      return `<article class="prop" data-o="${esc(o.origin)}">
        <header>
          <h3>${esc(o.origin)}</h3>
          <span class="meta">${num(o.tracks)} titres → ${o.subs.length} volumes · ${num(o.subs.reduce((a, s) => a + s.minutes, 0))} min</span>
          <span class="pill ${acc ? 'ok' : ''}">${acc} / ${o.subs.length} acceptées</span>
          <button type="button" class="btn small ghost" data-oact="accept">Tout accepter</button>
          <span class="muted">${open ? '▾' : '▸'}</span>
        </header>
        <div class="subs" ${open ? '' : 'hidden'}>
          ${o.subs.map((s) => {
            const st = stateOf(s);
            return `<div class="sub ${st}" data-s="${esc(s.id)}">
              <div class="seg">
                <button type="button" data-sact="accept" class="${st === 'accepted' ? 'on ok' : ''}">Créer</button>
                <button type="button" data-sact="reject" class="${st === 'rejected' ? 'on bad' : ''}">Rejeter</button>
              </div>
              <input type="text" value="${esc(nameOf(s))}" aria-label="Nom de la playlist" ${st === 'rejected' ? 'disabled' : ''}>
              <span class="meta">${esc(s.theme)} · ${s.tracks.length} titres · ${s.minutes} min</span>
              <button type="button" class="btn small ghost" data-sact="tracks">titres</button>
              <div class="tracks" hidden>${s.tracks.map((t) => `<div>${esc(t.artist)} — ${esc(t.track)}${t.energy != null ? ` <span class="muted">(${t.energy})</span>` : ''}</div>`).join('')}
                <div class="muted" style="margin-top:6px">artistes phares : ${esc(s.stars)}</div></div>
            </div>`;
          }).join('')}
        </div>
      </article>`;
    }).join('') || '<p class="hint">Rien ne correspond au filtre.</p>';
  }

  $('s-list').addEventListener('click', (e) => {
    const art = e.target.closest('article.prop'); if (!art) return;
    const o = SPLITS.find((x) => x.origin === art.dataset.o);
    const subEl = e.target.closest('.sub');
    const sub = subEl ? o.subs.find((x) => x.id === subEl.dataset.s) : null;
    const b = e.target.closest('button');
    if (b?.dataset.oact === 'accept') { o.subs.forEach((s) => accept(s, o)); renderSplits(); return; }
    if (b?.dataset.sact === 'accept') { stateOf(sub) === 'accepted' ? plan.clear(sub.id) : accept(sub, o); renderSplits(); return; }
    if (b?.dataset.sact === 'reject') {
      if (UI.rejected[sub.id]) delete UI.rejected[sub.id]; else { UI.rejected[sub.id] = true; plan.clear(sub.id); }
      saveUI(); renderSplits(); return;
    }
    if (b?.dataset.sact === 'tracks') { const t = subEl.querySelector('.tracks'); t.hidden = !t.hidden; return; }
    if (e.target.closest('header') && !b) {
      UI.open[o.origin] = !(UI.open[o.origin] !== false); saveUI(); renderSplits();
    }
  });
  $('s-list').addEventListener('change', (e) => {
    if (e.target.type !== 'text') return;
    const subEl = e.target.closest('.sub');
    const o = SPLITS.find((x) => x.origin === subEl.closest('article').dataset.o);
    const sub = o.subs.find((x) => x.id === subEl.dataset.s);
    const v = e.target.value.trim();
    if (v && v !== sub.name) UI.names[sub.id] = v; else delete UI.names[sub.id];
    saveUI();
    if (plan.get(sub.id)) accept(sub, o);   // met à jour le libellé dans le plan
  });
  ['s-q', 's-f'].forEach((id) => $(id).addEventListener('input', renderSplits));
  document.querySelectorAll('[data-spreset]').forEach((b) => b.addEventListener('click', () => {
    splitsFiltered().forEach((o) => o.subs.forEach((s) => {
      const orig = SPLITS.find((x) => x.origin === o.origin);
      if (b.dataset.spreset === 'accept') accept(s, orig);
      else { plan.clear(s.id); delete UI.rejected[s.id]; }
    }));
    saveUI(); renderSplits();
  }));

  // ================================================================== MOVES
  const MOVES = D.moves || [];
  $('n-moves').textContent = num(MOVES.length);
  [...new Set(MOVES.map((m) => m.playlist))].sort().forEach((p) => $('m-pl').insertAdjacentHTML('beforeend', `<option>${esc(p)}</option>`));
  [...new Set(MOVES.map((m) => m.gap))].sort().forEach((g) => $('m-gap').insertAdjacentHTML('beforeend', `<option>${esc(g)}</option>`));
  // Une seule datalist partagée : 366 <select> de 110 options rendaient la page inutilisable.
  $('pl-list').innerHTML = ALL_PLAYLISTS.map((p) => `<option value="${esc(p)}"></option>`).join('');

  const targetOf = (m) => plan.get(m.id)?.meta?.to || m.to;

  function movesFiltered() {
    const q = $('m-q').value.trim().toLowerCase();
    const p = $('m-pl').value; const g = $('m-gap').value; const reco = $('m-reco').checked;
    return MOVES.filter((m) => (!p || m.playlist === p) && (!g || m.gap === g) && (!reco || m.to)
      && (!q || (m.playlist + ' ' + m.artist + ' ' + m.track + ' ' + m.to).toLowerCase().includes(q)));
  }
  function setMove(m, kind, to) {
    if (!kind) { plan.clear(m.id); return; }
    plan.set(m.id, kind, {
      label: `${m.artist} — ${m.track}`,
      detail: kind === 'move' ? `« ${m.playlist} » → « ${to} »` : `retirer de « ${m.playlist} »`,
      meta: { playlist: m.playlist, artist: m.artist, track: m.track, to: kind === 'move' ? to : null, why: m.why },
    });
  }
  function renderMoves() {
    $('m-rows').innerHTML = movesFiltered().map((m) => {
      const d = plan.get(m.id);
      const to = targetOf(m);
      const gapTone = { genre: 'warn', energy: 'source', valence: 'source', tempo: 'source' }[m.gap] || '';
      return `<tr class="item ${d ? 'marked' : ''} ${d?.kind === 'remove' ? 'del' : ''}" data-i="${esc(m.id)}">
        <td>${esc(m.playlist)}</td>
        <td><strong>${esc(m.artist)}</strong><div class="sub">${esc(m.track)}</div></td>
        <td><span class="pill ${gapTone}">${esc(m.gap)}</span><div class="sub">${esc(m.why)}</div></td>
        <td><input type="text" class="to" list="pl-list" value="${esc(to || '')}" placeholder="— aucune —" aria-label="Playlist cible">
            ${m.to ? `<div class="sub">reco : ${esc(m.to)}</div>` : ''}</td>
        <td><div class="seg">
          <button type="button" data-k="move" class="${d?.kind === 'move' ? 'on warn' : ''}" ${to ? '' : 'disabled'}>Déplacer</button>
          <button type="button" data-k="remove" class="${d?.kind === 'remove' ? 'on bad' : ''}">Retirer</button>
        </div></td></tr>`;
    }).join('');
  }
  $('m-rows').addEventListener('click', (e) => {
    const b = e.target.closest('button[data-k]'); if (!b) return;
    const tr = b.closest('tr'); const m = MOVES.find((x) => x.id === tr.dataset.i);
    const to = tr.querySelector('input.to').value.trim();
    if (plan.get(m.id)?.kind === b.dataset.k) plan.clear(m.id); else setMove(m, b.dataset.k, to);
    renderMoves();
  });
  $('m-rows').addEventListener('change', (e) => {
    if (!e.target.classList.contains('to')) return;
    const tr = e.target.closest('tr'); const m = MOVES.find((x) => x.id === tr.dataset.i);
    const d = plan.get(m.id);
    if (d?.kind === 'move') setMove(m, 'move', e.target.value);
    else if (!d && e.target.value) setMove(m, 'move', e.target.value);
    else if (d?.kind === 'move' && !e.target.value) plan.clear(m.id);
    renderMoves();
  });
  ['m-q', 'm-pl', 'm-gap', 'm-reco'].forEach((id) => $(id).addEventListener('input', renderMoves));
  document.querySelectorAll('[data-mpreset]').forEach((b) => b.addEventListener('click', () => {
    movesFiltered().forEach((m) => {
      if (b.dataset.mpreset === 'none') plan.clear(m.id);
      else if (m.to) setMove(m, 'move', m.to);
    });
    renderMoves();
  }));

  // ------------------------------------------------------------------- init
  showTab(); renderSplits(); renderMoves();
  $('generated').textContent = `Propositions générées le ${D.generated} depuis extract_spotify.xlsx (onglets Monolithes, Monolithes détail, Cohérence — analyses de juillet 2026).`;
})();
