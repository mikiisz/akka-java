package pl.rozprochy.akka;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.stream.Materializer;
import akka.stream.javadsl.Flow;
import pl.rozprochy.akka.boundary.client.Client;
import pl.rozprochy.akka.boundary.server.Server;
import pl.rozprochy.akka.boundary.server.ServerHttp;

import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static akka.actor.ActorRef.noSender;

public class Main {

    private static Scanner reader = new Scanner(System.in);

    public static void main(String... args) {
        final ActorSystem system = ActorSystem.create("system");
        final Materializer materializer = Materializer.createMaterializer(system);
        final ActorRef server = system.actorOf(Props.create(Server.class), "server");
        final ServerHttp serverHttp = new ServerHttp(server, system, materializer);
        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = serverHttp.route().flow(system, materializer);
        final Http http = Http.get(system);

        http.bindAndHandle(routeFlow, ConnectHttp.toHost("localhost", 8080), materializer);

        final Map<String, ActorRef> clients = IntStream
                .range(1, 6)
                .boxed()
                .map(i -> String.format("client%s", i))
                .collect(Collectors.toMap(name -> name, name -> system.actorOf(Client.props(server), name)));

        System.out.println("Started app");
        System.out.println("Http server: http://localhost:8080/");
        System.out.println("Command pattern: clientN productName");

        while (reader.hasNextLine()) {
            final String[] tokens = reader.nextLine().split(" ");
            if (tokens.length == 2 && clients.containsKey(tokens[0])) {
                final String clientName = tokens[0];
                final String productName = tokens[1];
                final ActorRef client = clients.get(clientName);
                client.tell(productName, noSender());
            } else {
                System.out.println("Command pattern: clientN productName");
            }
        }
    }
}