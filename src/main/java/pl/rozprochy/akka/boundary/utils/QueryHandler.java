package pl.rozprochy.akka.boundary.utils;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
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

public class QueryHandler extends AbstractActor {

    private final Duration timeout = Duration.of(300, MILLIS);
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final Statement dbStatement;

    public QueryHandler(Statement dbStatement) {
        this.dbStatement = dbStatement;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(InternalPriceQuery.class, priceQuery -> {
                    log.debug("Received a query");
                    final CompletableFuture<Object> q1 = ask(context().actorOf(Props.create(PricingClient.class)), priceQuery, timeout).toCompletableFuture();
                    final CompletableFuture<Object> q2 = ask(context().actorOf(Props.create(PricingClient.class)), priceQuery, timeout).toCompletableFuture();
                    final CompletableFuture<Object> q3 = ask(context().actorOf(DBClient.props(dbStatement)), ImmutablePriceQuery.builder().name(priceQuery.name()).build(), timeout).toCompletableFuture();

                    final AtomicInteger respondValue = new AtomicInteger(-1);
                    final AtomicReference<QuantityQuery> query = new AtomicReference<>(ImmutableQuantityQuery.builder().name(priceQuery.name()).quantity(-1).build());

                    q3.whenComplete((res, err) -> {
                        if (Optional.ofNullable(err).isEmpty()) {
                            final QuantityQuery result = (QuantityQuery) res;
                            query.set(result);
                        }
                    });

                    Stream.of(q1, q2, q3).forEach(future -> future.whenComplete((res, err) -> {
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