const $ = id => document.getElementById(id);
let currentRole = '';
const ESTOQUE_MINIMO = 10;

async function api(path, opts) {
  const res = await fetch('/api' + path, { credentials: 'include', ...opts });
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

  if (currentRole === 'admin') {
    mostrarAvisosEstoque(r.data);
  } else {
    hide($('stock-alerts'));
  }
}

function mostrarAvisosEstoque(produtos) {
  const box = $('stock-alerts');
  const acabando = produtos.filter(p => p.quantidade <= ESTOQUE_MINIMO);

  if (acabando.length === 0) {
    hide(box);
    return;
  }

  box.innerHTML = acabando.map(p =>
    `<p>${esc(p.nome)} está acabando: ${p.quantidade} unidades restantes. Recomenda abastecer o estoque.</p>`
  ).join('');
  show(box);
}

async function loadMovements() {
  const r = await api('/movimentacoes');
  const ul = $('movements-list');
  ul.innerHTML = '';
  if (!r.ok) {
    ul.innerHTML = '<li>Nao foi possivel carregar as movimentacoes.</li>';
    return;
  }
  const lista = Array.isArray(r.data) ? r.data : [];
  if (lista.length === 0) {
    ul.innerHTML = '<li>Nenhuma movimentacao ainda. Registre uma saida primeiro.</li>';
    return;
  }
  const produtos = await api('/produtos');
  const nomes = {};
  if (produtos.ok && Array.isArray(produtos.data)) {
    produtos.data.forEach(p => { nomes[p.id] = p.nome; });
  }
  lista.forEach(m => {
    const li = document.createElement('li');
    const nomeProduto = nomes[m.produtoId] || ('Produto ' + m.produtoId);
    li.textContent = `${m.dataHora} - ${nomeProduto} - Qtd: ${m.quantidade} - Usuario: ${m.usuario}`;
    ul.appendChild(li);
  });
}

function setUserInfo(nome, role) {
  currentRole = role;
  $('user-info').textContent = nome;
  $('user-avatar').textContent = nome.charAt(0).toUpperCase();

  if (role === 'admin') {
    show(document.querySelector('.th-actions'));
    show($('tfoot-admin'));
    show($('nav-usuarios'));
  } else {
    hide(document.querySelector('.th-actions'));
    hide($('tfoot-admin'));
    hide($('nav-usuarios'));
  }
}

async function loadUsers() {
  const r = await api('/usuarios');
  if (!r.ok) return;

  const tbody = document.querySelector('#users-table tbody');
  tbody.innerHTML = '';

  r.data.forEach(u => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${esc(u.matricula)}</td>
      <td>${esc(u.nome)}</td>
      <td>${esc(u.tipo)}</td>
      <td class="td-actions">
        <button class="btn-edit-user" data-id="${u.id}" data-matricula="${esc(u.matricula)}" data-nome="${esc(u.nome)}" data-tipo="${esc(u.tipo)}">Editar</button>
        <button class="btn-delete-user" data-id="${u.id}">Excluir</button>
      </td>`;
    tbody.appendChild(tr);
  });
}

function makeEditableUserRow(tr, id, matricula, nome, tipo) {
  tr.innerHTML = `
    <td><input class="inline-input" type="text" value="${esc(matricula)}" placeholder="Matrícula"></td>
    <td><input class="inline-input" type="text" value="${esc(nome)}" placeholder="Nome"></td>
    <td>
      <select class="inline-input">
        <option value="operador" ${tipo === 'operador' ? 'selected' : ''}>Operador</option>
        <option value="admin" ${tipo === 'admin' ? 'selected' : ''}>Administrador</option>
      </select>
    </td>
    <td class="td-actions">
      <button class="btn-save-user" data-id="${id || ''}">Salvar</button>
      <button class="btn-cancel-user">Cancelar</button>
    </td>`;
  tr.querySelector('input').focus();
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
    const matricula = $('input-matricula').value.trim();
    const r = await api('/login', { method: 'POST', headers: {'content-type':'application/json'}, body: JSON.stringify({ matricula }) });
    if (r.ok) {
      hide($('login-section'));
      show($('app-section'));
      setUserInfo(r.data.nome, r.data.tipo);
      await loadProducts();
      await loadMovements();
      if (r.data.tipo === 'admin') await loadUsers();
    } else alert(r.data || 'Matrícula não encontrada');
  });

  $('btn-logout').addEventListener('click', async () => {
    await api('/logout', { method: 'POST' });
    hide($('app-section'));
    show($('login-section'));
  });

  document.querySelectorAll('.nav-item').forEach(item => {
    item.addEventListener('click', async e => {
      e.preventDefault();
      document.querySelectorAll('.nav-item').forEach(i => i.classList.remove('active'));
      document.querySelectorAll('.tab-panel').forEach(p => p.classList.add('hidden'));
      item.classList.add('active');
      $(item.dataset.tab).classList.remove('hidden');
      if (item.dataset.tab === 'tab-movimentacoes') await loadMovements();
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

  document.querySelector('#users-table tbody').addEventListener('click', async e => {
    const btn = e.target;
    if (!btn.matches('button')) return;
    const tr = btn.closest('tr');

    if (btn.classList.contains('btn-edit-user')) {
      makeEditableUserRow(tr, btn.dataset.id, btn.dataset.matricula, btn.dataset.nome, btn.dataset.tipo);
    }

    if (btn.classList.contains('btn-delete-user')) {
      if (!confirm('Excluir este usuário?')) return;
      const r = await api('/usuarios/' + btn.dataset.id, { method: 'DELETE' });
      if (!r.ok) { alert('Erro ao excluir: ' + (r.data || r.status)); return; }
      await loadUsers();
    }

    if (btn.classList.contains('btn-save-user')) {
      const matricula = tr.querySelectorAll('input')[0].value.trim();
      const nome = tr.querySelectorAll('input')[1].value.trim();
      const tipo = tr.querySelector('select').value;
      if (!matricula || !nome) { alert('Preencha matrícula e nome'); return; }
      const id = btn.dataset.id;
      const body = JSON.stringify({ matricula, nome, tipo });
      const headers = { 'content-type': 'application/json' };
      const r = id
        ? await api('/usuarios/' + id, { method: 'PUT', headers, body })
        : await api('/usuarios', { method: 'POST', headers, body });
      if (!r.ok) { alert('Erro ao salvar: ' + (r.data || r.status)); return; }
      await loadUsers();
    }

    if (btn.classList.contains('btn-cancel-user')) {
      await loadUsers();
    }
  });

  $('btn-new-user').addEventListener('click', () => {
    const tbody = document.querySelector('#users-table tbody');
    const tr = document.createElement('tr');
    tbody.appendChild(tr);
    makeEditableUserRow(tr, null, '', '', 'operador');
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
