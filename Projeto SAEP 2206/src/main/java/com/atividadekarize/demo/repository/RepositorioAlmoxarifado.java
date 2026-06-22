package com.atividadekarize.demo.repository;

import java.util.Collection;
import java.util.List;

import com.atividadekarize.demo.model.Movimentacao;
import com.atividadekarize.demo.model.Produto;
import com.atividadekarize.demo.model.Usuario;

public interface RepositorioAlmoxarifado {

    Collection<Produto> findAllProdutos();
    Produto findProduto(Long id);
    Produto adicionarProduto(String nome, int quantidade);
    Movimentacao adicionarMovimentacao(Long produtoId, int quantidade, String usuario, String tipo);
    List<Movimentacao> findAllMovimentacoes();
    boolean atualizarProduto(Long id, String nome, int quantidade);
    boolean removerProduto(Long id);
    boolean retirar(Long produtoId, int quantidade);

    Collection<Usuario> findAllUsuarios();
    Usuario findUsuario(Long id);
    Usuario findUsuarioPorMatricula(String matricula);
    Usuario adicionarUsuario(String matricula, String nome, String tipo);
    boolean atualizarUsuario(Long id, String matricula, String nome, String tipo);
    boolean removerUsuario(Long id);

    void initDadosIniciais();
}