package com.example.backend.service;

import com.example.backend.entity.PriceTick;
import com.example.backend.repository.PriceTickRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PriceTickSaver {
    @Autowired
    private PriceTickRepository priceTickRepository;
    @Transactional
    public void save(PriceTick priceTick) {
        priceTickRepository.save(priceTick);
    }
}
