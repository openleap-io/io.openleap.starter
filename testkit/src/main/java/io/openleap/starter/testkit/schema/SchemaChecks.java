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
package io.openleap.starter.testkit.schema;

import io.openleap.starter.testkit.ArchitectureRules;
import org.apache.avro.Schema;
import org.apache.avro.SchemaParseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utilities to validate Avro schema files and naming conventions.
 */
public final class SchemaChecks {
    private static final Pattern FILE_NAME_PATTERN = Pattern.compile(ArchitectureRules.ROUTING_KEY_REGEX.replace("$", "") + "\\.avsc$");

    private SchemaChecks() {}

    /** Parse a single .avsc file and return the Avro Schema, throwing a helpful message on error. */
    public static Schema parse(Path avscFile) {
        Objects.requireNonNull(avscFile, "avscFile");
        try {
            String json = Files.readString(avscFile);
            return new Schema.Parser().parse(json);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read schema file: " + avscFile, e);
        } catch (SchemaParseException e) {
            throw new IllegalStateException("Invalid Avro schema: " + avscFile + " -> " + e.getMessage(), e);
        }
    }

    /**
     * Validate that schema filenames mirror routing keys: <suite>.<domain>.<aggregate>.<kind>.<action>.v<major>.avsc
     * Returns list of violations (empty if OK).
     */
    public static List<String> validateFilenames(Path schemasDir) {
        try (Stream<Path> s = Files.walk(schemasDir)) {
            return s.filter(p -> p.toString().endsWith(".avsc"))
                    .map(SchemaChecks::violationOrNull)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read schemas dir: " + schemasDir, e);
        }
    }

    private static String violationOrNull(Path p) {
        String name = p.getFileName().toString();
        if (!FILE_NAME_PATTERN.matcher(name).matches()) {
            return "Schema file name does not match routing key pattern: " + name;
        }
        return null;
    }
}
