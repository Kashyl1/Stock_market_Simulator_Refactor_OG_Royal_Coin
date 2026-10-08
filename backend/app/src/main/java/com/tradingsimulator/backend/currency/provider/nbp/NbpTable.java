package com.tradingsimulator.backend.currency.provider.nbp;

import java.time.LocalDate;
import java.util.List;

record NbpTable(LocalDate effectiveDate, List<NbpRate> rates) {
}
