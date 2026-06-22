const $ = id => document.getElementById(id);
let currentRole = '';

async function api(path, opts) {
  const res = await fetch('/api' + path, opts);
  const text = await res.text();
  try { return { ok: res.ok, data: JSON.parse(text) }; } catch(e) { return { ok: res.ok, data: text }; }
}

function show(el) { el.classList.remove('hidden'); }
function hide(el) { el.classList.add('hidden'); }

function esc(s) {
  return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

async function loadProducts() {
  const r = await api('/produtos');
  if (!r.ok) return;

  const tbody = document.querySelector('#products-table tbody');
  tbody.innerHTML = '';
  const select = $('select-product');
  select.innerHTML = '';

  r.data.forEach(p => {
    const opt = document.createElement('option');
    opt.value = p.id;
    opt.textContent = p.nome;
    select.appendChild(opt);

    const tr = document.createElement('tr');
    tr.dataset.id = p.id;

    if (currentRole === 'admin') {
      tr.innerHTML = `
        <td>${esc(p.nome)}</td>
        <td>${p.quantidade}</td>
        <td class="td-actions">
          <button class="btn-edit" data-id="${p.id}" data-nome="${esc(p.nome)}" data-qty="${p.quantidade}">Editar</button>
          <button class="btn-delete" data-id="${p.id}">Excluir</button>
        </td>`;
    } else {
      tr.innerHTML = `<td>${esc(p.nome)}</td><td>${p.quantidade}</td>`;
    }
    tbody.appendChild(tr);
  });
}

async function loadMovements() {
  const r = await api('/movimentacoes');
  if (!r.ok) return;
  const ul = $('movements-list');
  ul.innerHTML = '';
  r.data.forEach(m => {
    const li = document.createElement('li');
    li.textContent = `${m.dataHora} - Produto ${m.produtoId} - Qtd: ${m.quantidade} - Usuário: ${m.usuario}`;
    ul.appendChild(li);
  });
}

function setUserInfo(username, role) {
  currentRole = role;
  $('user-info').textContent = username;
  $('user-avatar').textContent = username.charAt(0).toUpperCase();

  if (role === 'admin') {
    show(document.querySelector('.th-actions'));
    show($('tfoot-admin'));
  } else {
    hide(document.querySelector('.th-actions'));
    hide($('tfoot-admin'));
  }
}

function makeEditableRow(tr, id, nome, qty) {
  tr.innerHTML = `
    <td><input class="inline-input" type="text" value="${esc(nome)}" placeholder="Nome do produto"></td>
    <td><input class="inline-input inline-qty" type="number" min="0" value="${qty}"></td>
    <td class="td-actions">
      <button class="btn-save" data-id="${id || ''}">Salvar</button>
      <button class="btn-cancel">Cancelar</button>
    </td>`;
  tr.querySelector('.inline-input').focus();
}

document.addEventListener('DOMContentLoaded', () => {

  $('login-form').addEventListener('submit', async e => {
    e.preventDefault();
    const user = $('input-username').value.trim();
    const role = $('select-role').value;
    const r = await api('/login', { method: 'POST', headers: {'content-type':'application/json'}, body: JSON.stringify({ usuario: user, tipo: role }) });
    if (r.ok) {
      hide($('login-section'));
      show($('app-section'));
      setUserInfo(user, role);
      await loadProducts();
      await loadMovements();
    } else alert('Erro ao entrar');
  });

  $('btn-logout').addEventListener('click', async () => {
    await api('/logout', { method: 'POST' });
    hide($('app-section'));
    show($('login-section'));
  });

  document.querySelectorAll('.nav-item').forEach(item => {
    item.addEventListener('click', e => {
      e.preventDefault();
      document.querySelectorAll('.nav-item').forEach(i => i.classList.remove('active'));
      document.querySelectorAll('.tab-panel').forEach(p => p.classList.add('hidden'));
      item.classList.add('active');
      $(item.dataset.tab).classList.remove('hidden');
    });
  });

  // CRUD inline na tabela
  document.querySelector('#products-table tbody').addEventListener('click', async e => {
    const btn = e.target;
    if (!btn.matches('button')) return;
    const tr = btn.closest('tr');

    if (btn.classList.contains('btn-edit')) {
      makeEditableRow(tr, btn.dataset.id, btn.dataset.nome, btn.dataset.qty);
    }

    if (btn.classList.contains('btn-delete')) {
      if (!confirm('Excluir este produto?')) return;
      const r = await api('/produtos/' + btn.dataset.id, { method: 'DELETE' });
      if (!r.ok) { alert('Erro ao excluir: ' + (r.data || r.status)); return; }
      await loadProducts();
    }

    if (btn.classList.contains('btn-save')) {
      const nome = tr.querySelector('input[type="text"]').value.trim();
      const qty  = Number(tr.querySelector('input[type="number"]').value);
      if (!nome) { alert('Informe o nome do produto'); return; }
      const id = btn.dataset.id;
      const body = JSON.stringify({ nome, quantidade: qty });
      const headers = { 'content-type': 'application/json' };
      const r = id
        ? await api('/produtos/' + id, { method: 'PUT',  headers, body })
        : await api('/produtos',       { method: 'POST', headers, body });
      if (!r.ok) { alert('Erro ao salvar: ' + (r.data || r.status)); return; }
      await loadProducts();
    }

    if (btn.classList.contains('btn-cancel')) {
      await loadProducts();
    }
  });

  $('btn-new-product').addEventListener('click', () => {
    const tbody = document.querySelector('#products-table tbody');
    const tr = document.createElement('tr');
    tbody.appendChild(tr);
    makeEditableRow(tr, null, '', 0);
    tr.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  });

  $('form-withdraw').addEventListener('submit', async e => {
    e.preventDefault();
    const productId = Number($('select-product').value);
    const qty = Number($('input-qty').value);
    const r = await api('/saida', { method: 'POST', headers: {'content-type':'application/json'}, body: JSON.stringify({ produtoId: productId, quantidade: qty }) });
    const msgEl = $('withdraw-message');
    if (!r.ok) { msgEl.textContent = r.data; await loadProducts(); return; }
    msgEl.textContent = 'Saída registrada.';
    await loadProducts();
    await loadMovements();
  });

});
