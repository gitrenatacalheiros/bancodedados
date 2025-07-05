package br.com.projeto;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import redis.clients.jedis.Jedis;

public class CRUD {

    private MongoDB logger = new MongoDB();
    private Neo4jService neo4j = new Neo4jService();

    public void inserir(Pessoa pessoa) {
        Pessoa existente = buscarPorCpfComCache(pessoa.getCpf());
        if (existente != null) {
            System.out.println("⚠️ Pessoa com CPF " + pessoa.getCpf() + " já existe: " + existente.getNome());
            logger.log("INSERIR", "Falha: CPF duplicado " + pessoa.getCpf());
            return;
        }

        String sql = "INSERT INTO pessoa (nome, email, cpf, data_nascimento) VALUES (?, ?, ?, ?)";

        try (Connection con = ConexaoPostgres.conectar();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, pessoa.getNome());
            pst.setString(2, pessoa.getEmail());
            pst.setString(3, pessoa.getCpf());
            pst.setDate(4, Date.valueOf(pessoa.getDataNascimento()));

            pst.executeUpdate();
            System.out.println("Pessoa inserida com sucesso!");
            logger.log("INSERIR", "Pessoa inserida: " + pessoa.getNome() + " CPF: " + pessoa.getCpf());

            neo4j.criarOuAtualizarPessoa(pessoa);

            try (Jedis jedis = new Jedis("localhost", 6379)) {
                jedis.del("pessoa:" + pessoa.getCpf());
            } catch (Exception e) {
                System.out.println("Erro ao limpar cache Redis: " + e.getMessage());
                logger.log("CACHE", "Erro ao limpar cache Redis no inserir: " + e.getMessage());
            }

        } catch (SQLException e) {
            e.printStackTrace();
            logger.log("INSERIR", "Erro SQL: " + e.getMessage());
        }
    }

    public Pessoa buscarPorId(int id) {
        String sql = "SELECT * FROM pessoa WHERE id = ?";
        Pessoa pessoa = null;

        try (Connection con = ConexaoPostgres.conectar();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                pessoa = mapearPessoa(rs);
                logger.log("BUSCAR_ID", "Pessoa encontrada ID: " + id);
            } else {
                logger.log("BUSCAR_ID", "Pessoa não encontrada ID: " + id);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            logger.log("BUSCAR_ID", "Erro SQL: " + e.getMessage());
        }

        return pessoa;
    }

    public Pessoa buscarPorCpfComCache(String cpf) {
        Pessoa pessoa = null;

        try (Jedis jedis = new Jedis("localhost", 6379)) {
            String key = "pessoa:" + cpf;
            if (jedis.exists(key)) {
                System.out.println("🔄 Buscando pessoa no cache Redis para CPF: " + cpf);
                String[] dados = jedis.hmget(key, "id", "nome", "email", "cpf", "data_nascimento").toArray(new String[0]);

                pessoa = new Pessoa();
                pessoa.setId(Integer.parseInt(dados[0]));
                pessoa.setNome(dados[1]);
                pessoa.setEmail(dados[2]);
                pessoa.setCpf(dados[3]);
                pessoa.setDataNascimento(dados[4]);

                logger.log("BUSCAR_CPF_CACHE", "Pessoa encontrada no cache CPF: " + cpf);
                return pessoa;
            }
        } catch (Exception e) {
            System.out.println("Erro ao acessar cache Redis: " + e.getMessage());
            logger.log("CACHE", "Erro ao acessar cache Redis: " + e.getMessage());
        }

        String sql = "SELECT * FROM pessoa WHERE cpf = ?";

        try (Connection con = ConexaoPostgres.conectar();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, cpf);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                pessoa = mapearPessoa(rs);
                logger.log("BUSCAR_CPF_BD", "Pessoa encontrada no banco CPF: " + cpf);

                try (Jedis jedis = new Jedis("localhost", 6379)) {
                    String key = "pessoa:" + cpf;
                    jedis.hset(key, "id", String.valueOf(pessoa.getId()));
                    jedis.hset(key, "nome", pessoa.getNome());
                    jedis.hset(key, "email", pessoa.getEmail());
                    jedis.hset(key, "cpf", pessoa.getCpf());
                    jedis.hset(key, "data_nascimento", pessoa.getDataNascimento());
                    jedis.expire(key, 3600);
                } catch (Exception e) {
                    System.out.println("Erro ao salvar no cache Redis: " + e.getMessage());
                    logger.log("CACHE", "Erro ao salvar no cache Redis: " + e.getMessage());
                }
            } else {
                logger.log("BUSCAR_CPF_BD", "Pessoa não encontrada no banco CPF: " + cpf);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            logger.log("BUSCAR_CPF_BD", "Erro SQL: " + e.getMessage());
        }

        return pessoa;
    }

    public List<Pessoa> listarTodas() {
        String sql = "SELECT * FROM pessoa";
        List<Pessoa> pessoas = new ArrayList<>();

        try (Connection con = ConexaoPostgres.conectar();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Pessoa p = mapearPessoa(rs);
                pessoas.add(p);
            }
            logger.log("LISTAR_TODAS", "Listagem de pessoas realizada");

        } catch (SQLException e) {
            e.printStackTrace();
            logger.log("LISTAR_TODAS", "Erro SQL: " + e.getMessage());
        }

        return pessoas;
    }

    public void atualizar(Pessoa pessoa) {
        String sql = "UPDATE pessoa SET nome = ?, email = ?, cpf = ?, data_nascimento = ? WHERE id = ?";

        try (Connection con = ConexaoPostgres.conectar();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, pessoa.getNome());
            pst.setString(2, pessoa.getEmail());
            pst.setString(3, pessoa.getCpf());
            pst.setDate(4, Date.valueOf(pessoa.getDataNascimento()));
            pst.setInt(5, pessoa.getId());

            pst.executeUpdate();
            System.out.println("Pessoa atualizada com sucesso!");
            logger.log("ATUALIZAR", "Pessoa atualizada ID: " + pessoa.getId());

            neo4j.criarOuAtualizarPessoa(pessoa);

            try (Jedis jedis = new Jedis("localhost", 6379)) {
                jedis.del("pessoa:" + pessoa.getCpf());
            } catch (Exception e) {
                System.out.println("Erro ao limpar cache Redis: " + e.getMessage());
                logger.log("CACHE", "Erro ao limpar cache Redis no atualizar: " + e.getMessage());
            }

        } catch (SQLException e) {
            e.printStackTrace();
            logger.log("ATUALIZAR", "Erro SQL: " + e.getMessage());
        }
    }

    public void deletar(int id) {
        Pessoa pessoa = buscarPorId(id);
        if (pessoa == null) {
            System.out.println("Pessoa com id " + id + " não encontrada para deletar.");
            logger.log("DELETAR", "Falha: Pessoa não encontrada ID: " + id);
            return;
        }

        String sql = "DELETE FROM pessoa WHERE id = ?";

        try (Connection con = ConexaoPostgres.conectar();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, id);
            pst.executeUpdate();
            System.out.println("Pessoa deletada com sucesso!");
            logger.log("DELETAR", "Pessoa deletada ID: " + id);

            try (Jedis jedis = new Jedis("localhost", 6379)) {
                jedis.del("pessoa:" + pessoa.getCpf());
            } catch (Exception e) {
                System.out.println("Erro ao limpar cache Redis: " + e.getMessage());
                logger.log("CACHE", "Erro ao limpar cache Redis no deletar: " + e.getMessage());
            }

        } catch (SQLException e) {
            e.printStackTrace();
            logger.log("DELETAR", "Erro SQL: " + e.getMessage());
        }
    }

    public void criarRelacionamento(String cpf1, String cpf2, String tipoRel) {
        try {
            neo4j.criarRelacionamento(cpf1, cpf2, tipoRel);
            logger.log("RELACIONAMENTO", "Criado relacionamento " + tipoRel + " entre " + cpf1 + " e " + cpf2);
        } catch (Exception e) {
            System.out.println("Erro ao criar relacionamento Neo4j: " + e.getMessage());
            logger.log("RELACIONAMENTO", "Erro: " + e.getMessage());
        }
    }

    // ✅ Método auxiliar para converter ResultSet em Pessoa
    private Pessoa mapearPessoa(ResultSet rs) throws SQLException {
        Pessoa pessoa = new Pessoa();
        pessoa.setId(rs.getInt("id"));
        pessoa.setNome(rs.getString("nome"));
        pessoa.setEmail(rs.getString("email"));
        pessoa.setCpf(rs.getString("cpf"));
        Date data = rs.getDate("data_nascimento");
        pessoa.setDataNascimento(data != null ? data.toString() : null);
        return pessoa;
    }
}
