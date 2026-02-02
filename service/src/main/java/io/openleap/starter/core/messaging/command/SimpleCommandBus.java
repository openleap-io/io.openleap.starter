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
package io.openleap.starter.core.messaging.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple in-memory command bus that routes commands to Spring-managed handlers.
 * This uses synchronous invocation and relies on transactional handlers.
 */
@Component
public class SimpleCommandBus implements CommandGateway {

    private static final Logger log = LoggerFactory.getLogger(SimpleCommandBus.class);

    private final Map<Class<?>, CommandHandler<?, ?>> handlers = new HashMap<>();

    public SimpleCommandBus(ApplicationContext applicationContext) {
        // Autodiscover CommandHandler beans
        var beans = applicationContext.getBeansOfType(CommandHandler.class);
        beans.values().forEach(h -> handlers.put(h.commandType(), h));
        if (log.isInfoEnabled()) {
            log.info("Registered {} command handlers", handlers.size());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> R send(OlCommand command) {
        CommandHandler<OlCommand, R> handler = (CommandHandler<OlCommand, R>) handlers.get(command.getClass());
        if (handler == null) {
            throw new IllegalStateException("No handler for command type: " + command.getClass().getName());
        }
        return handler.handle(command);
    }
}
