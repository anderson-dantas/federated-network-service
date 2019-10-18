package cloud.fogbow.fns.core.drivers.intercomponent.xmpp;

import cloud.fogbow.fns.constants.ConfigurationPropertyDefaults;
import cloud.fogbow.fns.constants.ConfigurationPropertyKeys;
import cloud.fogbow.fns.constants.Messages;
import cloud.fogbow.fns.constants.SystemConstants;
import cloud.fogbow.fns.core.PropertiesHolder;
import org.apache.log4j.Logger;
import org.jamppa.component.PacketSender;
import org.xmpp.component.ComponentException;

public class PacketSenderHolder {
    private final static Logger LOGGER = Logger.getLogger(PacketSenderHolder.class);

    private static PacketSender packetSender = null;

    public static void init() {
        if (packetSender == null) {
            String jidServiceName = SystemConstants.JID_SERVICE_NAME;
            String jidConnector = SystemConstants.JID_CONNECTOR;
            String jidPrefix = SystemConstants.XMPP_SERVER_NAME_PREFIX;
            String providerId = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.PROVIDER_ID_KEY);
            String xmppPassword = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.XMPP_PASSWORD_KEY);
            String xmppServerIp = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.XMPP_SERVER_IP_KEY);
            int xmppServerPort = Integer.parseInt(PropertiesHolder.getInstance().
                    getProperty(ConfigurationPropertyKeys.XMPP_C2C_PORT_KEY, ConfigurationPropertyDefaults.XMPP_CSC_PORT));
            long xmppTimeout =
                    Long.parseLong(PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.XMPP_TIMEOUT_KEY,
                            ConfigurationPropertyDefaults.XMPP_TIMEOUT));
            XmppComponentManager xmppComponentManager = new XmppComponentManager(jidServiceName + jidConnector +
                    jidPrefix + providerId, xmppPassword, xmppServerIp, xmppServerPort, xmppTimeout);
            if (xmppServerIp != null && !xmppServerIp.isEmpty()) {
                try {
                    xmppComponentManager.connect();
                } catch (ComponentException e) {
                    throw new IllegalStateException();
                }
                PacketSenderHolder.packetSender = xmppComponentManager;
            } else {
                LOGGER.info(Messages.Info.NO_REMOTE_COMMUNICATION_CONFIGURED);
            }
        }
    }

    public static synchronized PacketSender getPacketSender() {
        init();
        return packetSender;
    }

    // Used in tests only
    public static void setPacketSender(PacketSender thePacketSender) {
        packetSender = thePacketSender;
    }
}

