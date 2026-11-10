package com.idet.pos.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("/welcome")
public class WelcomeController {

    private final MessageSource messageSource;

    public WelcomeController(@Autowired MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @GetMapping
    public String welcome(@RequestHeader(value = "Accept-Language") String acceptLanguage) {
        // The view name "welcome" will be resolved to a template (e.g., welcome.html) by Spring MVC.

        Locale locale = Locale.forLanguageTag(acceptLanguage);
        String greetingMessage = messageSource.getMessage("greeting", null, locale);
        System.out.println("Greeting message for locale " + locale + ": " + greetingMessage);
        return greetingMessage;
    }
}
