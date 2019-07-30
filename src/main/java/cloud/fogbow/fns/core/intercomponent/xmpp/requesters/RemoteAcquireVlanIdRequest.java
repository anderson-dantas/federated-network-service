package cloud.fogbow.fns.core.intercomponent.xmpp.requesters;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.fns.core.intercomponent.xmpp.RemoteMethod;
import cloud.fogbow.fns.core.intercomponent.xmpp.IqElement;
import cloud.fogbow.fns.core.intercomponent.xmpp.PacketSenderHolder;
import cloud.fogbow.ras.core.intercomponent.xmpp.XmppErrorConditionToExceptionTranslator;
import cloud.fogbow.ras.core.intercomponent.xmpp.requesters.RemoteRequest;
import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.xmpp.packet.IQ;


public class RemoteAcquireVlanIdRequest implements RemoteRequest<Integer> {
    private static final Logger LOGGER = Logger.getLogger(RemoteAcquireVlanIdRequest.class);

    private String provider;

    public RemoteAcquireVlanIdRequest(String provider) {
        this.provider = provider;
    }

    @Override
    public Integer send() throws Exception {
        IQ iq = RemoteAcquireVlanIdRequest.marshal(this.provider);
        IQ response = (IQ) PacketSenderHolder.getPacketSender().syncSendPacket(iq);

        XmppErrorConditionToExceptionTranslator.handleError(response, this.provider);

        return unmarshalVlanId(response);
    }

    public static IQ marshal(String provider) {
        IQ iq = new IQ(IQ.Type.get);
        iq.setTo(provider);

        iq.getElement().addElement(IqElement.QUERY.toString(), RemoteMethod.ACQUIRE_VLAN_ID.toString());

        return iq;
    }

    private Integer unmarshalVlanId(IQ response) throws UnexpectedException {
        Element queryElement = response.getElement().element(IqElement.QUERY.toString());
        String listStr = queryElement.element(IqElement.VLAN_ID.toString()).getText();

        String instanceClassName = queryElement.element(IqElement.VLAN_ID_CLASS_NAME.toString()).getText();

        Integer vlanId;

        try {
            vlanId = (Integer) new Gson().fromJson(listStr, Class.forName(instanceClassName));
        } catch (Exception e) {
            throw new UnexpectedException(e.getMessage());
        }

        return vlanId;
    }
}
