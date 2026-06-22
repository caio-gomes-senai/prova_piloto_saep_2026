# Almoxarifado - Atividade

Projeto simples em Spring Boot com frontend estático em HTML/CSS/JS.

## Como executar

1. Tenha Java 17+ instalado.
2. Na pasta do projeto rode:

```powershell
.\mvnw.cmd spring-boot:run
```

Se a máquina não tiver Java configurado, você pode usar o JDK local que já está dentro do projeto:

```powershell
set "JAVA_HOME=%CD%\src\main\java\com\atividadekarize\demo\repository\oracleJdk-26"
set "PATH=%JAVA_HOME%\bin;%PATH%"
.\mvnw.cmd spring-boot:run
```

Se você tiver Maven instalado no sistema, pode também usar:

```powershell
mvn spring-boot:run
```

3. Abra no navegador:

http://localhost:8080

## Funcionalidades implementadas

- Login simples em português com tipo de usuário `operador` ou `admin`.
- Listar produtos e saldos.
- Registrar saída: a operação é bloqueada se a quantidade solicitada for maior que o saldo.
- Mensagem de erro clara quando estoque insuficiente: `Saída não permitida: estoque insuficiente. Disponível: X. Solicitado: Y.`
- Registrar movimentações com data/hora e usuário responsável.
- Administrador pode cadastrar novos produtos.
"# Gaios-sistema-de-estoque" 
