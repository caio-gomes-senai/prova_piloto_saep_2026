package com.atividadekarize.demo.controller;

import java.util.Collection;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.atividadekarize.demo.model.Movimentacao;
import com.atividadekarize.demo.model.Produto;
import com.atividadekarize.demo.repository.RepositorioMemoria;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api")
public class ControladorApi {

    private final RepositorioMemoria repositorio;

    public ControladorApi(RepositorioMemoria repositorio) {
        this.repositorio = repositorio;
        this.repositorio.initDadosIniciais();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequisicao requisicao, HttpSession session) {
        if (requisicao.usuario == null || requisicao.tipo == null) return ResponseEntity.badRequest().build();
        session.setAttribute("usuario", requisicao.usuario);
        session.setAttribute("tipo", requisicao.tipo);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    public void logout(HttpSession session) { session.invalidate(); }

    @GetMapping("/produtos")
    public Collection<Produto> listarProdutos() { return repositorio.findAllProdutos(); }

    @PostMapping("/produtos")
    public ResponseEntity<?> adicionarProduto(@RequestBody ProdutoRequisicao requisicao, HttpSession session) {
        Object tipo = session.getAttribute("tipo");
        if (tipo == null || !"admin".equals(tipo)) return ResponseEntity.status(403).body("Acesso negado");
        if (requisicao.nome == null) return ResponseEntity.badRequest().build();
        Produto produto = repositorio.adicionarProduto(requisicao.nome, requisicao.quantidade);
        return ResponseEntity.ok(produto);
    }

    @PutMapping("/produtos/{id}")
    public ResponseEntity<?> atualizarProduto(@PathVariable Long id, @RequestBody ProdutoRequisicao requisicao, HttpSession session) {
        Object tipo = session.getAttribute("tipo");
        if (tipo == null || !"admin".equals(tipo)) return ResponseEntity.status(403).body("Acesso negado");
        if (requisicao.nome == null || requisicao.nome.isBlank()) return ResponseEntity.badRequest().body("Nome obrigatório");
        boolean ok = repositorio.atualizarProduto(id, requisicao.nome, requisicao.quantidade);
        if (!ok) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/produtos/{id}")
    public ResponseEntity<?> removerProduto(@PathVariable Long id, HttpSession session) {
        Object tipo = session.getAttribute("tipo");
        if (tipo == null || !"admin".equals(tipo)) return ResponseEntity.status(403).body("Acesso negado");
        boolean ok = repositorio.removerProduto(id);
        if (!ok) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/saida")
    public ResponseEntity<?> registrarSaida(@RequestBody SaidaRequisicao requisicao, HttpSession session) {
        Object usuario = session.getAttribute("usuario");
        if (usuario == null) return ResponseEntity.status(401).body("Usuário não autenticado");
        Produto produto = repositorio.findProduto(requisicao.produtoId);
        if (produto == null) return ResponseEntity.badRequest().body("Produto não encontrado");
        int saldo = produto.getQuantidade();
        if (requisicao.quantidade > saldo) {
            String msg = String.format("Saída não permitida: estoque insuficiente. Disponível: %d. Solicitado: %d.", saldo, requisicao.quantidade);
            return ResponseEntity.badRequest().body(msg);
        }
        boolean ok = repositorio.retirar(requisicao.produtoId, requisicao.quantidade);
        if (!ok) return ResponseEntity.status(500).body("Erro ao registrar saída");
        Movimentacao movimentacao = repositorio.adicionarMovimentacao(requisicao.produtoId, requisicao.quantidade, usuario.toString());
        return ResponseEntity.ok(movimentacao);
    }

    @GetMapping("/movimentacoes")
    public List<Movimentacao> listarMovimentacoes() { return repositorio.findAllMovimentacoes(); }

    public static class LoginRequisicao { public String usuario; public String tipo; }
    public static class ProdutoRequisicao { public String nome; public int quantidade; }
    public static class SaidaRequisicao { public Long produtoId; public int quantidade; }
}
