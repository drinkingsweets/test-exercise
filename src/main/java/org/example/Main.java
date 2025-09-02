package org.example;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main {

    private static final int MILLIS_IN_MINUTE = 60 * 1000;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yy HH:mm");
    private static final String INPUT_FILE = "tickets.json";
    private static final String ORIGIN_CITY = "Владивосток";
    private static final String DESTINATION_CITY = "Тель-Авив";

    public static void main(String[] args) {
        List<JsonNode> tickets = loadTickets(INPUT_FILE);

        Map<String, List<Integer>> carrierFlightTimes = extractFlightTimes(tickets, ORIGIN_CITY, DESTINATION_CITY);
        Map<String, List<Integer>> carrierPrices = extractPrices(tickets, ORIGIN_CITY, DESTINATION_CITY);

        printMinFlightTimes(carrierFlightTimes);
        printPriceDifference(carrierPrices);
    }

    private static List<JsonNode> loadTickets(String filename) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(new File(filename));
            JsonNode ticketsNode = root.path("tickets");
            if (ticketsNode.isMissingNode() || !ticketsNode.isArray()) {
                throw new RuntimeException("В файле нет массива tickets");
            }
            List<JsonNode> tickets = new ArrayList<>();
            ticketsNode.forEach(tickets::add);
            return tickets;
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при загрузке файла: " + filename, e);
        }
    }

    private static Map<String, List<Integer>> extractFlightTimes(List<JsonNode> tickets, String origin, String destination) {
        Map<String, List<Integer>> flightTimes = new HashMap<>();
        for (JsonNode ticket : tickets) {
            if (!matchesRoute(ticket, origin, destination)) {
                continue;
            }
            String carrier = ticket.path("carrier").asText();

            Date departure = parseDate(ticket.path("departure_date").asText(), ticket.path("departure_time").asText());
            Date arrival = parseDate(ticket.path("arrival_date").asText(), ticket.path("arrival_time").asText());

            int durationMinutes = calculateDurationMinutes(departure, arrival);

            flightTimes.computeIfAbsent(carrier, c -> new ArrayList<>()).add(durationMinutes);
        }
        return flightTimes;
    }

    private static Map<String, List<Integer>> extractPrices(List<JsonNode> tickets, String origin, String destination) {
        Map<String, List<Integer>> prices = new HashMap<>();
        for (JsonNode ticket : tickets) {
            if (!matchesRoute(ticket, origin, destination)) {
                continue;
            }
            String carrier = ticket.path("carrier").asText();
            int price = ticket.path("price").asInt();
            prices.computeIfAbsent(carrier, c -> new ArrayList<>()).add(price);
        }
        return prices;
    }

    private static boolean matchesRoute(JsonNode ticket, String origin, String destination) {
        return origin.equals(ticket.path("origin_name").asText())
                && destination.equals(ticket.path("destination_name").asText());
    }

    private static Date parseDate(String date, String time) {
        try {
            return DATE_FORMAT.parse(date + " " + time);
        } catch (ParseException e) {
            throw new RuntimeException("Ошибка парсинга даты/времени: " + date + " " + time, e);
        }
    }

    private static int calculateDurationMinutes(Date departure, Date arrival) {
        return (int) ((arrival.getTime() - departure.getTime()) / MILLIS_IN_MINUTE);
    }

    private static void printMinFlightTimes(Map<String, List<Integer>> flightTimes) {
        System.out.println("Минимальное время полета (в минутах):");
        flightTimes.forEach((carrier, times) -> {
            int minTime = Collections.min(times);
            System.out.println(carrier + ": " + minTime);
        });
    }

    private static void printPriceDifference(Map<String, List<Integer>> prices) {
        System.out.println("\nРазница между средней ценой и медианой:");
        prices.forEach((carrier, priceList) -> {
            double avg = priceList.stream().mapToInt(i -> i).average().orElse(0.0);
            double med = calculateMedian(priceList);
            System.out.printf("%s: %.2f%n", carrier, avg - med);
        });
    }

    private static double calculateMedian(List<Integer> values) {
        Collections.sort(values);
        int size = values.size();
        if (size == 0) return 0;
        if (size % 2 == 1) {
            return values.get(size / 2);
        } else {
            return (values.get(size / 2 - 1) + values.get(size / 2)) / 2.0;
        }
    }
}