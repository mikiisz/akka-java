package pl.rozprochy.akka.model;

import akka.actor.ActorRef;
import org.immutables.value.Value;

@Value.Immutable
public interface InternalPriceQuery extends Product {
    ActorRef sender();
}