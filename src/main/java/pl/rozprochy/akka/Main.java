package pl.rozprochy.akka;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import pl.rozprochy.akka.boundary.client.Client;
import pl.rozprochy.akka.boundary.server.Server;

import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Main {

    private static Scanner reader = new Scanner(System.in);

    public static void main(String... args) {
        final ActorSystem system = ActorSystem.create("system");
        final ActorRef server = system.actorOf(Props.create(Server.class), "server");
        final Map<String, ActorRef> clients = IntStream
                .range(1, 6)
                .boxed()
                .map(i -> String.format("client%s", i))
                .collect(Collectors.toMap(name -> name, name -> system.actorOf(Client.props(server), name)));

        System.out.println("Started app");
        System.out.println("Command pattern: clientN productName");

        while (reader.hasNextLine()) {
            final String[] tokens = reader.nextLine().split(" ");
            if (tokens.length == 2 && clients.containsKey(tokens[0])) {
                final String clientName = tokens[0];
                final String productName = tokens[1];
                final ActorRef client = clients.get(clientName);
                client.tell(productName, client);
            } else {
                System.out.println("Command pattern: clientN productName");
            }
        }
    }
}