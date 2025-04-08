package com.dedalus.amphi_integration.service;

import java.util.List;
import com.dedalus.amphi_integration.model.amphi.Symbol;

public interface AmphiSymbolService {
    Symbol[] updateSymbols(Symbol[] symbols);
    List<Symbol> getAllSymbols();
}
