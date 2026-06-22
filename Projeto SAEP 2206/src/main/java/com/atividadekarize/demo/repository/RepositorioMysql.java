package com.atividadekarize.demo.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.atividadekarize.demo.model.Movimentacao;
import com.atividadekarize.demo.model.Produto;
import com.atividadekarize.demo.model.Usuario;

@Repository
@Profile("mysql")
public class RepositorioMysql implements RepositorioAlmoxarifado {

    private final JdbcTemplate jdbc;

    public RepositorioMysql(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Collection<Produto> findAllProdutos() {
        return jdbc.query("SELECT id, nome, quantidade FROM produto",
            (rs, row) -> new Produto(rs.getLong("id"), rs.getString("nome"), rs.getInt("quantidade")));
    }

    public Produto findProduto(Long id) {
        List<Produto> lista = jdbc.query("SELECT id, nome, quantidade FROM produto WHERE id = ?",
            (rs, row) -> new Produto(rs.getLong("id"), rs.getString("nome"), rs.getInt("quantidade")), id);
        return lista.isEmpty() ? null : lista.get(0);
    }

    public Produto adicionarProduto(String nome, int quantidade) {
        jdbc.update("INSERT INTO produto (nome, quantidade) VALUES (?, ?)", nome, quantidade);
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return new Produto(id, nome, quantidade);
    }

    public Movimentacao adicionarMovimentacao(Long produtoId, int quantidade, String usuario, String tipo) {
        LocalDateTime agora = LocalDateTime.now();
        jdbc.update("INSERT INTO movimentacao (produto_id, quantidade, usuario, data_hora, tipo) VALUES (?, ?, ?, ?, ?)",
            produtoId, quantidade, usuario, Timestamp.valueOf(agora), tipo);
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return new Movimentacao(id, produtoId, quantidade, usuario, agora, tipo);
    }

    public List<Movimentacao> findAllMovimentacoes() {
        return jdbc.query("SELECT id, produto_id, quantidade, usuario, data_hora, tipo FROM movimentacao ORDER BY id",
            (rs, row) -> new Movimentacao(
                rs.getLong("id"),
                rs.getLong("produto_id"),
                rs.getInt("quantidade"),
                rs.getString("usuario"),
                rs.getTimestamp("data_hora").toLocalDateTime(),
                rs.getString("tipo") != null ? rs.getString("tipo") : "SAIDA"));
    }

    public boolean atualizarProduto(Long id, String nome, int quantidade) {
        return jdbc.update("UPDATE produto SET nome = ?, quantidade = ? WHERE id = ?", nome, quantidade, id) > 0;
    }

    public boolean removerProduto(Long id) {
        return jdbc.update("DELETE FROM produto WHERE id = ?", id) > 0;
    }

    public boolean retirar(Long produtoId, int quantidade) {
        Produto p = findProduto(produtoId);
        if (p == null || p.getQuantidade() < quantidade) return false;
        return jdbc.update("UPDATE produto SET quantidade = quantidade - ? WHERE id = ? AND quantidade >= ?",
            quantidade, produtoId, quantidade) > 0;
    }

    public Collection<Usuario> findAllUsuarios() {
        return jdbc.query("SELECT id, matricula, nome, tipo FROM usuario",
            (rs, row) -> new Usuario(rs.getLong("id"), rs.getString("matricula"), rs.getString("nome"), rs.getString("tipo")));
    }

    public Usuario findUsuario(Long id) {
        List<Usuario> lista = jdbc.query("SELECT id, matricula, nome, tipo FROM usuario WHERE id = ?",
            (rs, row) -> new Usuario(rs.getLong("id"), rs.getString("matricula"), rs.getString("nome"), rs.getString("tipo")), id);
        return lista.isEmpty() ? null : lista.get(0);
    }

    public Usuario findUsuarioPorMatricula(String matricula) {
        List<Usuario> lista = jdbc.query("SELECT id, matricula, nome, tipo FROM usuario WHERE matricula = ?",
            (rs, row) -> new Usuario(rs.getLong("id"), rs.getString("matricula"), rs.getString("nome"), rs.getString("tipo")), matricula);
        return lista.isEmpty() ? null : lista.get(0);
    }

    public Usuario adicionarUsuario(String matricula, String nome, String tipo) {
        jdbc.update("INSERT INTO usuario (matricula, nome, tipo) VALUES (?, ?, ?)", matricula, nome, tipo);
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return new Usuario(id, matricula, nome, tipo);
    }

    public boolean atualizarUsuario(Long id, String matricula, String nome, String tipo) {
        return jdbc.update("UPDATE usuario SET matricula = ?, nome = ?, tipo = ? WHERE id = ?", matricula, nome, tipo, id) > 0;
    }

    public boolean removerUsuario(Long id) {
        return jdbc.update("DELETE FROM usuario WHERE id = ?", id) > 0;
    }

    public void initDadosIniciais() {
        Integer qtdProdutos = jdbc.queryForObject("SELECT COUNT(*) FROM produto", Integer.class);
        if (qtdProdutos == 0) {
            adicionarProduto("Álcool 70%", 50);
            adicionarProduto("Sabão detergente", 30);
            adicionarProduto("Luvas descartáveis", 100);
            adicionarProduto("Coca-cola", 2);
        }
        Integer qtdUsuarios = jdbc.queryForObject("SELECT COUNT(*) FROM usuario", Integer.class);
        if (qtdUsuarios == 0) {
            adicionarUsuario("1001", "Administrador", "admin");
            adicionarUsuario("2001", "Operador Teste", "operador");
        }
    }
}