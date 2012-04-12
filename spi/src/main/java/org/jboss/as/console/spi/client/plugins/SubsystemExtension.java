package org.jboss.as.console.spi.client.plugins;

/**
 * @author Heiko Braun
 * @date 3/26/12
 */
public class SubsystemExtension {
    private String token;
    private String name;
    private String group;
    private String key;

    public SubsystemExtension(String name, String token, String group, String key) {
        this.name = name;
        this.token = token;
        this.group = group;
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public String getGroup() {
        return group;
    }

    public String getToken() {
        return token;
    }

    public String getKey() {
        return key;
    }
}
