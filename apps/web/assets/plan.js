/* Portal6 — moteur de « plan de changements » partagé par les vues d'action
 * (dédup local / Spotify, propositions de playlists).
 *
 * Principe : la page enregistre des décisions (id → kind + libellé) ; une barre
 * fixe en bas compte ce qui est en attente ; « Commit changes » ouvre une revue
 * façon plan Jira (regroupé par type, chaque ligne cochable) ; « Valider »
 * fige le plan dans l'historique, le télécharge en JSON et affiche la commande
 * qui l'appliquera. Le navigateur n'écrit jamais ailleurs que dans localStorage.
 *
 *   const plan = P6Plan.mount({
 *     storeKey: 'portal6.plan.dedup-local',
 *     title:    'Dédup locale',
 *     kinds:    { quarantine: { label: 'Déplacer en quarantaine', tone: 'warn' }, ... },
 *     filename: 'dedup_plan.local.json',
 *     command:  (plan) => 'python ... --dry-run',
 *     onChange: () => render(),          // la page se redessine
 *   });
 *   plan.set(id, 'quarantine', { label, detail, meta });   plan.clear(id);   plan.get(id)
 */
window.P6Plan = (function () {
  'use strict';

  const esc = (s) => String(s ?? '').replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
  const num = (n) => Number(n || 0).toLocaleString('fr-FR');
  const human = (b) => {
    if (!b) return '';
    const u = ['o', 'Ko', 'Mo', 'Go']; let i = 0; let v = b;
    while (v >= 1024 && i < u.length - 1) { v /= 1024; i++; }
    return `${v.toFixed(i ? 1 : 0)} ${u[i]}`;
  };

  function mount(cfg) {
    const KEY = cfg.storeKey;
    const HKEY = cfg.storeKey + '.history';

    const load = (k, d) => { try { return JSON.parse(localStorage.getItem(k)) || d; } catch { return d; } };
    const store = (k, v) => { try { localStorage.setItem(k, JSON.stringify(v)); } catch { /* indisponible */ } };

    // pending : { id: { kind, label, detail, meta, at } }
    let pending = load(KEY, {});
    let history = load(HKEY, []);

    // ------------------------------------------------------------- barre
    const bar = document.createElement('div');
    bar.className = 'planbar';
    bar.innerHTML = `
      <div class="planbar-inner">
        <div class="planbar-counts"></div>
        <span class="spacer"></span>
        <button class="btn ghost small" data-plan="reset">Tout annuler</button>
        <button class="btn" data-plan="commit">Commit changes</button>
      </div>`;
    document.body.appendChild(bar);
    document.body.classList.add('has-planbar');

    // ---------------------------------------------------------- dialogue
    const dlg = document.createElement('dialog');
    dlg.className = 'plan-dialog';
    document.body.appendChild(dlg);

    function counts() {
      const c = {};
      let bytes = 0;
      Object.values(pending).forEach((d) => {
        c[d.kind] = (c[d.kind] || 0) + 1;
        bytes += (d.meta && d.meta.bytes) || 0;
      });
      return { c, bytes, total: Object.keys(pending).length };
    }

    function refreshBar() {
      const { c, bytes, total } = counts();
      bar.classList.toggle('empty', total === 0);
      bar.querySelector('.planbar-counts').innerHTML = total === 0
        ? `<span class="muted">${esc(cfg.title)} — aucune action en attente</span>`
        : `<strong>${num(total)}</strong> action${total > 1 ? 's' : ''} en attente · `
          + Object.entries(c).map(([k, n]) =>
            `<span class="pill ${cfg.kinds[k]?.tone || ''}">${num(n)} ${esc(cfg.kinds[k]?.label || k)}</span>`).join(' ')
          + (bytes ? ` · <span class="muted">${human(bytes)} libérables</span>` : '');
      bar.querySelector('[data-plan="commit"]').disabled = total === 0;
    }

    function openCommit() {
      const { c, bytes, total } = counts();
      if (!total) return;
      const groups = Object.keys(cfg.kinds).filter((k) => c[k]);
      dlg.innerHTML = `
        <form method="dialog">
          <div class="plan-head">
            <div>
              <h3>Plan de changements — ${esc(cfg.title)}</h3>
              <p class="hint">Revue avant validation. Décoche ce que tu ne veux pas dans ce plan ;
                 ce qui reste coché part dans le JSON, le reste reste en attente.</p>
            </div>
            <div class="plan-summary">
              ${groups.map((k) => `<div class="stat ${cfg.kinds[k].tone || ''}"><div class="n">${num(c[k])}</div><div class="l">${esc(cfg.kinds[k].label)}</div></div>`).join('')}
              ${bytes ? `<div class="stat"><div class="n">${human(bytes)}</div><div class="l">libérables</div></div>` : ''}
            </div>
          </div>
          <div class="plan-body">
            ${groups.map((k) => {
              const items = Object.entries(pending).filter(([, d]) => d.kind === k);
              return `
                <section class="plan-group" data-kind="${esc(k)}">
                  <header>
                    <label><input type="checkbox" class="grp" checked> <strong>${esc(cfg.kinds[k].label)}</strong>
                      <span class="pill ${cfg.kinds[k].tone || ''}">${num(items.length)}</span></label>
                    <span class="muted">${esc(cfg.kinds[k].help || '')}</span>
                  </header>
                  <ul>
                    ${items.map(([id, d]) => `
                      <li><label>
                        <input type="checkbox" class="item" data-id="${esc(id)}" checked>
                        <span class="lbl">${esc(d.label)}</span>
                        ${d.detail ? `<span class="det">${esc(d.detail)}</span>` : ''}
                      </label></li>`).join('')}
                  </ul>
                </section>`;
            }).join('')}
          </div>
          <div class="plan-foot">
            <span class="plan-total"></span>
            <span class="spacer"></span>
            <button class="btn ghost" value="cancel">Annuler</button>
            <button class="btn" type="button" data-plan="validate">Valider le plan</button>
          </div>
        </form>`;

      const total_ = () => {
        const n = dlg.querySelectorAll('input.item:checked').length;
        dlg.querySelector('.plan-total').textContent = `${num(n)} / ${num(total)} changements retenus`;
        dlg.querySelector('[data-plan="validate"]').disabled = n === 0;
      };
      dlg.addEventListener('change', (e) => {
        if (e.target.classList.contains('grp')) {
          e.target.closest('.plan-group').querySelectorAll('input.item').forEach((i) => { i.checked = e.target.checked; });
        }
        total_();
      });
      total_();
      // Bouton explicite plutôt que submit + événement `close` : ce dernier n'est
      // pas dispatché de façon fiable par tous les moteurs après un submit de dialog.
      dlg.querySelector('[data-plan="validate"]').addEventListener('click', () => { dlg.close(); validate(); });
      dlg.showModal();
    }

    function validate() {
      const ids = [...dlg.querySelectorAll('input.item:checked')].map((i) => i.dataset.id);
      if (!ids.length) return;
      const items = ids.map((id) => ({ id, ...pending[id] }));
      const plan = {
        id: new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19),
        title: cfg.title,
        created: new Date().toISOString(),
        status: 'to_apply',
        items,
      };
      history.unshift(plan);
      history = history.slice(0, 20);
      ids.forEach((id) => delete pending[id]);
      store(KEY, pending); store(HKEY, history);
      download(plan);
      refreshBar(); renderHistory();
      cfg.onChange && cfg.onChange();
      showCommand(plan);
    }

    function download(plan) {
      const blob = new Blob([JSON.stringify(plan, null, 2)], { type: 'application/json' });
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = (cfg.filename || 'plan.json').replace('.json', `.${plan.id}.json`);
      a.click();
      URL.revokeObjectURL(a.href);
    }

    function showCommand(plan) {
      const cmd = cfg.command ? cfg.command(plan) : '';
      dlg.innerHTML = `
        <form method="dialog">
          <h3>Plan validé — ${esc(plan.id)}</h3>
          <p>${num(plan.items.length)} changements figés et téléchargés en JSON.
             Dépose le fichier dans <code>data/music/</code>, puis :</p>
          <pre>${esc(cmd)}</pre>
          <p class="hint">Le navigateur n'a rien modifié sur le disque ni sur Spotify : le plan est
             un contrat d'interface pour le script (dry-run d'abord, jamais de suppression
             sans passer par la quarantaine).</p>
          <div class="plan-foot"><span class="spacer"></span><button class="btn">Fermer</button></div>
        </form>`;
      dlg.showModal();
    }

    // -------------------------------------------------------- historique
    function renderHistory() {
      const host = document.getElementById('plan-history');
      if (!host) return;
      if (!history.length) { host.innerHTML = '<p class="hint">Aucun plan validé pour l\'instant.</p>'; return; }
      host.innerHTML = `
        <div class="scroll"><table>
          <thead><tr><th>Plan</th><th>Date</th><th class="num">Changements</th><th>Détail</th><th>Statut</th><th></th></tr></thead>
          <tbody>${history.map((p) => {
            const c = {};
            p.items.forEach((d) => { c[d.kind] = (c[d.kind] || 0) + 1; });
            return `<tr data-plan-id="${esc(p.id)}">
              <td class="mono">${esc(p.id)}</td>
              <td class="mono">${esc(p.created.slice(0, 16).replace('T', ' '))}</td>
              <td class="num">${num(p.items.length)}</td>
              <td>${Object.entries(c).map(([k, n]) => `<span class="pill ${cfg.kinds[k]?.tone || ''}">${num(n)} ${esc(cfg.kinds[k]?.label || k)}</span>`).join(' ')}</td>
              <td>
                <select class="master" data-plan-status>
                  <option value="to_apply" ${p.status === 'to_apply' ? 'selected' : ''}>À appliquer</option>
                  <option value="applied" ${p.status === 'applied' ? 'selected' : ''}>Appliqué</option>
                  <option value="dropped" ${p.status === 'dropped' ? 'selected' : ''}>Abandonné</option>
                </select>
              </td>
              <td>
                <button type="button" class="btn small ghost" data-plan-act="download">JSON</button>
                <button type="button" class="btn small ghost" data-plan-act="reopen">Réouvrir</button>
              </td>
            </tr>`;
          }).join('')}</tbody>
        </table></div>`;
    }

    document.addEventListener('click', (e) => {
      const b = e.target.closest('[data-plan],[data-plan-act]');
      if (!b) return;
      if (b.dataset.plan === 'commit') openCommit();
      if (b.dataset.plan === 'reset') {
        if (!Object.keys(pending).length || confirm('Annuler toutes les actions en attente ?')) {
          pending = {}; store(KEY, pending); refreshBar(); cfg.onChange && cfg.onChange();
        }
      }
      if (b.dataset.planAct) {
        const tr = b.closest('tr');
        const p = history.find((x) => x.id === tr.dataset.planId);
        if (!p) return;
        if (b.dataset.planAct === 'download') download(p);
        if (b.dataset.planAct === 'reopen') {
          p.items.forEach((d) => { pending[d.id] = { kind: d.kind, label: d.label, detail: d.detail, meta: d.meta, at: d.at }; });
          history = history.filter((x) => x !== p);
          store(KEY, pending); store(HKEY, history);
          refreshBar(); renderHistory(); cfg.onChange && cfg.onChange();
        }
      }
    });
    document.addEventListener('change', (e) => {
      if (!e.target.matches('[data-plan-status]')) return;
      const p = history.find((x) => x.id === e.target.closest('tr').dataset.planId);
      if (p) { p.status = e.target.value; store(HKEY, history); }
    });

    refreshBar(); renderHistory();

    return {
      get: (id) => pending[id] || null,
      set(id, kind, info) {
        pending[id] = { kind, label: info?.label || id, detail: info?.detail || '', meta: info?.meta || {}, at: Date.now() };
        store(KEY, pending); refreshBar();
      },
      clear(id) { delete pending[id]; store(KEY, pending); refreshBar(); },
      bulk(fn) { fn(pending); store(KEY, pending); refreshBar(); },
      count: () => Object.keys(pending).length,
      pending: () => pending,
      esc, num, human,
    };
  }

  return { mount, esc, num, human };
})();
