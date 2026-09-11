/* Food — front de l'app. Un seul fichier, pas de dépendance.
   Tout l'état vient de GET /api/state ; chaque écriture renvoie le nouvel état. */
(() => {
  'use strict';

  const $ = (sel, root = document) => root.querySelector(sel);
  const el = (tag, attrs = {}, ...kids) => {
    const n = document.createElement(tag);
    for (const [k, v] of Object.entries(attrs)) {
      if (k === 'class') n.className = v;
      else if (k === 'html') n.innerHTML = v;
      else if (k.startsWith('on')) n.addEventListener(k.slice(2), v);
      else if (v !== null && v !== undefined && v !== false) n.setAttribute(k, v === true ? '' : v);
    }
    for (const k of kids.flat()) if (k !== null && k !== undefined) n.append(k.nodeType ? k : String(k));
    return n;
  };

  const MOMENTS = { matin: 'Matin', midi: 'Midi', gouter: 'Goûter', soir: 'Soir', apero: 'Apéro' };
  const CATS = { frigo: '🧊 Frigo', frais: '🥬 Frais', congelo: '❄️ Congélo', placard: '🏺 Placard' };
  const SKIP_DAYS = 3;

  // --- état ------------------------------------------------------------------
  let S = null;                       // état serveur
  let ING = {};                       // id → ingrédient
  let tab = 'ideas';
  let momentOverride = 'auto';        // auto | matin | midi | gouter | soir | apero | tout
  let persons = +(localStorage.getItem('food.persons') || 2);
  const skippedLeftovers = new Set(); // restes ignorés pour cette session
  let deckKeys = [];                  // clés des cartes du deck courant

  // --- utilitaires -------------------------------------------------------------
  const FR = { 0.25: '¼', 0.5: '½', 0.75: '¾' };
  function fmtQty(q, unit) {
    if (unit === 'g' || unit === 'ml') return `${Math.round(q)} ${unit}`;
    const i = Math.floor(q), f = Math.round((q - i) * 4) / 4;
    let s = i ? String(i) : '';
    if (f) s += FR[f] || String(f).slice(1);
    if (!s) s = String(Math.round(q * 100) / 100);
    if (unit === 'pièce') return s;
    return `${s} ${unit}`;
  }
  const plural = (n, s, p) => `${n} ${n > 1 ? p : s}`;
  function autoMoment() {
    const h = new Date().getHours();
    if (h >= 6 && h <= 10) return 'matin';
    if (h >= 11 && h <= 14) return 'midi';
    if (h >= 15 && h <= 17) return 'gouter';
    if (h >= 18 && h <= 22) return 'soir';
    return 'tout';
  }
  const curMoment = () => (momentOverride === 'auto' ? autoMoment() : momentOverride);

  /** Couverture d'une recette pour N personnes : ratio moyen, liste des manques. */
  function coverage(recipe, n) {
    const rows = recipe.ingredients.map(({ ingredient_id, qty }) => {
      const ing = ING[ingredient_id] || { name: ingredient_id, unit: '' };
      const need = qty * n, have = S.stock[ingredient_id] || 0;
      const ratio = need > 0 ? Math.min(1, have / need) : 1;
      return { ing, need, have, ratio, ok: have + 1e-9 >= need, lack: Math.max(0, need - have) };
    });
    const ratio = rows.length ? rows.reduce((a, r) => a + r.ratio, 0) / rows.length : 1;
    const missing = rows.filter(r => !r.ok);
    return { rows, ratio, missing, full: missing.length === 0 };
  }

  function toast(msg) {
    const t = $('#toast');
    t.textContent = msg; t.classList.add('show');
    clearTimeout(t._h); t._h = setTimeout(() => t.classList.remove('show'), 1800);
  }

  // --- API ---------------------------------------------------------------------
  async function api(method, path, body) {
    const r = await fetch(path, {
      method, headers: body ? { 'Content-Type': 'application/json' } : {},
      body: body ? JSON.stringify(body) : undefined,
    });
    const data = await r.json().catch(() => ({}));
    if (!r.ok) { toast(`Erreur : ${data.error || r.status}`); throw new Error(data.error || r.status); }
    if (data.ingredients) setState(data);
    return data;
  }
  function setState(s) {
    S = s; ING = Object.fromEntries(s.ingredients.map(i => [i.id, i]));
    render();
  }

  // --- rendu global --------------------------------------------------------------
  function render() {
    if (!S) return;
    const y = window.scrollY; // le re-rendu vide puis remplit les listes : on garde la position
    $('#persons').value = persons; $('#persons-n').textContent = persons;
    const liked = S.swipes.filter(s => s.verdict === 'like').length + S.leftovers.length;
    const n = $('#liked-n'); n.hidden = !liked; n.textContent = liked;
    for (const b of document.querySelectorAll('nav.tabs button')) b.toggleAttribute('aria-current', b.dataset.tab === tab);
    for (const s of document.querySelectorAll('.tab')) s.hidden = s.id !== `tab-${tab}`;
    if (tab === 'ideas') renderIdeas();
    if (tab === 'liked') renderLiked();
    if (tab === 'stock') renderStock();
    window.scrollTo(0, y);
  }

  // --- onglet Idées (deck) -------------------------------------------------------
  function renderMoments() {
    const box = $('#moments'); box.replaceChildren();
    const auto = autoMoment();
    const items = [['auto', `Auto · ${auto === 'tout' ? 'tout' : MOMENTS[auto]}`], ...Object.entries(MOMENTS), ['tout', 'Tout']];
    for (const [k, label] of items) {
      box.append(el('button', { class: 'chip', 'aria-pressed': String(momentOverride === k), onclick: () => { momentOverride = k; render(); } }, label));
    }
  }

  function buildDeck() {
    const m = curMoment();
    const now = Date.now();
    const sw = Object.fromEntries(S.swipes.map(s => [s.recipe_id, s]));
    const cards = [];
    for (const l of S.leftovers) if (!skippedLeftovers.has(l.id)) cards.push({ key: `L${l.id}`, leftover: l, recipe: S.recipes.find(r => r.id === l.recipe_id) });
    const recipes = S.recipes
      .filter(r => m === 'tout' || r.moments.includes(m))
      .filter(r => {
        const s = sw[r.id];
        if (!s) return true;
        if (s.verdict === 'like') return false;
        return now - Date.parse(s.at) > SKIP_DAYS * 864e5;
      })
      .map(r => ({ key: r.id, recipe: r, cov: coverage(r, persons) }))
      .sort((a, b) => b.cov.ratio - a.cov.ratio || a.cov.missing.length - b.cov.missing.length || a.recipe.name.localeCompare(b.recipe.name));
    return cards.concat(recipes);
  }

  function renderIdeas() {
    renderMoments();
    renderInbox();
    const deck = $('#deck'); deck.replaceChildren();
    const cards = buildDeck();
    deckKeys = cards.map(c => c.key);
    if (!cards.length) {
      const m = curMoment();
      deck.append(el('div', { class: 'empty' }, el('b', {}, 'Plus rien à proposer'),
        m === 'tout' ? 'Toutes les recettes sont validées ou passées.' : `Rien de plus pour « ${MOMENTS[m]} » — essaie un autre moment.`));
      $('#btn-no').disabled = $('#btn-yes').disabled = $('#btn-info').disabled = true;
      return;
    }
    $('#btn-no').disabled = $('#btn-yes').disabled = $('#btn-info').disabled = false;
    cards.slice(0, 3).reverse().forEach((c, i, arr) => {
      const pos = arr.length - 1 - i; // 0 = dessus
      const card = cardNode(c);
      if (pos === 1) card.classList.add('behind');
      if (pos === 2) card.classList.add('behind2');
      deck.append(card);
    });
    attachSwipe(deck.lastElementChild, cards[0]);
  }

  function cardNode(c) {
    const r = c.recipe;
    if (c.leftover) {
      const l = c.leftover;
      const d = new Date(l.cooked_at);
      return el('div', { class: 'card leftover', 'data-key': c.key },
        el('div', { class: 'badge yes' }, 'MIAM'), el('div', { class: 'badge no' }, 'PLUS TARD'),
        el('div', { class: 'emoji' }, r ? r.emoji : '🍱'),
        el('h2', {}, `Reste : ${r ? r.name : l.recipe_id}`),
        el('div', { class: 'meta' }, el('span', { class: 'pill' }, plural(l.portions, 'part', 'parts')), ' ',
          el('span', { class: 'pill' }, `cuisiné le ${d.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' })}`)),
        el('div', { class: 'lack' }, 'À réchauffer, zéro ingrédient à sortir.'),
        el('div', { class: 'hint' }, '→ j’en mange une part · ← plus tard'));
    }
    const cov = c.cov || coverage(r, persons);
    const lack = el('div', { class: 'lack' });
    if (cov.full) lack.append(el('b', {}, `Tout est là pour ${plural(persons, 'personne', 'personnes')}.`));
    else {
      lack.append(el('b', {}, `Manque ${cov.missing.length}/${cov.rows.length} :`));
      lack.append(el('ul', {}, cov.missing.slice(0, 5).map(m => el('li', { class: 'bad' }, `${m.ing.name} · ${fmtQty(m.lack, m.ing.unit)}`)),
        cov.missing.length > 5 ? el('li', {}, `… et ${cov.missing.length - 5} autres`) : null));
    }
    return el('div', { class: 'card', 'data-key': c.key },
      el('div', { class: 'badge yes' }, 'OUI'), el('div', { class: 'badge no' }, 'NOPE'),
      el('div', { class: 'emoji' }, r.emoji),
      el('h2', {}, r.name),
      el('div', { class: 'meta' }, r.minutes ? el('span', { class: 'pill' }, `⏱ ${r.minutes} min`) : null, ' ',
        r.moments.map(m => el('span', { class: 'pill' }, MOMENTS[m] || m))),
      gaugeRow(cov),
      lack,
      el('div', { class: 'hint' }, 'toucher = détail · → garder · ← passer'));
  }

  function gaugeRow(cov) {
    const pct = Math.round(cov.ratio * 100);
    return el('div', { class: `gauge-row${cov.full ? ' full' : ''}` },
      el('div', { class: `gauge${cov.full ? ' full' : ''}` }, el('i', { style: `width:${pct}%` })),
      el('b', {}, cov.full ? '✓ prêt' : `${pct} %`));
  }

  // Swipe : pointer events sur la carte du dessus. |dx| > 100px → verdict.
  function attachSwipe(card, c) {
    let x0 = 0, y0 = 0, dx = 0, dy = 0, drag = false, moved = false;
    const yes = $('.badge.yes', card), no = $('.badge.no', card);
    const set = () => {
      card.style.transform = `translate(${dx}px, ${dy * 0.3}px) rotate(${dx / 18}deg)`;
      yes.style.opacity = Math.min(1, Math.max(0, dx / 90));
      no.style.opacity = Math.min(1, Math.max(0, -dx / 90));
    };
    card.addEventListener('pointerdown', e => {
      if (e.button) return;
      drag = true; moved = false; x0 = e.clientX; y0 = e.clientY; dx = dy = 0;
      card.setPointerCapture(e.pointerId); card.classList.add('dragging');
    });
    card.addEventListener('pointermove', e => {
      if (!drag) return;
      dx = e.clientX - x0; dy = e.clientY - y0;
      if (Math.abs(dx) > 6 || Math.abs(dy) > 6) moved = true;
      set();
    });
    const end = e => {
      if (!drag) return;
      drag = false; card.classList.remove('dragging');
      if (Math.abs(dx) > 100) return decide(card, c, dx > 0 ? 'like' : 'skip');
      if (!moved) { card.style.transform = ''; return openDetail(c.recipe, c.leftover); }
      card.classList.add('settle'); card.style.transform = ''; yes.style.opacity = no.style.opacity = 0;
      setTimeout(() => card.classList.remove('settle'), 260);
    };
    card.addEventListener('pointerup', end);
    card.addEventListener('pointercancel', end);
  }

  async function decide(card, c, verdict) {
    card.classList.add('fly');
    card.style.transform = `translate(${verdict === 'like' ? 600 : -600}px, 40px) rotate(${verdict === 'like' ? 25 : -25}deg)`;
    await new Promise(r => setTimeout(r, 220));
    if (c.leftover) {
      if (verdict === 'like') { await api('POST', `/api/leftovers/${c.leftover.id}/eat`, { portions: 1 }); toast('Bon appétit — 1 part de moins'); }
      else { skippedLeftovers.add(c.leftover.id); render(); }
      return;
    }
    await api('POST', '/api/swipes', { recipe_id: c.recipe.id, verdict });
    toast(verdict === 'like' ? `${c.recipe.name} → Validées` : `Passée pour ${SKIP_DAYS} jours`);
  }

  function topCard() {
    const key = deckKeys[0]; if (!key) return null;
    const node = $(`#deck .card[data-key="${CSS.escape(key)}"]`);
    const c = buildDeck().find(x => x.key === key);
    return node && c ? { node, c } : null;
  }
  $('#btn-no').onclick = () => { const t = topCard(); if (t) decide(t.node, t.c, 'skip'); };
  $('#btn-yes').onclick = () => { const t = topCard(); if (t) decide(t.node, t.c, 'like'); };
  $('#btn-info').onclick = () => { const t = topCard(); if (t) openDetail(t.c.recipe, t.c.leftover); };

  function renderInbox() {
    const box = $('#inbox'); box.replaceChildren();
    box.hidden = !S.inbox.length;
    if (!S.inbox.length) return;
    box.append(el('b', {}, `📥 ${plural(S.inbox.length, 'recette reçue', 'recettes reçues')} à trier`),
      el('div', { class: 'small muted' }, 'Reçues via le Raccourci iOS. Le tri automatique arrive en phase 2 ; en attendant, le texte brut :'));
    for (const it of S.inbox.slice(0, 3)) {
      box.append(el('pre', {}, (it.url ? it.url + '\n' : '') + it.text),
        el('button', { class: 'btn small ghost', onclick: () => api('DELETE', `/api/inbox/${it.id}`) }, 'Marquer traitée'));
    }
  }

  // --- onglet Validées ---------------------------------------------------------------
  function renderLiked() {
    const lo = $('#leftovers'); lo.replaceChildren();
    if (!S.leftovers.length) lo.append(el('div', { class: 'muted small' }, 'Aucun reste. Cuisine pour plusieurs, mange une part : le reste arrive ici.'));
    for (const l of S.leftovers) {
      const r = S.recipes.find(x => x.id === l.recipe_id);
      lo.append(el('div', { class: 'row' },
        el('div', { class: 'ico' }, r ? r.emoji : '🍱'),
        el('div', { class: 'body' }, el('div', { class: 'name' }, r ? r.name : l.recipe_id),
          el('div', { class: 'sub' }, `${plural(l.portions, 'part', 'parts')} · ${new Date(l.cooked_at).toLocaleDateString('fr-FR')}`)),
        el('button', { class: 'btn small', onclick: () => api('POST', `/api/leftovers/${l.id}/eat`, { portions: 1 }).then(() => toast('−1 part')) }, '−1 part')));
    }
    const box = $('#liked'); box.replaceChildren();
    const liked = S.swipes.filter(s => s.verdict === 'like').map(s => S.recipes.find(r => r.id === s.recipe_id)).filter(Boolean);
    if (!liked.length) box.append(el('div', { class: 'muted small' }, 'Rien de validé. Swipe à droite dans Idées.'));
    for (const r of liked) {
      const cov = coverage(r, persons);
      box.append(el('div', { class: 'row clickable', onclick: () => openDetail(r) },
        el('div', { class: 'ico' }, r.emoji),
        el('div', { class: 'body' }, el('div', { class: 'name' }, r.name),
          el('div', { class: 'sub' }, cov.full ? `✓ prêt pour ${persons}` : `manque ${cov.missing.length}/${cov.rows.length}`)),
        el('div', { class: `gauge${cov.full ? ' full' : ''}` }, el('i', { style: `width:${Math.round(cov.ratio * 100)}%` }))));
    }
  }

  // --- onglet Stock -------------------------------------------------------------------
  const STEP = { g: 50, ml: 100, 'pièce': 1, tranche: 1, cas: 1, cac: 1 };
  function renderStock() {
    const q = ($('#stock-search').value || '').trim().toLowerCase();
    const only = $('#stock-only').checked;
    const box = $('#stock'); box.replaceChildren();
    const norm = s => s.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
    for (const [cat, label] of Object.entries(CATS)) {
      const items = S.ingredients.filter(i => i.category === cat)
        .filter(i => !q || norm(i.name).includes(norm(q)))
        .filter(i => !only || (S.stock[i.id] || 0) > 0);
      if (!items.length) continue;
      box.append(el('h2', { class: 'sec' }, label));
      const list = el('div', { class: 'list' });
      for (const i of items) list.append(stockRow(i));
      box.append(list);
    }
    if (!box.children.length) box.append(el('div', { class: 'muted small' }, 'Rien ne correspond.'));
  }
  function stockRow(i) {
    const qty = S.stock[i.id] || 0;
    const input = el('input', { type: 'number', min: 0, step: 'any', value: Math.round(qty * 100) / 100 });
    let h;
    input.addEventListener('input', () => { clearTimeout(h); h = setTimeout(() => api('PUT', `/api/stock/${i.id}`, { qty: +input.value || 0 }), 500); });
    const step = STEP[i.unit] || 1;
    return el('div', { class: `row stock-row${qty > 0 ? '' : ' zero'}` },
      el('div', { class: 'body' }, el('div', { class: 'name' }, i.name)),
      el('div', { class: 'qty' },
        el('button', { onclick: () => api('PUT', `/api/stock/${i.id}`, { delta: -step }) }, '−'),
        input,
        el('span', { class: 'u' }, i.unit),
        el('button', { onclick: () => api('PUT', `/api/stock/${i.id}`, { delta: step }) }, '+')));
  }
  $('#stock-search').addEventListener('input', renderStock);
  $('#stock-only').addEventListener('change', renderStock);
  $('#add-ing').addEventListener('submit', async e => {
    e.preventDefault();
    const f = new FormData(e.target);
    await api('POST', '/api/ingredients', { name: f.get('name'), unit: f.get('unit'), category: f.get('category') });
    e.target.reset(); toast('Ingrédient ajouté');
  });

  // --- dialogue recette ------------------------------------------------------------------
  const dlg = $('#dlg'), body = $('#dlg-body');
  let D = null; // { recipe, n, edit, cook, draft }

  function openDetail(recipe, leftover) {
    if (!recipe) return;
    D = { recipe, n: persons, edit: false, cook: false, leftover };
    renderDetail();
    if (!dlg.open) dlg.showModal();
  }
  dlg.addEventListener('close', () => { D = null; });
  dlg.addEventListener('click', e => { if (e.target === dlg) dlg.close(); });

  function renderDetail() {
    const r = S.recipes.find(x => x.id === D.recipe.id) || D.recipe;
    D.recipe = r;
    body.replaceChildren();
    const liked = S.swipes.some(s => s.recipe_id === r.id && s.verdict === 'like');

    // en-tête
    const head = el('div', { class: 'head' }, el('div', { class: 'emoji' }, r.emoji));
    if (D.edit) {
      D.draft.nameInput = el('input', { class: 'name', value: D.draft.name });
      D.draft.minInput = el('input', { class: 'q', type: 'number', min: 0, value: D.draft.minutes, style: 'width:60px' });
      head.append(el('div', { style: 'flex:1' }, D.draft.nameInput, el('div', { class: 'meta', style: 'margin-top:6px' }, '⏱ ', D.draft.minInput, ' min')));
    } else {
      head.append(el('div', { style: 'flex:1' }, el('h3', {}, r.name),
        el('div', { class: 'meta' }, r.minutes ? `⏱ ${r.minutes} min · ` : '', r.moments.map(m => MOMENTS[m] || m).join(', '),
          r.source === 'user' ? ' · ajustée' : '')));
    }
    head.append(el('button', { class: 'close', onclick: () => dlg.close() }, '×'));
    body.append(head);

    // personnes + jauge
    const cov = coverage(D.edit ? draftRecipe() : r, D.n);
    const range = el('input', { type: 'range', min: 1, max: 8, step: 1, value: D.n });
    range.addEventListener('input', () => { D.n = +range.value; renderDetail(); });
    body.append(el('div', { class: 'persons' }, '👥 ', range, el('b', {}, D.n), el('span', {}, D.n > 1 ? ' personnes' : ' personne')));
    body.append(gaugeRow(cov));

    // ingrédients
    body.append(el('h4', {}, D.edit ? 'Ingrédients (quantité par personne)' : `Ingrédients pour ${D.n}`));
    const table = el('table', { class: 'ings' });
    if (D.edit) {
      D.draft.ingredients.forEach((x, idx) => {
        const ing = ING[x.ingredient_id];
        const inp = el('input', { class: 'q', type: 'number', min: 0, step: 'any', value: x.qty });
        inp.addEventListener('input', () => { x.qty = +inp.value || 0; });
        table.append(el('tr', {}, el('td', {}, ing.name), el('td', { class: 'q' }, inp, ' ', el('span', { class: 'muted small' }, ing.unit)),
          el('td', { class: 's' }, el('button', { class: 'rm', title: 'Retirer', onclick: () => { D.draft.ingredients.splice(idx, 1); renderDetail(); } }, '✕'))));
      });
      body.append(table);
      const used = new Set(D.draft.ingredients.map(x => x.ingredient_id));
      const sel = el('select', {}, el('option', { value: '' }, '+ ajouter un ingrédient…'),
        S.ingredients.filter(i => !used.has(i.id)).map(i => el('option', { value: i.id }, `${i.name} (${i.unit})`)));
      const q = el('input', { type: 'number', min: 0, step: 'any', placeholder: 'qté' });
      body.append(el('div', { class: 'addline' }, sel, q,
        el('button', { class: 'btn small', onclick: () => { if (sel.value && +q.value > 0) { D.draft.ingredients.push({ ingredient_id: sel.value, qty: +q.value }); renderDetail(); } } }, 'OK')));
    } else {
      for (const row of cov.rows) {
        table.append(el('tr', { class: row.ok ? 'ok' : 'bad' },
          el('td', {}, row.ing.name),
          el('td', { class: 'q' }, fmtQty(row.need, row.ing.unit)),
          el('td', { class: 's' }, row.ok ? '✓' : `manque ${fmtQty(row.lack, row.ing.unit)}`)));
      }
      body.append(table);
    }

    // étapes
    if (!D.edit && r.steps.length) {
      body.append(el('h4', {}, 'Préparation'), el('ol', { class: 'steps' }, r.steps.map(s => el('li', {}, s))));
    }

    // cuisine
    if (D.cook) body.append(cookBox(r, cov));

    // actions
    const actions = el('div', { class: 'actions' });
    if (D.edit) {
      actions.append(el('button', { class: 'btn', onclick: saveEdit }, 'Enregistrer'),
        el('button', { class: 'btn ghost', onclick: () => { D.edit = false; renderDetail(); } }, 'Annuler'));
    } else if (D.leftover) {
      const l = S.leftovers.find(x => x.id === D.leftover.id);
      if (l) actions.append(el('button', { class: 'btn warn', onclick: () => api('POST', `/api/leftovers/${l.id}/eat`, { portions: 1 }).then(() => { toast('Bon appétit — 1 part de moins'); dlg.close(); }) }, `🍴 J'en mange une part (reste ${l.portions})`));
    } else if (!D.cook) {
      actions.append(el('button', { class: 'btn warn', onclick: () => { D.cook = true; D.eaten = 1; renderDetail(); } }, '🍳 Je cuisine'),
        el('button', { class: 'btn ghost', onclick: startEdit }, '✏️ Ajuster'),
        el('span', { class: 'spacer' }),
        liked
          ? el('button', { class: 'btn bad small', onclick: () => api('DELETE', `/api/swipes/${r.id}`).then(() => { toast('Retirée des validées'); renderDetail(); }) }, 'Retirer')
          : el('button', { class: 'btn ghost small', onclick: () => api('POST', '/api/swipes', { recipe_id: r.id, verdict: 'like' }).then(() => { toast('→ Validées'); renderDetail(); }) }, '♥ Garder'));
    }
    body.append(actions);
  }

  function draftRecipe() { return { ...D.recipe, ingredients: D.draft.ingredients }; }
  function startEdit() {
    D.edit = true;
    D.draft = { name: D.recipe.name, minutes: D.recipe.minutes, ingredients: D.recipe.ingredients.map(x => ({ ...x })) };
    renderDetail();
  }
  async function saveEdit() {
    const payload = {
      name: D.draft.nameInput.value.trim() || D.recipe.name,
      minutes: +D.draft.minInput.value || 0,
      ingredients: D.draft.ingredients.filter(x => x.qty > 0),
    };
    await api('PUT', `/api/recipes/${D.recipe.id}`, payload);
    D.edit = false; toast('Recette ajustée'); renderDetail();
  }

  function cookBox(r, cov) {
    const eaten = el('input', { type: 'number', min: 0, max: D.n, step: 1, value: Math.min(D.eaten, D.n) });
    eaten.addEventListener('input', () => { D.eaten = Math.max(0, Math.min(D.n, +eaten.value || 0)); prev(); });
    const preview = el('div', { class: 'preview' });
    const prev = () => {
      const rest = Math.max(0, D.n - D.eaten);
      const dec = cov.rows.map(x => `${x.ing.name} −${fmtQty(Math.min(x.have, x.need), x.ing.unit)}`).join(', ');
      preview.replaceChildren(
        el('div', {}, `Stock décrémenté : ${dec || 'rien'}.`),
        cov.missing.length ? el('div', { style: 'color:var(--bad)' }, `⚠ ${cov.missing.length} ingrédient(s) pas en stock — décrémenté à 0, pas en négatif.`) : null,
        el('div', {}, rest > 0 ? `${plural(rest, 'part', 'parts')} au frigo → onglet Validées / Restes.` : 'Aucun reste.'));
    };
    prev();
    return el('div', { class: 'cookbox' },
      el('b', {}, `Je cuisine pour ${plural(D.n, 'personne', 'personnes')}`),
      el('label', {}, 'Parts mangées maintenant', eaten),
      preview,
      el('div', { class: 'actions' },
        el('button', { class: 'btn warn', onclick: async () => {
          const res = await api('POST', '/api/cook', { recipe_id: r.id, persons: D.n, eaten: D.eaten });
          toast(res.rest > 0 ? `Stock mis à jour · ${plural(res.rest, 'part', 'parts')} en reste` : 'Stock mis à jour');
          dlg.close();
        } }, 'Confirmer'),
        el('button', { class: 'btn ghost', onclick: () => { D.cook = false; renderDetail(); } }, 'Annuler')));
  }

  // --- global -----------------------------------------------------------------------------
  $('#persons').addEventListener('input', e => { persons = +e.target.value; localStorage.setItem('food.persons', persons); render(); });
  for (const b of document.querySelectorAll('nav.tabs button')) b.addEventListener('click', () => { tab = b.dataset.tab; render(); });
  const tick = () => { $('#clock').textContent = new Date().toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' }); };
  tick(); setInterval(tick, 30000);

  api('GET', '/api/state').catch(e => { $('#deck').append(el('div', { class: 'empty' }, el('b', {}, 'Serveur injoignable'), String(e))); });
})();
