package uk.co.gresearch.siembol.spark;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.commons.lang3.exception.ExceptionUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import uk.co.gresearch.siembol.alerts.common.AlertingResult;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AlertingSparkResult implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final ObjectWriter JSON_WRITER = new ObjectMapper()
            .writerFor(new TypeReference<Map<String, Object>>() { });
    private static final String ERROR_STATUS_CODE_MSG = "Status code: %s";
    private final int maxResult;
    private int matchesTotal;
    private int exceptionsTotal;
    private final ArrayList<String> matches;
    private final ArrayList<String> exceptions;

    private AlertingSparkResult(int maxResult) {
        this.maxResult = maxResult;
        matchesTotal = 0;
        exceptionsTotal = 0;
        matches = new ArrayList<>();
        exceptions = new ArrayList<>();
    }

    public AlertingSparkResult(AlertingResult alertingResult, int maxResult) {
        this.maxResult = maxResult;
        ArrayList<String> allMatches = new ArrayList<>();
        ArrayList<String> allExceptions = new ArrayList<>();
        if (alertingResult.getStatusCode() != AlertingResult.StatusCode.OK) {
            String exception = alertingResult.getAttributes().getException() == null
                    ? String.format(ERROR_STATUS_CODE_MSG, alertingResult.getStatusCode())
                    : alertingResult.getAttributes().getException();

            allExceptions.add(exception);
        }

        if (alertingResult.getAttributes().getExceptionEvents() != null) {
            alertingResult.getAttributes().getExceptionEvents()
                    .forEach( x -> {
                        try {
                            allExceptions.add(JSON_WRITER.writeValueAsString(x));
                        } catch (JsonProcessingException e) {
                            allExceptions.add(ExceptionUtils.getStackTrace(e));
                        }
                    });
        }

        if (alertingResult.getAttributes().getOutputEvents() != null) {
            alertingResult.getAttributes().getOutputEvents()
                    .forEach( x -> {
                        try {
                            allMatches.add(JSON_WRITER.writeValueAsString(x));
                        } catch (Exception e) {
                            allExceptions.add(ExceptionUtils.getStackTrace(e));
                        }
                    });
        }

        matchesTotal = allMatches.size();
        exceptionsTotal = allExceptions.size();
        matches = allMatches.size() > maxResult
                ? new ArrayList<>(allMatches.subList(0, maxResult))
                : allMatches;

        exceptions = allExceptions.size() > maxResult
                ? new ArrayList<>(allExceptions.subList(0, maxResult))
                : allExceptions;
    }

    public AlertingSparkResult merge(AlertingSparkResult other) {
        matchesTotal += other.matchesTotal;
        exceptionsTotal += other.exceptionsTotal;

        if (!other.matches.isEmpty()
                && matches.size() < maxResult) {
            other.matches.forEach(x -> {
                if (matches.size() < maxResult) {
                    matches.add(x);
                }
            });
        }

        if (!other.exceptions.isEmpty()
                && exceptions.size() < maxResult) {
            other.exceptions.forEach(x -> {
                if (exceptions.size() < maxResult) {
                    exceptions.add(x);
                }
            });
        }

        return this;
    }

    public int getMatchesTotal() {
        return matchesTotal;
    }

    public int getExceptionsTotal() {
        return exceptionsTotal;
    }

    public List<String> getMatches() {
        return matches;
    }

    public List<String> getExceptions() {
        return exceptions;
    }

    public boolean isEmpty() {
        return matchesTotal == 0 && exceptionsTotal == 0;
    }

    public static AlertingSparkResult emptyResult(int maxResult) {
        return new AlertingSparkResult(maxResult);
    }
}
