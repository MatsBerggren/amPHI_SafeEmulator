package com.dedalus.amphi_integration.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.dedalus.amphi_integration.model.amphi.Symbol;
import com.dedalus.amphi_integration.repository.AmphiSymbolRepository;
import com.dedalus.amphi_integration.service.AmphiSymbolService;

@Service
public class AmphiSymbolServiceImpl implements AmphiSymbolService {

    @Autowired
    AmphiSymbolRepository amphiSymbolRepository;

    @Override
    public Symbol[] updateSymbols(Symbol[] symbols) {
        System.out.println(symbols);
        amphiSymbolRepository.deleteAll();
        for (Symbol symbol : symbols) {
            amphiSymbolRepository.save(symbol);
        } 
        return symbols;
    }

    @Override
    public List<Symbol> getAllSymbols() {
        return amphiSymbolRepository.findAll();
    }

}
