package pl.rozprochy.akka.model;

import org.immutables.value.Value;

@Value.Immutable
public interface InternalPriceResponse extends Product {
    Integer price();
}