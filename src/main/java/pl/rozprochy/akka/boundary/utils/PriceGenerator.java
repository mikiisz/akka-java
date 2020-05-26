package pl.rozprochy.akka.boundary.utils;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import pl.rozprochy.akka.model.ImmutableInternalPriceResponse;
import pl.rozprochy.akka.model.InternalPriceQuery;

import java.util.Random;

public class PriceGenerator extends AbstractActor {

    private static Random rand = new Random();
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(InternalPriceQuery.class, priceQuery -> {
                    Thread.sleep(rand.nextInt(400) + 100);
                    final int price = rand.nextInt(10) + 1;
                    log.debug("Generator priced `{}` as {}", priceQuery.name(), price);
                    getSender().tell(ImmutableInternalPriceResponse.builder().name(priceQuery.name()).price(price).build(), getSelf());
                    context().stop(self());
                })
                .build();
    }
}
