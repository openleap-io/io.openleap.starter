/*
 * This file is part of the openleap.io software project.
 *
 *  Copyright (C) 2025 Dr.-Ing. Sören Kemmann
 *
 * This software is dual-licensed under:
 *
 * 1. The European Union Public License v.1.2 (EUPL)
 *    https://joinup.ec.europa.eu/collection/eupl
 *
 *     You may use, modify and redistribute this file under the terms of the EUPL.
 *
 *  2. A commercial license available from:
 *
 *     B+B Unternehmensberatung GmbH & Co.KG
 *     Robert-Bunsen-Straße 10
 *     67098 Bad Dürkheim
 *     Germany
 *     Contact: license@bb-online.de
 *
 *  You may choose which license to apply.
 */
package io.openleap.starter.core.util;

import org.javamoney.moneta.Money;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.MonetaryAmount;
import java.math.BigDecimal;

/**
 * Utility component to convert between BigDecimal persistence values and JSR 354 MonetaryAmount
 * using the configured base currency (acc.baseCurrency).
 */
@Component
public class MoneyUtil {

    // Reads base currency from ol.starter.idempotency.money.basecurrency, falls back to acc.baseCurrency, then EUR
    @Value("${ol.starter.idempotency.money.basecurrency:EUR}")
    private String baseCurrency;

    public CurrencyUnit currency() {
        String cur = baseCurrency;
        return Monetary.getCurrency(cur == null || cur.isBlank() ? "EUR" : cur);
    }

    public MonetaryAmount of(BigDecimal amount) {
        if (amount == null) return null;
        return Money.of(amount, currency());
    }

    public BigDecimal toBigDecimal(MonetaryAmount amount) {
        if (amount == null) return null;
        return amount.getNumber().numberValueExact(BigDecimal.class);
    }

    public MonetaryAmount zero() {
        return Money.of(BigDecimal.ZERO, currency());
    }

    public boolean isZero(MonetaryAmount amount) {
        return amount == null || amount.isZero();
    }
}
