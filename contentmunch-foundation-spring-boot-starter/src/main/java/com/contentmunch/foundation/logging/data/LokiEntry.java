package com.contentmunch.foundation.logging.data;

import java.util.List;

public record LokiEntry(LokiStream stream, List<List<String>> values) {
    public LokiEntry {
        values = values == null ? List.of() : List.copyOf(values);
    }
}
