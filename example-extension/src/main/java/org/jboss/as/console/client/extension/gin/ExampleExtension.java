package org.jboss.as.console.client.extension.gin;

import com.google.gwt.inject.client.AsyncProvider;
import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;

import org.jboss.as.console.client.extension.HelloWorldPresenter;
import org.jboss.as.console.spi.GinExtension;

/**
 * @author Heiko Braun
 * @date 3/29/12
 */

@GinExtension("org.jboss.as.console.Extension")
@GinModules(ExampleExtensionBinding.class)
public interface ExampleExtension extends Ginjector {

    AsyncProvider<HelloWorldPresenter> getHelloWorldPresenter();
}
