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
package io.openleap.starter.core.client;

public class ClientProperties {
    /** Enable the client autoconfiguration/usage. */
    private boolean enabled = true;

    /** Base URL of the client service, e.g. http://localhost:8080 */
    private String baseUrl;



    /** Type of the client, e.g. "noop" or "rest" or "feign". */
    private String type = "noop";

    /** HTTP connect timeout in milliseconds. */
    private int connectTimeoutMillis = 5000;

    /** HTTP read timeout in milliseconds. */
    private int readTimeoutMillis = 15000;



    /** Authentication mode to use when calling the service */
    private AuthenticationMode authenticationMode = AuthenticationMode.NONE;

    /**
     * Properties for different authentication modes.
     * Only some of these are applicable depending on the authenticationMode.
     */
    // NONE, BASIC, APIKEY
    private String tenantId;
    private String userId;

    // BASIC
    private String username;
    private String password;

    // CLIENT (client credentials)
    private String clientId;
    private String clientSecret;

    // APIKEY
    private String apiKey;

    /**
     * No-arg constructor for manual instantiation and programmatic configuration.
     */
    public ClientProperties() {
    }


    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public int getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    public void setReadTimeoutMillis(int readTimeoutMillis) {
        this.readTimeoutMillis = readTimeoutMillis;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public AuthenticationMode getAuthenticationMode() {
        return authenticationMode;
    }

    public void setAuthenticationMode(AuthenticationMode authenticationMode) {
        this.authenticationMode = authenticationMode;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public enum AuthenticationMode {
        NONE,
        BASIC,
        CLIENT,
        APIKEY
    }
}
