package com.basketballbot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class Controller {

    @GetMapping("/ping")
    public void ping() {
        log.info("Bot has been renewed");
    }
}
