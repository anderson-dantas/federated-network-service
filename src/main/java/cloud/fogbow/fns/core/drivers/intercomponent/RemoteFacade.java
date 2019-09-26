package cloud.fogbow.fns.core.drivers.intercomponent;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.fns.constants.ConfigurationPropertyKeys;
import cloud.fogbow.fns.core.PropertiesHolder;
import cloud.fogbow.fns.core.ServiceDriverConnector;
import cloud.fogbow.fns.core.drivers.ServiceDriver;
import cloud.fogbow.fns.core.drivers.dfns.AgentConfiguration;
import cloud.fogbow.fns.core.drivers.dfns.DfnsServiceDriver;
import cloud.fogbow.fns.core.drivers.dfns.SSAgentConfiguration;
import cloud.fogbow.fns.core.model.FederatedNetworkOrder;

public class RemoteFacade {
    private static final String LOCAL_MEMBER_NAME = PropertiesHolder.getInstance().getProperty(
            ConfigurationPropertyKeys.LOCAL_MEMBER_NAME_KEY);

    private static RemoteFacade instance;

    private RemoteFacade() {
    }

    public static RemoteFacade getInstance() {
        synchronized (RemoteFacade.class) {
            if (instance == null) {
                instance = new RemoteFacade();
            }
            return instance;
        }
    }

    public void removeAgentToComputeTunnel(FederatedNetworkOrder order, String hostIp) throws FogbowException {
        ServiceDriver driver = new ServiceDriverConnector(order.getServiceName(), LOCAL_MEMBER_NAME).getDriver();
        driver.cleanupAgent(order, hostIp);
    }

    public AgentConfiguration configureAgent(String publicKey, String serviceName) throws FogbowException {
        ServiceDriver driver = new ServiceDriverConnector(serviceName, LOCAL_MEMBER_NAME).getDriver();
        return driver.doConfigureAgent(publicKey);
    }
}
