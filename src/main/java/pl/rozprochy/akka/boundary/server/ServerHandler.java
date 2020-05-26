package pl.rozprochy.akka.boundary.server;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import pl.rozprochy.akka.boundary.utils.DBHandler;
import pl.rozprochy.akka.boundary.utils.PriceGenerator;
import pl.rozprochy.akka.model.*;

import java.sql.Statement;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static akka.pattern.Patterns.ask;
import static java.time.temporal.ChronoUnit.MILLIS;

public class ServerHandler extends AbstractActor {

    private final Duration timeout = Duration.of(300, MILLIS);
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final Statement dbStatement;

    public ServerHandler(Statement dbStatement) {
        this.dbStatement = dbStatement;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(InternalPriceQuery.class, priceQuery -> {
                    log.debug("Received a query");
                    final CompletableFuture<Object> q1 = ask(context().actorOf(Props.create(PriceGenerator.class)), priceQuery, timeout).toCompletableFuture();
                    final CompletableFuture<Object> q2 = ask(context().actorOf(Props.create(PriceGenerator.class)), priceQuery, timeout).toCompletableFuture();
                    final CompletableFuture<Object> q = ask(context().actorOf(DBHandler.props(dbStatement)), ImmutablePriceQuery.builder().name(priceQuery.name()).build(), timeout).toCompletableFuture();
                    final AtomicInteger respondValue = new AtomicInteger(-1);
                    final AtomicReference<QueryQuantity> query = new AtomicReference<>();

                    q.whenComplete((res, err) -> {
                        if (Optional.ofNullable(err).isEmpty()) {
                            final QueryQuantity result = (QueryQuantity) res;
                            query.set(result);
                        }
                    });

                    Stream.of(q1, q2, q).forEach(future -> future.whenComplete((res, err) -> {
                        if (Optional.ofNullable(err).isEmpty()) {
                            final InternalPriceResponse result = (InternalPriceResponse) res;
                            if (respondValue.get() > 0) {
                                respondValue.set(Integer.min(result.price(), respondValue.get()));
                            } else {
                                respondValue.set(result.price());
                            }
                        }
                    }));

                    CompletableFuture.allOf(q1, q2).whenComplete((res, err) -> {
                        final int quantity = query.get().quantity();
                        if (respondValue.get() > 0) {
                            priceQuery.sender().tell(ImmutablePriceResponse.builder().name(priceQuery.name()).price(respondValue.get()).quantity(quantity).build(), getSelf());
                        } else {
                            priceQuery.sender().tell(ImmutableNoPrices.builder().name(priceQuery.name()).quantity(quantity).build(), getSelf());
                        }
                    });

                    context().stop(self());
                })
                .build();
    }
}