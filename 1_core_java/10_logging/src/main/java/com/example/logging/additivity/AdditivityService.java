package com.example.logging.additivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdditivityService {
    /*
     * logback.xml has additivity="false" for this package.
     * That means this log goes only to ADDITIVITY_CONSOLE and does not also bubble to root appenders.
     */
    private static final Logger log = LoggerFactory.getLogger(AdditivityService.class);

    public void showAdditivity() {
        log.info("ADDITIVITY");
    }
}
