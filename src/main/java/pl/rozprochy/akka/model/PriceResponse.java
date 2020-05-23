package pl.rozprochy.akka.model;

import org.immutables.value.Value;

@Value.Immutable
public interface PriceResponse extends Product {
    Integer price();
}