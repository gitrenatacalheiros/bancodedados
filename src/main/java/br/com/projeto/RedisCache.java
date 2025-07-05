package br.com.projeto;

import redis.clients.jedis.Jedis;

public class RedisCache {

    private Jedis jedis;

    public RedisCache() {
        // Conecta no Redis na localhost, porta padrão 6379
        jedis = new Jedis("localhost", 6379);
        System.out.println("Conectado ao Redis");
    }

    // Método para testar a conexão
    public void testarConexao() {
        try {
            String resposta = jedis.ping();
            System.out.println("Resposta do Redis: " + resposta);  // Deve retornar "PONG" se estiver tudo certo
        } catch (Exception e) {
            System.err.println("Erro ao conectar com o Redis:");
            e.printStackTrace();
        }
    }

    // Salvar uma chave e valor no Redis
    public void salvar(String chave, String valor) {
        jedis.set(chave, valor);
    }

    // Recuperar valor por chave do Redis
    public String buscar(String chave) {
        return jedis.get(chave);
    }

    // Fechar conexão
    public void fechar() {
        jedis.close();
    }
}
