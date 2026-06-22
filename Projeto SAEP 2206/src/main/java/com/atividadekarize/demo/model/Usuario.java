package com.atividadekarize.demo.model;

public class Usuario {
    private Long id;
    private String matricula;
    private String nome;
    private String tipo;

    public Usuario() {}

    public Usuario(Long id, String matricula, String nome, String tipo) {
        this.id = id;
        this.matricula = matricula;
        this.nome = nome;
        this.tipo = tipo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
}