package pl.rozprochy.akka.boundary.server;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.stream.Materializer;
import org.jsoup.Jsoup;
import pl.rozprochy.akka.model.ImmutablePriceQuery;
import pl.rozprochy.akka.model.NoPrices;
import pl.rozprochy.akka.model.PriceQuery;
import pl.rozprochy.akka.model.PriceResponse;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

import static akka.http.javadsl.server.PathMatchers.segment;
import static akka.pattern.Patterns.ask;
import static java.time.temporal.ChronoUnit.SECONDS;

public class ServerHttp extends AllDirectives {

    private final ActorRef server;
    private final ActorSystem system;
    private final Materializer materializer;
    private final Duration timeout = Duration.of(1, SECONDS);

    public ServerHttp(ActorRef server, ActorSystem system, Materializer materializer) {
        this.server = server;
        this.system = system;
        this.materializer = materializer;
    }

    public Route route() {
        return concat(
                path(segment("price").slash(segment()), name ->
                        get(() -> {
                            final PriceQuery priceQuery = ImmutablePriceQuery.builder().name(name).build();
                            final CompletionStage<Object> query = ask(server, priceQuery, timeout)
                                    .exceptionally(PriceResponse.class::cast)
                                    .exceptionally(NoPrices.class::cast);
                            return completeOKWithFuture(query, Jackson.marshaller());
                        })
                ),
                path(segment("review").slash(segment()), name ->
                        get(() -> {
                            final CompletionStage<Object> query = Http.get(system)
                                    .singleRequest(HttpRequest.create("https://www.opineo.pl/?szukaj=" + name + "&s=2"))
                                    .thenCompose(response -> response.entity().toStrict(timeout.get(SECONDS), materializer))
                                    .thenApply(entity -> Jsoup.parse(entity.getData().utf8String())
                                            .body()
                                            .getElementById("page")
                                            .getElementById("content")
                                            .getElementById("screen")
                                            .getElementsByClass("pls")
                                            .get(0)
                                            .getElementsByClass("shl_i pl_i")
                                            .get(0)
                                            .getElementsByClass("pl_attr")
                                            .get(0)
                                            .getElementsByTag("li")
                                            .eachText()
                                            .toArray()
                                    );
                            return completeOKWithFuture(query, Jackson.marshaller());
                        })
                )
        );
    }
}