package com.example.demo.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class LoggingController {

    private static final Logger logger = LogManager.getLogger(LoggingController.class);

    @PostMapping("/post")
    public @ResponseBody
    ResponseEntity<String> post(@RequestBody String data) {
        //.. calling your layers (services, dao, ... etc implementations)
        logger.info(data); // logging your data before using it by exemple
        logger.error("data:{}",data);
        return new ResponseEntity<String>("Recieved: "+data, HttpStatus.OK);
    }
}