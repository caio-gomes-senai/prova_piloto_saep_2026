package com.atividadekarize.demo.model;

import java.time.LocalDateTime;

public class Movimentacao {
    private Long id;
    private Long produtoId;
    private int quantidade;
    private String usuario;
    private LocalDateTime dataHora;

    public Movimentacao() {}

    public Movimentacao(Long id, Long produtoId, int quantidade, String usuario, LocalDateTime dataHora) {
        this.id = id;
        this.produtoId = produtoId;
        this.quantidade = quantidade;
        this.usuario = usuario;
        this.dataHora = dataHora;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProdutoId() { return produtoId; }
    public void setProdutoId(Long produtoId) { this.produtoId = produtoId; }

    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
}
