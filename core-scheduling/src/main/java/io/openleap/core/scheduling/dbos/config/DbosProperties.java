package io.openleap.core.scheduling.dbos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// TODO (itaseski): Re-think configuration hierarchy
@ConfigurationProperties(prefix = "dbos")
public class DbosProperties {

    private String jdbcUrl;
    private String username;
    private String password;
    private boolean adminServerEnabled = false;
    private int adminServerPort = 3001;

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isAdminServerEnabled() {
        return adminServerEnabled;
    }

    public void setAdminServerEnabled(boolean adminServerEnabled) {
        this.adminServerEnabled = adminServerEnabled;
    }

    public int getAdminServerPort() {
        return adminServerPort;
    }

    public void setAdminServerPort(int adminServerPort) {
        this.adminServerPort = adminServerPort;
    }
}
