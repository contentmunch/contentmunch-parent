package com.contentmunch.foundation.logging.data;

import java.util.ArrayList;
import java.util.List;

public record LokiStreams(List<LokiEntry> streams) {
    public LokiStreams {
        streams = streams == null ? List.of() : List.copyOf(streams);
    }

    public static LokiStreams from(LokiStream lokiStream, List<LokiLog> lokiLogs) {
        List<LokiEntry> streams = new ArrayList<>();
        lokiLogs.forEach(lokiLog -> {
            var log = new ArrayList<String>();
            var nanos = lokiLog.timestamp() * 1_000_000;

            log.add(String.valueOf(nanos));
            log.add(lokiLog.log());
            streams.add(new LokiEntry(
                    LokiStream.builder()
                            .app(lokiStream.app())
                            .environment(lokiStream.environment())
                            .traceId(lokiLog.traceId())
                            .spanId(lokiLog.spanId())
                            .build(),
                    List.of(log)));
        });
        return new LokiStreams(streams);
    }
}
