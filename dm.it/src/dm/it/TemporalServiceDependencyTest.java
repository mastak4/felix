package dm.it;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.ServiceDependency;

import junit.framework.Assert;

public class TemporalServiceDependencyTest extends TestBase {
    public void testServiceConsumptionAndIntermittentAvailability() {
        DependencyManager m = new DependencyManager(context);
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service provider and consumer
        TemporalServiceProvider provider = new TemporalServiceProvider(e);
        Component sp = m.createComponent().setImplementation(provider).setInterface(TemporalServiceInterface.class.getName(), null);
        TemporalServiceProvider2 provider2 = new TemporalServiceProvider2(e);
        Component sp2 = m.createComponent().setImplementation(provider2).setInterface(TemporalServiceInterface.class.getName(), null);
        TemporalServiceConsumer consumer = new TemporalServiceConsumer(e);
        Component sc = m.createComponent().setImplementation(consumer)
            .add(m.createTemporalServiceDependency(10000).setService(TemporalServiceInterface.class).setRequired(true));
        // add the service consumer
        m.add(sc);
        // now add the first provider
        m.add(sp);
        e.waitForStep(2, 5000);
        // and remove it again (this should not affect the consumer yet)
        m.remove(sp);
        // now add the second provider
        m.add(sp2);
        e.step(3);
        e.waitForStep(4, 5000);
        // and remove it again
        m.remove(sp2);
        // finally remove the consumer
        m.remove(sc);
        // ensure we executed all steps inside the component instance
        e.step(6);
        m.clear();
    }

    public void testServiceConsumptionWithCallbackAndIntermittentAvailability() {
        DependencyManager m = new DependencyManager(context);
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service provider and consumer
        TemporalServiceProvider provider = new TemporalServiceProvider(e);
        Component sp = m.createComponent().setImplementation(provider).setInterface(TemporalServiceInterface.class.getName(), null);
        TemporalServiceProvider2 provider2 = new TemporalServiceProvider2(e);
        Component sp2 = m.createComponent().setImplementation(provider2).setInterface(TemporalServiceInterface.class.getName(), null);
        TemporalServiceConsumerWithCallback consumer = new TemporalServiceConsumerWithCallback(e);
        ServiceDependency temporalDep =  m.createTemporalServiceDependency(10000).setService(TemporalServiceInterface.class).setRequired(true).setCallbacks("add", "remove");
        Component sc = m.createComponent().setImplementation(consumer).add(temporalDep);
            
        // add the service consumer
        m.add(sc);
        // now add the first provider
        m.add(sp);
        e.waitForStep(2, 5000);
        // and remove it again (this should not affect the consumer yet)
        m.remove(sp);
        // now add the second provider
        m.add(sp2);
        e.step(3);
        e.waitForStep(4, 5000);
        // and remove it again
        m.remove(sp2);
        // finally remove the consumer
        m.remove(sc);
        // Wait for the consumer.remove callback
        e.waitForStep(6, 5000);
        // ensure we executed all steps inside the component instance
        e.step(7);
        m.clear();
    }

    static interface TemporalServiceInterface {
        public void invoke();
    }

    static class TemporalServiceProvider implements TemporalServiceInterface {
        private final Ensure m_ensure;
        public TemporalServiceProvider(Ensure e) {
            m_ensure = e;
        }
        public void invoke() {
            m_ensure.step(2);
        }
    }

    static class TemporalServiceProvider2 implements TemporalServiceInterface {
        protected final Ensure m_ensure;
        public TemporalServiceProvider2(Ensure e) {
            m_ensure = e;
        }
        public void invoke() {
            m_ensure.step(4);
        }
    }

    static class TemporalServiceConsumer implements Runnable {
        protected volatile TemporalServiceInterface m_service;
        protected final Ensure m_ensure;

        public TemporalServiceConsumer(Ensure e) {
            m_ensure = e;
        }
        
        public void init() {
            m_ensure.step(1);
            Thread t = new Thread(this);
            t.start();
        }
        
        public void run() {
            m_service.invoke();
            m_ensure.waitForStep(3, 15000);
            m_service.invoke();
        }
        
        public void destroy() {
            m_ensure.step(5);
        }
    }
    
    static class TemporalServiceConsumerWithCallback extends TemporalServiceConsumer {
        public TemporalServiceConsumerWithCallback(Ensure e) {
            super(e);
        }
        
        public void add(TemporalServiceInterface service) {
            m_service = service;
        }
        
        public void remove(TemporalServiceInterface service) {
            Assert.assertTrue(m_service == service);
            m_ensure.step(6);
        }
    }
}
