package br.com.projeto;

import java.sql.Connection;

public class Teste {
    public static void main(String[] args) {
        Connection con = ConexaoPostgres.conectar();
        if (con != null) {
            System.out.println("✅ Conectado ao PostgreSQL com sucesso!");
        } else {
            System.out.println("❌ Falha na conexão com PostgreSQL.");
        }
    }
}

