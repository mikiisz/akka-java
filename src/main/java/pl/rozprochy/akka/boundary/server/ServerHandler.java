package pl.rozprochy.akka.boundary.server;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import pl.rozprochy.akka.model.ImmutableNoPrices;
import pl.rozprochy.akka.model.ImmutablePriceResponse;
import pl.rozprochy.akka.model.InternalPriceQuery;
import pl.rozprochy.akka.model.PriceResponse;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static akka.pattern.Patterns.ask;
import static java.time.temporal.ChronoUnit.MILLIS;

public class ServerHandler extends AbstractActor {

    private final Duration timeout = Duration.of(300, MILLIS);
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(InternalPriceQuery.class, priceQuery -> {
                    log.debug("Received a query");
                    final CompletableFuture<Object> q1 = ask(context().actorOf(Props.create(PriceGenerator.class)), priceQuery, timeout).toCompletableFuture();
                    final CompletableFuture<Object> q2 = ask(context().actorOf(Props.create(PriceGenerator.class)), priceQuery, timeout).toCompletableFuture();
                    final AtomicInteger respondValue = new AtomicInteger(-1);

                    Stream.of(q1, q2).forEach(future -> future.whenComplete((res, err) -> {
                        if (err == null) {
                            final PriceResponse result = (PriceResponse) res;
                            if (respondValue.get() > 0) {
                                respondValue.set(Integer.min(result.price(), respondValue.get()));
                            } else {
                                respondValue.set(result.price());
                            }
                        }
                    }));

                    CompletableFuture.allOf(q1, q2).whenComplete((res, err) -> {
                        if (respondValue.get() > 0) {
                            priceQuery.sender().tell(ImmutablePriceResponse.builder().name(priceQuery.name()).price(respondValue.get()).build(), getSelf());
                        } else {
                            priceQuery.sender().tell(ImmutableNoPrices.builder().name(priceQuery.name()).build(), getSelf());
                        }
                    });

                    context().stop(self());
                })
                .build();
    }
}