package br.com.projeto;

import java.util.List;

public class TesteCRUD {

    public static void main(String[] args) {
        CRUD crud = new CRUD();

        System.out.println("\n🟢 Inserindo pessoas no PostgreSQL e registrando logs no MongoDB:");

        Pessoa[] pessoas = new Pessoa[2];

        pessoas[0] = new Pessoa(0, "Renata Souza", "renata.souza@example.com", "11111111111", "1995-01-01");
        pessoas[1] = new Pessoa(0, "Bruno Lima", "bruno.lima@example.com", "22222222222", "1988-05-12");

        try (Neo4jService neo4j = new Neo4jService()) {

            for (Pessoa p : pessoas) {
                if (crud.buscarPorCpfComCache(p.getCpf()) == null) {
                    crud.inserir(p);
                }

                // Sincroniza a pessoa no Neo4j também
                neo4j.criarOuAtualizarPessoa(p);
            }

            System.out.println("\n🔗 Criando relacionamento no Neo4j:");
            neo4j.criarRelacionamento("11111111111", "22222222222", "amizade");

        } catch (Exception e) {
            System.err.println("❌ Erro ao interagir com o Neo4j: " + e.getMessage());
        }

        System.out.println("\n📋 Listando todas as pessoas no PostgreSQL:");
        List<Pessoa> lista = crud.listarTodas();
        for (Pessoa p : lista) {
            System.out.println(p.getId() + ": " + p.getNome() + " - CPF: " + p.getCpf());
        }

        System.out.println("\n🔍 Testando busca com cache Redis:");
        Pessoa buscada = crud.buscarPorCpfComCache("11111111111");
        if (buscada != null) {
            System.out.println("Encontrada no sistema: " + buscada.getNome() + " - " + buscada.getEmail());
        }

        System.out.println("\n✅ Teste finalizado com sucesso!");
    }
}
