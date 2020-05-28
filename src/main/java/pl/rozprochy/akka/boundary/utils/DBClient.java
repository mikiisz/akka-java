package pl.rozprochy.akka.boundary.utils;

import akka.actor.AbstractActor;
import akka.actor.Props;
import pl.rozprochy.akka.model.ImmutableQuantityQuery;
import pl.rozprochy.akka.model.PriceQuery;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.OptionalInt;

public class DBClient extends AbstractActor {

    private final static String QUANTITY = "quantity";
    private final Statement dbStatement;

    public DBClient(Statement dbStatement) {
        this.dbStatement = dbStatement;
    }

    private void insertQuantity(String name) {
        final String query = buildInsertQuery(name);
        try {
            dbStatement.execute(query);
        } catch (SQLException e) {
            throw new IllegalStateException("I have SQLite", e);
        }
    }

    private void updateQuantity(String name) {
        final String query = buildUpdateQuery(name);
        try {
            dbStatement.execute(query);
        } catch (SQLException e) {
            throw new IllegalStateException("What a beautiful day", e);
        }
    }

    private OptionalInt getQuantity(String name) {
        final String query = buildGetQuery(name);
        try {
            final ResultSet res = dbStatement.executeQuery(query);
            if (!res.next()) {
                return OptionalInt.empty();
            } else {
                ;
                return OptionalInt.of(res.getInt(QUANTITY));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Wrong statement", e);
        }
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(PriceQuery.class, query -> {
                    final OptionalInt q = getQuantity(query.name());
                    if (q.isPresent()) {
                        updateQuantity(query.name());
                        getSender().tell(ImmutableQuantityQuery.builder().name(query.name()).quantity(q.getAsInt()).build(), getSelf());
                    } else {
                        insertQuantity(query.name());
                        getSender().tell(ImmutableQuantityQuery.builder().name(query.name()).quantity(0).build(), getSelf());
                    }
                    context().stop(self());
                })
                .build();
    }

    public static Props props(Statement dbStatement) {
        return Props.create(DBClient.class, dbStatement);
    }

    private static String buildGetQuery(String name) {
        return String.format("SELECT quantity FROM queries WHERE name = \"%s\"", name);
    }

    private static String buildUpdateQuery(String name) {
        return String.format("UPDATE queries SET quantity = quantity + 1 WHERE name = \"%s\"", name);
    }

    private static String buildInsertQuery(String name) {
        return String.format("INSERT INTO queries (name, quantity) values (\"%s\", 1)", name);
    }
}
