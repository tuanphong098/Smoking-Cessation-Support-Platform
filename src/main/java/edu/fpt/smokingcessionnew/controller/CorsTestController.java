package edu.fpt.smokingcessionnew.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller để test CORS và kết nối API
 */
@RestController
@RequestMapping("/api/cors-test")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class CorsTestController {

    @GetMapping("/ping")
    public ResponseEntity<?> ping() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "CORS test successful");
        response.put("timestamp", System.currentTimeMillis());
        response.put("server", "Spring Boot");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/echo")
    public ResponseEntity<?> echo(@RequestBody Map<String, Object> data) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "POST request successful");
        response.put("receivedData", data);
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/options-test", method = RequestMethod.OPTIONS)
    public ResponseEntity<?> optionsTest() {
        return ResponseEntity.ok().build();
    }
}
