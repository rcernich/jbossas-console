package org.jboss.as.console.spi.client.plugins;

import java.util.List;

/**
 * @author Heiko Braun
 * @date 3/27/12
 */
public interface SubsystemRegistry {

    public List<SubsystemExtension> getExtensions();
}
