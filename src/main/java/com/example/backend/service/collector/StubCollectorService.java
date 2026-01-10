package com.example.backend.service.collector;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Profile("!collector")
public class StubCollectorService implements CollectorProvider{
    public StubCollectorService(){

    }
}
