package com.contentmunch.foundation.logging.data;

import java.util.List;

public record LokiStreams(List<LokiEntry> streams) {
    public LokiStreams {
        streams = streams == null ? List.of() : List.copyOf(streams);
    }

    public static LokiStreams from(LokiStream lokiStream,List<LokiLog> lokiLogs){
        return new LokiStreams(List.of(new LokiEntry(lokiStream, lokiLogs)));
    }
}
