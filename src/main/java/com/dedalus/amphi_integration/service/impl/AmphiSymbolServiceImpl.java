package com.dedalus.amphi_integration.service.impl;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.dedalus.amphi_integration.model.amphi.Symbol;
import com.dedalus.amphi_integration.repository.AmphiSymbolRepository;

@Slf4j
@Service
public class AmphiSymbolServiceImpl {

    @Autowired
    AmphiSymbolRepository amphiSymbolRepository;

    public Symbol[] updateSymbols(Symbol[] symbols) {
        log.info("Updating {} symbols", symbols.length);
        amphiSymbolRepository.deleteAll();
        for (Symbol symbol : symbols) {
            amphiSymbolRepository.save(symbol);
        }
        return symbols;
    }

    public List<Symbol> getAllSymbols() {
        return amphiSymbolRepository.findAll();
    }
}
