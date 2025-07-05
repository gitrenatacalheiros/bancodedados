package br.com.projeto;

import org.neo4j.driver.*;

public class Neo4jService implements AutoCloseable {

    private final Driver driver;

    public Neo4jService() {
        // Obtém a senha da variável de ambiente
        String senha = System.getenv("NEO4J_PASSWORD");

        if (senha == null || senha.isBlank()) {
            throw new RuntimeException("❌ A variável de ambiente NEO4J_PASSWORD não está definida!");
        }

        // Conecta ao Neo4j usando a senha segura
        driver = GraphDatabase.driver("neo4j://localhost:7687", AuthTokens.basic("neo4j", senha));
    }

    // Cria ou atualiza um nó Pessoa com propriedades básicas
    public void criarOuAtualizarPessoa(Pessoa pessoa) {
        try (Session session = driver.session()) {
            String cypher = "MERGE (p:Pessoa {cpf: $cpf}) " +
                            "SET p.nome = $nome, p.email = $email, p.dataNascimento = $dataNascimento";

            session.executeWrite(tx -> {
                tx.run(cypher,
                    Values.parameters(
                        "cpf", pessoa.getCpf(),
                        "nome", pessoa.getNome(),
                        "email", pessoa.getEmail(),
                        "dataNascimento", pessoa.getDataNascimento()
                    ));
                return null;
            });
            System.out.println("✅ Pessoa sincronizada no Neo4j: " + pessoa.getNome() + " (CPF: " + pessoa.getCpf() + ")");
        } catch (Exception e) {
            System.err.println("❌ Erro ao criar/atualizar pessoa no Neo4j: " + e.getMessage());
        }
    }

    // Cria relacionamento entre duas pessoas baseado em CPF e tipo de relacionamento seguro
    public void criarRelacionamento(String cpf1, String cpf2, String tipoRel) {
        try (Session session = driver.session()) {
            String tipoSeguro = validarTipoRel(tipoRel);
            String cypher = "MATCH (a:Pessoa {cpf: $cpf1}), (b:Pessoa {cpf: $cpf2}) " +
                            "MERGE (a)-[r:" + tipoSeguro + "]->(b)";

            session.executeWrite(tx -> {
                tx.run(cypher,
                    Values.parameters(
                        "cpf1", cpf1,
                        "cpf2", cpf2
                    ));
                return null;
            });
            System.out.println("✅ Relacionamento " + tipoSeguro + " criado entre " + cpf1 + " e " + cpf2);
        } catch (Exception e) {
            System.err.println("❌ Erro ao criar relacionamento no Neo4j: " + e.getMessage());
        }
    }

    // Valida e limita os tipos de relacionamento para evitar injeção Cypher
    private String validarTipoRel(String tipoRel) {
        return switch (tipoRel.toUpperCase()) {
            case "AMIGO", "FAMILIAR", "COLEGA", "CONHECIDO" -> tipoRel.toUpperCase();
            default -> "RELACIONADO";
        };
    }

    @Override
    public void close() {
        driver.close();
    }
}
