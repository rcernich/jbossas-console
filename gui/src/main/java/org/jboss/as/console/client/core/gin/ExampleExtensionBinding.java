package org.jboss.as.console.client.core.gin;

import com.gwtplatform.mvp.client.gin.AbstractPresenterModule;
import org.jboss.as.console.client.standalone.runtime.VMMetricsPresenter;
import org.jboss.as.console.client.standalone.runtime.VMMetricsView;

/**
 * @author Heiko Braun
 * @date 3/23/12
 */
public class ExampleExtensionBinding extends AbstractPresenterModule {
    @Override
    protected void configure() {

        bindPresenter(VMMetricsPresenter.class,
                VMMetricsPresenter.MyView.class,
                VMMetricsView.class,
                VMMetricsPresenter.MyProxy.class);

    }
}
