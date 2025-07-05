package br.com.projeto;

public class TesteRedis {
    public static void main(String[] args) {
        // Instancia o RedisCache
        RedisCache redisCache = new RedisCache();

        // Testa a conexão com o Redis
        redisCache.testarConexao();

        // Fecha a conexão
        redisCache.fechar();
    }
}
