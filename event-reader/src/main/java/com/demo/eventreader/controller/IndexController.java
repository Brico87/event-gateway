package com.demo.eventreader.controller;

import com.google.common.net.HttpHeaders;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

    private final String landingUrl;

    @Autowired
    public IndexController(@Value("${springdoc.swagger-ui.path}") String landingUrl) {
        this.landingUrl = landingUrl;
    }

    @Hidden
    @GetMapping("/")
    public ResponseEntity<Void> redirectLandingUrl() {
        return ResponseEntity.status(HttpStatus.PERMANENT_REDIRECT)
                .header(HttpHeaders.LOCATION, landingUrl)
                .build();
    }
}
