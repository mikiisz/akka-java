package pl.rozprochy.akka.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutablePriceResponse.class)
public interface PriceResponse extends Product, Quantity {
    Integer price();
}