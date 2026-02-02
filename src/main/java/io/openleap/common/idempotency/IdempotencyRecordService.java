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
package io.openleap.common.idempotency;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * Command-level idempotency idempotency backed by the shared idempotency table.
 * Stores a unique key per processed command and allows duplicate detection.
 */
@Service
public class IdempotencyRecordService {

    private final IdempotencyRecordRepository repository;

    public IdempotencyRecordService(IdempotencyRecordRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public boolean alreadyProcessed(String commandId) {
        if (commandId == null || commandId.isBlank()) return false;
        return repository.findByIdemKey(commandKey(commandId)).isPresent();
    }

    @Transactional
    public void markProcessed(String commandId, String purpose, UUID entryUuid) {
        if (commandId == null || commandId.isBlank()) return;
        String key = commandKey(commandId);
        Optional<IdempotencyRecordEntity> existing = repository.findByIdemKey(key);
        if (existing.isPresent()) return;
        IdempotencyRecordEntity rec = new IdempotencyRecordEntity();
        rec.setIdemKey(key);
        rec.setPayloadHash(hash(purpose == null ? "" : purpose));
        rec.setEntryUuid(entryUuid == null ? UUID.randomUUID() : entryUuid);
        repository.save(rec);
    }

    private String commandKey(String commandId) {
        return "cmd:" + commandId;
    }

    private String hash(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return s; // fallback
        }
    }
}
