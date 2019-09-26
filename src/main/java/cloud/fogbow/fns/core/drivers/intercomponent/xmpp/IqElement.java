package cloud.fogbow.fns.core.drivers.intercomponent.xmpp;

public enum IqElement {
    QUERY("query"),
    FEDERATED_NETWORK_ORDER("order"),
    COMPUTE_ORDER("computeOrder"),
    MEMBER_CONFIGURATION_STATE("memberConfigurationState"),
    VLAN_ID("vlanId"),
    VLAN_ID_CLASS_NAME("vlanIdClassName"),
    HOST_IP("hostIp"),
    INSTANCE_PUBLIC_KEY("instancePublicKey"),
    REMOTE_AGENT_CONFIGURATION("remoteAgentConfiguration"),
    DFNS_AGENT_CONFIGURATION_CLASS("dfnsAgentConfigurationClass"),
    SERVICE_NAME("serviceName");

    private final String element;

    IqElement(final String elementName) {
        this.element = elementName;
    }

    @Override
    public String toString() {
        return element;
    }
}