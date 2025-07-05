package br.com.projeto;

import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.*;
import org.bson.Document;

import java.time.LocalDateTime;
import java.util.Collections;

public class MongoDB {

    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final MongoCollection<Document> collection;

    public MongoDB() {
        // Configurar conexão - ajuste o host e porta conforme seu MongoDB
        mongoClient = MongoClients.create(
            MongoClientSettings.builder()
                .applyToClusterSettings(builder ->
                    builder.hosts(Collections.singletonList(new ServerAddress("localhost", 27017))))
                .build());

        database = mongoClient.getDatabase("auditoria");
        collection = database.getCollection("logs");
    }

    public void log(String acao, String mensagem) {
        Document log = new Document();
        log.append("acao", acao);
        log.append("mensagem", mensagem);
        log.append("timestamp", LocalDateTime.now().toString());

        collection.insertOne(log);
        System.out.println("Log salvo no MongoDB: " + acao + " - " + mensagem);
    }

    public void close() {
        mongoClient.close();
    }
}
