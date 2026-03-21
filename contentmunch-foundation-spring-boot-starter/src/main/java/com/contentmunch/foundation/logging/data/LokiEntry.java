package com.contentmunch.foundation.logging.data;

import java.util.List;

public record LokiEntry(LokiStream stream, List<LokiLog> values) {
    public LokiEntry {
        values = values == null ? List.of() : List.copyOf(values);
    }
}
