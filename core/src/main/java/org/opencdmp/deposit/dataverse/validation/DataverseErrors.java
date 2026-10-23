package org.opencdmp.deposit.dataverse.validation;

import gr.cite.tools.exception.MyValidationException;

import java.util.*;
import java.util.regex.Pattern;

/** Turns problems, the plugin's own or the message Dataverse answers with, into one message the user can act on. */
public final class DataverseErrors {

    public static final String INVALID_VALUES = "The repository does not accept some of the dataset's values.";

    private static final String VALIDATION_PREFIX = "Validation Failed:";
    // Dataverse appends the Java objects behind each violation, e.g. "(Invalid value:edu.harvard.iq.dataverse.DatasetFieldValueValue[ id=null ])" and "java.util.stream.ReferencePipeline$3@56266e2d"
    private static final Pattern INVALID_VALUE = Pattern.compile("\\s*\\(Invalid value:[^()]*(\\([^()]*\\))?[^()]*\\)");
    private static final Pattern JAVA_OBJECT = Pattern.compile("\\.?\\s*java\\.util\\.\\S+$");

    private DataverseErrors() {
    }

    public static MyValidationException exception(String intro, Map<String, List<String>> problems) {
        StringBuilder message = new StringBuilder(intro);
        List<Map.Entry<String, List<String>>> errors = new ArrayList<>();
        for (Map.Entry<String, List<String>> problem : problems.entrySet()) {
            for (String text : problem.getValue()) message.append(' ').append(problem.getKey()).append(": ").append(sentence(text));
            errors.add(Map.entry(problem.getKey(), problem.getValue()));
        }
        return new MyValidationException(message.toString(), errors);
    }

    /** The violations of a "Validation Failed: ..." message, one per entry; empty when the message is not one. */
    public static List<String> violations(String message) {
        if (message == null || !message.trim().startsWith(VALIDATION_PREFIX)) return List.of();
        String text = message.trim().substring(VALIDATION_PREFIX.length());
        text = INVALID_VALUE.matcher(text).replaceAll("");
        text = JAVA_OBJECT.matcher(text.trim()).replaceAll("");
        List<String> violations = new ArrayList<>();
        for (String part : text.split("\\.\\s*,\\s*")) {
            String violation = part.replaceAll("\\s+", " ").trim();
            if (violation.endsWith(".")) violation = violation.substring(0, violation.length() - 1);
            if (!violation.isEmpty() && !violations.contains(violation)) violations.add(violation);
        }
        return violations;
    }

    /** What Dataverse answered: its validation violations, or its message as it is. */
    public static String describe(String message) {
        List<String> violations = violations(message);
        if (violations.isEmpty()) return message;
        StringBuilder text = new StringBuilder(INVALID_VALUES);
        for (String violation : violations) text.append(' ').append(sentence(violation));
        return text.toString();
    }

    private static String sentence(String text) {
        return text.endsWith(".") ? text : text + ".";
    }
}
