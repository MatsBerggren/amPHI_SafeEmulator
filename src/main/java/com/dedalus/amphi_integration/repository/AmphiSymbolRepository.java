package com.dedalus.amphi_integration.repository;

import org.springframework.stereotype.Repository;

import com.dedalus.amphi_integration.model.amphi.Symbol;

import jakarta.annotation.PostConstruct;

@Repository
public class AmphiSymbolRepository extends JsonFileRepository<Symbol> {

    @PostConstruct
    public void init() {
        initialize(Symbol.class);
    }
}