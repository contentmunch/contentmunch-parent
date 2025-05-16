package com.contentmunch.foundation.logging;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class DummyLoggingController {
    @PostMapping(value = "/api/dummy/logs", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, String>> test(@RequestBody Map<String, String> body){
        return ResponseEntity.ok(body);
    }

    @GetMapping(value = "/public/health", consumes = "application/json", produces = "application/json")
    public ResponseEntity<String> health(){
        return ResponseEntity.ok("ok");
    }

    @GetMapping(value = "/api/dummy/text")
    public ResponseEntity<String> text(){
        return ResponseEntity.ok("This is a plain text response");
    }

    @GetMapping("/api/dummy/binary")
    public ResponseEntity<byte[]> binary(){
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(new byte[]{0, 1, 2});
    }
}
