package pl.rozprochy.akka.boundary.client;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import pl.rozprochy.akka.model.ImmutablePriceQuery;
import pl.rozprochy.akka.model.NoPrices;
import pl.rozprochy.akka.model.PriceResponse;

public class Client extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final ActorRef server;

    private Client(ActorRef server) {
        this.server = server;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, product -> server.tell(ImmutablePriceQuery.builder().name(product).build(), getSelf()))
                .match(PriceResponse.class, pr -> log.info("Price of `{}` is {}", pr.name(), pr.price()))
                .match(NoPrices.class, np -> log.info("No prices available for product `{}`", np.name()))
                .build();
    }

    public static Props props(ActorRef server) {
        return Props.create(Client.class, server);
    }
}
