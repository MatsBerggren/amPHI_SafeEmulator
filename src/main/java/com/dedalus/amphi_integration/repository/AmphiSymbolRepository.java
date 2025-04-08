package com.dedalus.amphi_integration.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.dedalus.amphi_integration.model.amphi.Symbol;

@Repository
public interface AmphiSymbolRepository extends MongoRepository<Symbol, String> {
}