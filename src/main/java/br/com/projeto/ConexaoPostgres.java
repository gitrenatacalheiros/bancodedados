package br.com.projeto;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;
import java.io.InputStream;

public class ConexaoPostgres {

    public static Connection conectar() {
        try {
            Properties props = new Properties();

            // Lê o arquivo config.properties
            InputStream input = ConexaoPostgres.class.getClassLoader().getResourceAsStream("config.properties");

            if (input == null) {
                System.out.println("❌ Arquivo config.properties não encontrado no classpath!");
                return null;
            }

            props.load(input);

            String url = props.getProperty("postgres.url");
            String usuario = props.getProperty("postgres.usuario");
            String senha = props.getProperty("postgres.senha");

            if (url == null || usuario == null || senha == null) {
                System.out.println("❌ Propriedades do arquivo config.properties estão incompletas!");
                return null;
            }

            System.out.println("🌐 URL: " + url);
            System.out.println("👤 Usuário: " + usuario);

            Class.forName("org.postgresql.Driver");

            Connection conexao = DriverManager.getConnection(url, usuario, senha);
            System.out.println("✅ Conexão com PostgreSQL estabelecida com sucesso!");

            return conexao;

        } catch (Exception e) {
            System.out.println("❌ Erro ao conectar com PostgreSQL:");
            e.printStackTrace();
            return null;
        }
    }
}
