package com.basketballbot;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.abilitybots.api.db.DBContext;
import org.telegram.abilitybots.api.db.MapDBContext;

@Configuration
public class Config {

    @Bean
    public DBContext dbContext() {
        return MapDBContext.onlineInstance("messages");
    }
}
