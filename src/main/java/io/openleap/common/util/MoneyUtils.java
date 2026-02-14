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
package io.openleap.common.util;

import org.javamoney.moneta.Money;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.MonetaryAmount;
import java.math.BigDecimal;

/**
 * Utility component to convert between BigDecimal persistence values and JSR 354 MonetaryAmount
 * using the configured base currency (acc.baseCurrency).
 */
public class MoneyUtils {

    private final String baseCurrency;

    public MoneyUtils(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public CurrencyUnit currency(String baseCurrency) {
        return Monetary.getCurrency(baseCurrency == null || baseCurrency.isBlank() ? "EUR" : baseCurrency);
    }

    public MonetaryAmount of(BigDecimal amount) {
        if (amount == null) return null;
        return Money.of(amount, currency(baseCurrency));
    }

    public BigDecimal toBigDecimal(MonetaryAmount amount) {
        if (amount == null) return null;
        return amount.getNumber().numberValueExact(BigDecimal.class);
    }

    public MonetaryAmount zero() {
        return Money.of(BigDecimal.ZERO, currency(baseCurrency));
    }

    public boolean isZero(MonetaryAmount amount) {
        return amount == null || amount.isZero();
    }
}
