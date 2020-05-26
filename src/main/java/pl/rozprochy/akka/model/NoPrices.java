package pl.rozprochy.akka.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableNoPrices.class)
public interface NoPrices extends Product, Quantity {
}