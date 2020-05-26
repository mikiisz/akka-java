package pl.rozprochy.akka.boundary.server;

import akka.actor.AbstractActor;
import akka.actor.Props;
import pl.rozprochy.akka.model.ImmutableInternalPriceQuery;
import pl.rozprochy.akka.model.PriceQuery;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Server extends AbstractActor {

    private static final String DB_URL = "jdbc:sqlite:akka.db";
    private static final String CREATE_TABLE = "" +
            "CREATE TABLE IF NOT EXISTS " +
            "queries (" +
            "   name TEXT NOT NULL UNIQUE, " +
            "   quantity  INTEGER" +
            ")";

    private final Statement dbStatement;

    public Server() {
        try {
            final Connection dbConnection = DriverManager.getConnection(DB_URL);
            this.dbStatement = dbConnection.createStatement();
            dbStatement.execute(CREATE_TABLE);
        } catch (SQLException e) {
            throw new IllegalStateException("I'm the bad database, duh", e);
        }
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(PriceQuery.class, query -> context()
                        .actorOf(Props.create(ServerHandler.class, dbStatement))
                        .tell(ImmutableInternalPriceQuery.builder().name(query.name()).sender(getSender()).build(), getSelf()))
                .build();
    }
}