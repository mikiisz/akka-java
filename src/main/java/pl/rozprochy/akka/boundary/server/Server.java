package pl.rozprochy.akka.boundary.server;

import akka.actor.AbstractActor;
import akka.actor.Props;
import pl.rozprochy.akka.model.ImmutableInternalPriceQuery;
import pl.rozprochy.akka.model.PriceQuery;

public class Server extends AbstractActor {
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(PriceQuery.class, query -> context()
                        .actorOf(Props.create(ServerHandler.class))
                        .tell(ImmutableInternalPriceQuery.builder().name(query.name()).sender(getSender()).build(), getSelf()))
                .build();
    }
}