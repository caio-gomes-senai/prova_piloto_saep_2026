package com.atividadekarize.demo.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

import com.atividadekarize.demo.model.Movimentacao;
import com.atividadekarize.demo.model.Produto;

@Repository
public class RepositorioMemoria {
    private final Map<Long, Produto> produtos = new ConcurrentHashMap<>();
    private final List<Movimentacao> movimentacoes = new ArrayList<>();
    private final AtomicLong produtoIdGen = new AtomicLong(1);
    private final AtomicLong movimentacaoIdGen = new AtomicLong(1);

    public Collection<Produto> findAllProdutos() { return produtos.values(); }

    public Produto findProduto(Long id) { return produtos.get(id); }

    public Produto adicionarProduto(String nome, int quantidade) {
        Long id = produtoIdGen.getAndIncrement();
        Produto p = new Produto(id, nome, quantidade);
        produtos.put(id, p);
        return p;
    }

    public Movimentacao adicionarMovimentacao(Long produtoId, int quantidade, String usuario) {
        Long id = movimentacaoIdGen.getAndIncrement();
        Movimentacao m = new Movimentacao(id, produtoId, quantidade, usuario, LocalDateTime.now());
        movimentacoes.add(m);
        return m;
    }

    public List<Movimentacao> findAllMovimentacoes() { return new ArrayList<>(movimentacoes); }

    public boolean atualizarProduto(Long id, String nome, int quantidade) {
        Produto p = produtos.get(id);
        if (p == null) return false;
        p.setNome(nome);
        p.setQuantidade(quantidade);
        return true;
    }

    public boolean removerProduto(Long id) {
        return produtos.remove(id) != null;
    }

    public boolean retirar(Long produtoId, int quantidade) {
        Produto p = produtos.get(produtoId);
        if (p == null) return false;
        synchronized (p) {
            if (p.getQuantidade() < quantidade) return false;
            p.setQuantidade(p.getQuantidade() - quantidade);
            return true;
        }
    }

    public void initDadosIniciais() {
        if (produtos.isEmpty()) {
            adicionarProduto("Álcool 70%", 50);
            adicionarProduto("Sabão detergente", 30);
            adicionarProduto("Luvas descartáveis", 100);
        }
    }
}
