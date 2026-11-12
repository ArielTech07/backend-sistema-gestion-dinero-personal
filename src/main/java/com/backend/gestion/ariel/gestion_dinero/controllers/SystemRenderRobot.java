package com.backend.gestion.ariel.gestion_dinero.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SystemRenderRobot {

    @GetMapping("/ping")
    public ResponseEntity<?> render() {
        return ResponseEntity.ok("CXDD");
    }
}
