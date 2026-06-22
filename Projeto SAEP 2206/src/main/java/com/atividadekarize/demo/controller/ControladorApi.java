package com.atividadekarize.demo.controller;

import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.atividadekarize.demo.model.Usuario;
import com.atividadekarize.demo.repository.RepositorioAlmoxarifado;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api")
public class ControladorApi {

    private static final Logger log = LoggerFactory.getLogger(ControladorApi.class);
    private final RepositorioAlmoxarifado repositorio;

    public ControladorApi(RepositorioAlmoxarifado repositorio) {
        this.repositorio = repositorio;
        this.repositorio.initDadosIniciais();
        log.info("Banco em uso: {}", repositorio.getClass().getSimpleName());
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequisicao requisicao, HttpSession session) {
        if (requisicao.matricula == null || requisicao.matricula.isBlank()) return ResponseEntity.badRequest().body("Informe a matrícula");
        Usuario usuario = repositorio.findUsuarioPorMatricula(requisicao.matricula.trim());
        if (usuario == null) return ResponseEntity.status(401).body("Matrícula não encontrada");
        session.setAttribute("usuario", usuario.getNome());
        session.setAttribute("matricula", usuario.getMatricula());
        session.setAttribute("tipo", usuario.getTipo());
        return ResponseEntity.ok(usuario);
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
        Object admin = session.getAttribute("usuario");
        repositorio.adicionarMovimentacao(produto.getId(), requisicao.quantidade, admin.toString(), "ENTRADA");
        return ResponseEntity.ok(produto);
    }

    @PutMapping("/produtos/{id}")
    public ResponseEntity<?> atualizarProduto(@PathVariable Long id, @RequestBody ProdutoRequisicao requisicao, HttpSession session) {
        Object tipo = session.getAttribute("tipo");
        if (tipo == null || !"admin".equals(tipo)) return ResponseEntity.status(403).body("Acesso negado");
        if (requisicao.nome == null || requisicao.nome.isBlank()) return ResponseEntity.badRequest().body("Nome obrigatório");
        Produto antigo = repositorio.findProduto(id);
        if (antigo == null) return ResponseEntity.notFound().build();
        boolean ok = repositorio.atualizarProduto(id, requisicao.nome, requisicao.quantidade);
        if (!ok) return ResponseEntity.notFound().build();
        if (requisicao.quantidade > antigo.getQuantidade()) {
            int adicionado = requisicao.quantidade - antigo.getQuantidade();
            Object admin = session.getAttribute("usuario");
            repositorio.adicionarMovimentacao(id, adicionado, admin.toString(), "ENTRADA");
        }
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
        Movimentacao movimentacao = repositorio.adicionarMovimentacao(requisicao.produtoId, requisicao.quantidade, usuario.toString(), "SAIDA");
        return ResponseEntity.ok(movimentacao);
    }

    @GetMapping("/movimentacoes")
    public List<Movimentacao> listarMovimentacoes() { return repositorio.findAllMovimentacoes(); }

    @GetMapping("/usuarios")
    public ResponseEntity<?> listarUsuarios(HttpSession session) {
        if (!isAdmin(session)) return ResponseEntity.status(403).body("Acesso negado");
        return ResponseEntity.ok(repositorio.findAllUsuarios());
    }

    @PostMapping("/usuarios")
    public ResponseEntity<?> adicionarUsuario(@RequestBody UsuarioRequisicao requisicao, HttpSession session) {
        if (!isAdmin(session)) return ResponseEntity.status(403).body("Acesso negado");
        if (requisicao.matricula == null || requisicao.matricula.isBlank()) return ResponseEntity.badRequest().body("Matrícula obrigatória");
        if (requisicao.nome == null || requisicao.nome.isBlank()) return ResponseEntity.badRequest().body("Nome obrigatório");
        if (requisicao.tipo == null || (!"admin".equals(requisicao.tipo) && !"operador".equals(requisicao.tipo))) return ResponseEntity.badRequest().body("Tipo inválido");
        if (repositorio.findUsuarioPorMatricula(requisicao.matricula.trim()) != null) return ResponseEntity.badRequest().body("Matrícula já cadastrada");
        Usuario usuario = repositorio.adicionarUsuario(requisicao.matricula.trim(), requisicao.nome.trim(), requisicao.tipo);
        return ResponseEntity.ok(usuario);
    }

    @PutMapping("/usuarios/{id}")
    public ResponseEntity<?> atualizarUsuario(@PathVariable Long id, @RequestBody UsuarioRequisicao requisicao, HttpSession session) {
        if (!isAdmin(session)) return ResponseEntity.status(403).body("Acesso negado");
        if (requisicao.matricula == null || requisicao.matricula.isBlank()) return ResponseEntity.badRequest().body("Matrícula obrigatória");
        if (requisicao.nome == null || requisicao.nome.isBlank()) return ResponseEntity.badRequest().body("Nome obrigatório");
        if (requisicao.tipo == null || (!"admin".equals(requisicao.tipo) && !"operador".equals(requisicao.tipo))) return ResponseEntity.badRequest().body("Tipo inválido");
        Usuario existente = repositorio.findUsuarioPorMatricula(requisicao.matricula.trim());
        if (existente != null && !existente.getId().equals(id)) return ResponseEntity.badRequest().body("Matrícula já cadastrada");
        boolean ok = repositorio.atualizarUsuario(id, requisicao.matricula.trim(), requisicao.nome.trim(), requisicao.tipo);
        if (!ok) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<?> removerUsuario(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) return ResponseEntity.status(403).body("Acesso negado");
        boolean ok = repositorio.removerUsuario(id);
        if (!ok) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().build();
    }

    private boolean isAdmin(HttpSession session) {
        Object tipo = session.getAttribute("tipo");
        return tipo != null && "admin".equals(tipo);
    }

    public static class LoginRequisicao { public String matricula; }
    public static class ProdutoRequisicao { public String nome; public int quantidade; }
    public static class SaidaRequisicao { public Long produtoId; public int quantidade; }
    public static class UsuarioRequisicao { public String matricula; public String nome; public String tipo; }
}
