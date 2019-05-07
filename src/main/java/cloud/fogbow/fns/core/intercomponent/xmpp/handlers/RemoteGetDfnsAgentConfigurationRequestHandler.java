package cloud.fogbow.fns.core.intercomponent.xmpp.handlers;

import cloud.fogbow.fns.core.intercomponent.RemoteFacade;
import cloud.fogbow.fns.core.intercomponent.xmpp.RemoteMethod;
import cloud.fogbow.fns.core.intercomponent.xmpp.IqElement;
import cloud.fogbow.fns.core.serviceconnector.DfnsAgentConfiguration;
import cloud.fogbow.ras.core.intercomponent.xmpp.XmppExceptionToErrorConditionTranslator;
import com.google.gson.Gson;
import org.dom4j.Element;
import org.jamppa.component.handler.AbstractQueryHandler;
import org.xmpp.packet.IQ;

public class RemoteGetDfnsAgentConfigurationRequestHandler extends AbstractQueryHandler {
    private static final String REMOTE_GET_DFNS_AGENT_CONFIGURATION = RemoteMethod.REMOTE_GET_DFNS_AGENT_CONFIGURATION.toString();

    public RemoteGetDfnsAgentConfigurationRequestHandler() {
        super(REMOTE_GET_DFNS_AGENT_CONFIGURATION);
    }

    @Override
    public IQ handle(IQ iq) {
        String publicKey = unmarshalPublicKey(iq);
        IQ response = IQ.createResultIQ(iq);

        try {
            DfnsAgentConfiguration dfnsAgentConfiguration = RemoteFacade.getInstance().getDfnsAgentConfiguration(publicKey);
            updateResponse(response, dfnsAgentConfiguration);
        } catch (Exception e) {
            XmppExceptionToErrorConditionTranslator.updateErrorCondition(response, e);
        }

        return response;
    }

    private void updateResponse(IQ response, DfnsAgentConfiguration dfnsAgentConfiguration) {
        Element queryElement = response.getElement().addElement(IqElement.QUERY.toString(), REMOTE_GET_DFNS_AGENT_CONFIGURATION);
        Element dfnsAgentConfigurationElement = queryElement.addElement(IqElement.DFNS_AGENT_CONFIGURATION.toString());
        Element dfnsAgentConfigurationClassElement = queryElement.addElement(IqElement.DFNS_AGENT_CONFIGURATION_CLASS.toString());

        dfnsAgentConfigurationClassElement.setText(dfnsAgentConfiguration.getClass().getName());
        dfnsAgentConfigurationElement.setText(new Gson().toJson(dfnsAgentConfiguration));
    }

    private String unmarshalPublicKey(IQ iq) {
        Element federationUserElement =
                iq.getElement().element(IqElement.INSTANCE_PUBLIC_KEY.toString());

        String systemUser = new Gson().fromJson(federationUserElement.getText(), String.class);

        return systemUser;
    }
}
