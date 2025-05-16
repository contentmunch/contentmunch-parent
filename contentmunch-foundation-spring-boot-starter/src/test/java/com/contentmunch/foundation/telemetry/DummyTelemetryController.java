package com.contentmunch.foundation.telemetry;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dummy/telemetry")
class DummyTelemetryController {
    @GetMapping("/ok")
    public String ok(){
        return "ok";
    }
}
