package cloud.fogbow.fns.core.intercomponent.xmpp;

import cloud.fogbow.fns.constants.ConfigurationPropertyKeys;
import cloud.fogbow.fns.constants.Messages;
import cloud.fogbow.fns.core.PropertiesHolder;
import org.apache.log4j.Logger;
import org.jamppa.component.PacketSender;
import org.xmpp.component.ComponentException;

public class PacketSenderHolder {
    private final static Logger LOGGER = Logger.getLogger(PacketSenderHolder.class);

    private static PacketSender packetSender = null;

    public static void init() {
        if (packetSender == null) {
            String xmppJid = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.XMPP_JID_KEY);
            String xmppPassword = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.XMPP_PASSWORD_KEY);
            String xmppServerIp = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.XMPP_SERVER_IP_KEY);
            int xmppServerPort = Integer.parseInt(PropertiesHolder.getInstance().
                    getProperty(ConfigurationPropertyKeys.XMPP_C2C_PORT_KEY));
            long xmppTimeout =
                    Long.parseLong(PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.XMPP_TIMEOUT_KEY));
            XmppComponentManager xmppComponentManager = new XmppComponentManager(xmppJid, xmppPassword,
                    xmppServerIp, xmppServerPort, xmppTimeout);
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

