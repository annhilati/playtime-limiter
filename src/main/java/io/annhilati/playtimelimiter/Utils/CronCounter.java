package io.annhilati.playtimelimiter.Utils;

import com.cronutils.model.Cron;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.cronutils.model.CronType;

import java.time.*;
import java.util.Optional;

public class CronCounter {

    /**
     * Zählt, wie oft der Cron-Expression zwischen from (exklusive) und to
     * (inklusive) ausführt.
     * Unterstützt Unix (5 Felder) und Quartz (6-7 Felder). Nutzt System-Default
     * Zone.
     */
    public static int countOccurrences(Instant from, Instant to, String cronExpression) {
        return countOccurrences(from, to, cronExpression, ZoneId.systemDefault());
    }

    /**
     * Gleiche Funktion, erlaubt explizite ZoneId.
     */
    public static int countOccurrences(Instant from, Instant to, String cronExpression, ZoneId zone) {
        if (from.isAfter(to))
            return 0;

        String expr = cronExpression.trim();
        String[] parts = expr.split("\\s+");
        CronType cronType = (parts.length == 5) ? CronType.UNIX : CronType.QUARTZ;

        CronDefinition definition = CronDefinitionBuilder.instanceDefinitionFor(cronType);
        CronParser parser = new CronParser(definition);
        Cron cron;
        try {
            cron = parser.parse(expr);
            cron.validate();
        } catch (Exception e) {
            throw new IllegalArgumentException("Ungültige Cron-Expression: " + e.getMessage(), e);
        }

        ExecutionTime executionTime = ExecutionTime.forCron(cron);

        ZonedDateTime cursor = from.atZone(zone);
        ZonedDateTime endZdt = to.atZone(zone);

        Optional<ZonedDateTime> nextOpt = executionTime.nextExecution(cursor);

        int count = 0;
        final int MAX_ITER = 5_000_000; // Schutz gegen endloses Durchlaufen
        int iter = 0;

        while (nextOpt.isPresent()) {
            if (++iter > MAX_ITER) {
                throw new IllegalStateException(
                        "Zu viele Treffer (>" + MAX_ITER + "). Abbruch zum Schutz vor Endlosschleife.");
            }
            ZonedDateTime occ = nextOpt.get();
            if (occ.isAfter(endZdt))
                break;
            count++;
            // nächstes Ereignis nach current occurrence
            nextOpt = executionTime.nextExecution(occ);
        }

        return count;
    }
}
