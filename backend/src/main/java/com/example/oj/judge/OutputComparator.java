package com.example.oj.judge;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class OutputComparator {
    public boolean matches(String expected, String actual, String compareMode) {
        if ("strict".equals(compareMode)) {
            return expected.equals(actual);
        }
        return normalizeIgnoreTrailingSpace(expected).equals(normalizeIgnoreTrailingSpace(actual));
    }

    private String normalizeIgnoreTrailingSpace(String value) {
        String normalized = value == null ? "" : value.replace("\r\n", "\n").replace('\r', '\n');
        return Arrays.stream(normalized.split("\n", -1))
                .map(line -> line.replaceFirst("\\s+$", ""))
                .collect(Collectors.joining("\n"))
                .replaceFirst("\\s+$", "");
    }
}
