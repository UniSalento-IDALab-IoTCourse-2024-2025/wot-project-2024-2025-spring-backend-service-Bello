package bello.antonio.carrier_management_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class ReportService {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * Operating Hours — vista mensile per TUTTI i veicoli refrigerati.
     * Restituisce per ogni veicolo quali giorni del mese avevano dati telemetrici.
     * Risultato: [ { "vehicleName": "Truck1", "activeDays": [1, 3, 5, ...] }, ... ]
     */
    public List<Map<String, Object>> getOperatingHoursMonthlyAll(int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1);

        Date from = Date.from(startDate.atStartOfDay().toInstant(ZoneOffset.UTC));
        Date to = Date.from(endDate.atStartOfDay().toInstant(ZoneOffset.UTC));

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("timestamp").gte(from).lt(to)),
                Aggregation.project("vehicleName")
                        .and(DateOperators.DayOfMonth.dayOfMonth("timestamp")).as("day"),
                Aggregation.group("vehicleName", "day"),
                Aggregation.group("_id.vehicleName")
                        .push("_id.day").as("activeDays"),
                Aggregation.project().and("_id").as("vehicleName").and("activeDays").as("activeDays"),
                Aggregation.sort(org.springframework.data.domain.Sort.Direction.ASC, "vehicleName")
        );

        return mongoTemplate.aggregate(aggregation, "telemetry", Map.class)
                .getMappedResults()
                .stream()
                .map(doc -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("vehicleName", doc.get("vehicleName"));
                    result.put("activeDays", doc.get("activeDays"));
                    return result;
                })
                .toList();
    }

    /**
     * Operating Hours — drill-down ore in un giorno per TUTTI i veicoli.
     * Restituisce per ogni veicolo quali ore del giorno avevano dati.
     * Risultato: [ { "vehicleName": "Truck1", "activeHours": [5, 6, 7, 8, ...] }, ... ]
     */
    public List<Map<String, Object>> getOperatingHoursDailyAll(int year, int month, int day) {
        LocalDate date = LocalDate.of(year, month, day);
        Date from = Date.from(date.atStartOfDay().toInstant(ZoneOffset.UTC));
        Date to = Date.from(date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("timestamp").gte(from).lt(to)),
                Aggregation.project("vehicleName")
                        .and(DateOperators.Hour.hourOf("timestamp")).as("hour"),
                Aggregation.group("vehicleName", "hour"),
                Aggregation.group("_id.vehicleName")
                        .push("_id.hour").as("activeHours"),
                Aggregation.project().and("_id").as("vehicleName").and("activeHours").as("activeHours"),
                Aggregation.sort(org.springframework.data.domain.Sort.Direction.ASC, "vehicleName")
        );

        return mongoTemplate.aggregate(aggregation, "telemetry", Map.class)
                .getMappedResults()
                .stream()
                .map(doc -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("vehicleName", doc.get("vehicleName"));
                    result.put("activeHours", doc.get("activeHours"));
                    return result;
                })
                .toList();
    }

    /**
     * Cumulative Hours — ore totali di funzionamento per veicolo in un periodo.
     * Raggruppa per vehicleName e conta le ore distinte (giorno+ora) con dati.
     * Risultato: [ { "vehicleName": "Truck1", "totalHours": 148 }, ... ]
     */
    public List<Map<String, Object>> getCumulativeHours(Date from, Date to) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("timestamp").gte(from).lt(to)),
                Aggregation.project("vehicleName")
                        .and(DateOperators.Year.yearOf("timestamp")).as("year")
                        .and(DateOperators.Month.monthOf("timestamp")).as("month")
                        .and(DateOperators.DayOfMonth.dayOfMonth("timestamp")).as("day")
                        .and(DateOperators.Hour.hourOf("timestamp")).as("hour"),
                Aggregation.group("vehicleName", "year", "month", "day", "hour"),
                Aggregation.group("_id.vehicleName").count().as("totalHours"),
                Aggregation.project().and("_id").as("vehicleName").and("totalHours").as("totalHours"),
                Aggregation.sort(org.springframework.data.domain.Sort.Direction.ASC, "vehicleName")
        );

        return mongoTemplate.aggregate(aggregation, "telemetry", Map.class)
                .getMappedResults()
                .stream()
                .map(doc -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("vehicleName", doc.get("vehicleName"));
                    result.put("totalHours", doc.get("totalHours"));
                    return result;
                })
                .toList();
    }

    /**
     * Alert Report — numero di anomalie per veicolo raggruppate per anno e mese.
     * Risultato: [ { "vehicleName": "Truck1", "count": 12 }, ... ] ordinato per count DESC
     */
    public List<Map<String, Object>> getAlertReport(String vehicleName, int year,
                                                    Integer month, Integer day) {
        // Build ISO prefix: "2026", "2026-03", or "2026-03-22"
        StringBuilder prefix = new StringBuilder(String.valueOf(year));
        if (month != null) {
            prefix.append("-").append(String.format("%02d", month));
            if (day != null) {
                prefix.append("-").append(String.format("%02d", day));
            }
        }

        Criteria criteria = Criteria.where("timestamp").regex("^" + prefix);
        if (vehicleName != null && !vehicleName.isEmpty()) {
            criteria = criteria.and("vehicleName").is(vehicleName);
        }

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.group("vehicleName").count().as("count"),
                Aggregation.project().and("_id").as("vehicleName").and("count").as("count"),
                Aggregation.sort(org.springframework.data.domain.Sort.Direction.DESC, "count")
        );

        return mongoTemplate.aggregate(aggregation, "notification", Map.class)
                .getMappedResults()
                .stream()
                .map(doc -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("vehicleName", doc.get("vehicleName"));
                    result.put("count", doc.get("count"));
                    return result;
                })
                .toList();
    }
}