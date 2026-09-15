package com.wovenreviews.preflight;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The two checks the frontend needs to prove the chain is wired up. */
@RestController
@RequestMapping("/api/v1/utils")
class UtilsController {

    private final PingRepository pings;

    UtilsController(PingRepository pings) {
        this.pings = pings;
    }

    /** The API process is answering. Says nothing about the database. */
    @GetMapping("/health-check/")
    boolean healthCheck() {
        return true;
    }

    /**
     * Round-trips to Postgres, so a green answer means the whole chain works rather than just the
     * API being up: the query goes through JPA against the table Flyway migrated.
     */
    @GetMapping("/db-check/")
    Map<String, String> dbCheck() {
        pings.count();
        return Map.of("database", "ok");
    }
}
