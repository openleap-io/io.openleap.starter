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
 * 2. A commercial license available from:
 *
 *     B+B Unternehmensberatung GmbH & Co.KG
 *     Robert-Bunsen-Straße 10
 *     67098 Bad Dürkheim
 *     Germany
 *     Contact: license@bb-online.de
 *
 *  You may choose which license to apply.
 */
package io.openleap.starter.core.persistence.specification;

import jakarta.persistence.criteria.Path;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Helper to build Spring Data JPA Specifications for filtering and searching.
 *
 * @param <T> The entity type.
 */
public class OlSpecificationBuilder<T> {

    private final List<Specification<T>> specifications = new ArrayList<>();

    /**
     * Creates a new builder instance.
     */
    public static <T> OlSpecificationBuilder<T> create() {
        return new OlSpecificationBuilder<>();
    }

    /**
     * Adds an equality filter.
     */
    public OlSpecificationBuilder<T> equal(String field, Object value) {
        if (value != null) {
            specifications.add((root, query, cb) -> cb.equal(getFieldPath(root, field), value));
        }
        return this;
    }

    /**
     * Adds a non-equality filter.
     */
    public OlSpecificationBuilder<T> notEqual(String field, Object value) {
        if (value != null) {
            specifications.add((root, query, cb) -> cb.notEqual(getFieldPath(root, field), value));
        }
        return this;
    }

    /**
     * Adds a greater than filter.
     */
    public <Y extends Comparable<? super Y>> OlSpecificationBuilder<T> greaterThan(String field, Y value) {
        if (value != null) {
            specifications.add((root, query, cb) -> cb.greaterThan(getFieldPath(root, field).as((Class<Y>) value.getClass()), value));
        }
        return this;
    }

    /**
     * Adds a less than filter.
     */
    public <Y extends Comparable<? super Y>> OlSpecificationBuilder<T> lessThan(String field, Y value) {
        if (value != null) {
            specifications.add((root, query, cb) -> cb.lessThan(getFieldPath(root, field).as((Class<Y>) value.getClass()), value));
        }
        return this;
    }

    /**
     * Adds an 'in' filter.
     */
    public OlSpecificationBuilder<T> in(String field, Collection<?> values) {
        if (values != null && !values.isEmpty()) {
            specifications.add((root, query, cb) -> getFieldPath(root, field).in(values));
        }
        return this;
    }

    /**
     * Adds a 'like' (case-insensitive) filter.
     */
    public OlSpecificationBuilder<T> like(String field, String pattern) {
        if (pattern != null && !pattern.isBlank()) {
            specifications.add((root, query, cb) -> 
                cb.like(cb.lower(getFieldPath(root, field).as(String.class)), "%" + pattern.toLowerCase() + "%"));
        }
        return this;
    }

    /**
     * Adds a custom specification.
     */
    public OlSpecificationBuilder<T> with(Specification<T> spec) {
        if (spec != null) {
            specifications.add(spec);
        }
        return this;
    }

    /**
     * Builds the final specification by AND-ing all added filters.
     */
    public Specification<T> build() {
        if (specifications.isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }

        Specification<T> result = specifications.get(0);
        for (int i = 1; i < specifications.size(); i++) {
            result = result.and(specifications.get(i));
        }
        return result;
    }

    /**
     * Helper to navigate nested paths (e.g., "customer.name").
     */
    private Path<?> getFieldPath(jakarta.persistence.criteria.Root<T> root, String field) {
        if (!field.contains(".")) {
            return root.get(field);
        }
        String[] parts = field.split("\\.");
        Path<?> path = root.get(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            path = path.get(parts[i]);
        }
        return path;
    }
}
