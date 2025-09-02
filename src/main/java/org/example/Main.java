package org.team4;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main {
    private static final int MILLIS_IN_MINUTE = 60 * 1000;
    public static void main(String[] args) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = null;
        try {
            root = mapper.readTree(new File("tickets.json"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        JsonNode tickets = root.path("tickets");

        Map<String, List<Integer>> flightTimes = new HashMap<>();
        Map<String, List<Integer>> prices = new HashMap<>();

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yy HH:mm");

        for (JsonNode ticket : tickets) {
            String origin = ticket.path("origin_name").asText();
            String destination = ticket.path("destination_name").asText();
            if (!origin.equals("Владивосток") || !destination.equals("Тель-Авив")) {
                continue;
            }

            String carrier = ticket.path("carrier").asText();
            String departureStr = ticket.path("departure_date").asText() + " " + ticket.path("departure_time").asText();
            String arrivalStr = ticket.path("arrival_date").asText() + " " + ticket.path("arrival_time").asText();

            Date departure = null;
            Date arrival = null;
            try {
                departure = sdf.parse(departureStr);
                arrival = sdf.parse(arrivalStr);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }

            int durationMinutes = (int) ((arrival.getTime() - departure.getTime()) / MILLIS_IN_MINUTE);

            flightTimes.putIfAbsent(carrier, new ArrayList<>());
            flightTimes.get(carrier).add(durationMinutes);

            int price = ticket.path("price").asInt();
            prices.putIfAbsent(carrier, new ArrayList<>());
            prices.get(carrier).add(price);
        }

        System.out.println("Минимальное время полета (в минутах):");
        for (String carrier : flightTimes.keySet()) {
            int minTime = Collections.min(flightTimes.get(carrier));
            System.out.println(carrier + ": " + minTime);
        }

        System.out.println("\nРазница между средней ценой и медианой:");
        for (String carrier : prices.keySet()) {
            List<Integer> carrierPrices = prices.get(carrier);
            double avg = carrierPrices.stream().mapToInt(i -> i).average().orElse(0.0);
            Collections.sort(carrierPrices);
            double med;
            int size = carrierPrices.size();
            if (size % 2 == 1) {
                med = carrierPrices.get(size / 2);
            } else {
                med = (carrierPrices.get(size / 2 - 1) + carrierPrices.get(size / 2)) / 2.0;
            }
            System.out.printf("%s: %.2f%n", carrier, avg - med);
        }
    }
}