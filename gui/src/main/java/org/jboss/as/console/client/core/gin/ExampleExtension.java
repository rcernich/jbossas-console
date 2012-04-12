package org.jboss.as.console.client.core.gin;

import com.google.gwt.inject.client.AsyncProvider;
import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;

import org.jboss.as.console.client.shared.subsys.tx.TransactionPresenter;
import org.jboss.as.console.spi.GinExtension;

/**
 * @author Heiko Braun
 * @date 3/27/12
 */
@GinExtension("org.jboss.as.console.App")
@GinModules(ExampleExtensionBinding.class)
public interface ExampleExtension extends Ginjector {

    AsyncProvider<TransactionPresenter> getTransactionPresenter();

}
