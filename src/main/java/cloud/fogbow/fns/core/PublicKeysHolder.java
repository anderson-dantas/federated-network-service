package cloud.fogbow.fns.core;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnavailableProviderException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.util.RSAUtil;
import cloud.fogbow.common.util.connectivity.GenericRequestHttpResponse;
import cloud.fogbow.common.util.connectivity.HttpRequestClientUtil;
import cloud.fogbow.fns.constants.ConfigurationPropertyKeys;
import cloud.fogbow.fns.constants.ConfigurationPropertyDefaults;
import cloud.fogbow.fns.constants.Messages;
import cloud.fogbow.ras.api.http.PublicKey;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;

public class PublicKeysHolder {
    private HttpRequestClientUtil client;
    private RSAPublicKey asPublicKey;
    private RSAPublicKey rasPublicKey;

    private static PublicKeysHolder instance;

    private PublicKeysHolder() {
        String timeoutStr = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.HTTP_REQUEST_TIMEOUT_KEY,
                ConfigurationPropertyDefaults.HTTP_REQUEST_TIMEOUT);
        this.client = new HttpRequestClientUtil();
        this.asPublicKey = null;
        this.rasPublicKey = null;
    }

    public static synchronized PublicKeysHolder getInstance() {
        if (instance == null) {
            instance = new PublicKeysHolder();
        }
        return instance;
    }

    public RSAPublicKey getAsPublicKey() throws FogbowException {
        if (this.asPublicKey == null) {
            String asAddress = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.AS_URL_KEY);
            String asPort = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.AS_PORT_KEY);
            this.asPublicKey = getPublicKey(asAddress, asPort, cloud.fogbow.as.api.http.PublicKey.PUBLIC_KEY_ENDPOINT);
        }
        return this.asPublicKey;
    }

    public RSAPublicKey getRasPublicKey() throws FogbowException {
        if (this.rasPublicKey == null) {
            String rasAddress = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.RAS_URL_KEY);
            String rasPort = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.RAS_PORT_KEY);
            this.rasPublicKey = getPublicKey(rasAddress, rasPort, PublicKey.PUBLIC_KEY_ENDPOINT);
        }
        return this.rasPublicKey;
    }

    private RSAPublicKey getPublicKey(String serviceAddress, String servicePort, String suffix)
            throws FogbowException {
        RSAPublicKey publicKey = null;

        URI uri = null;
        try {
            uri = new URI(serviceAddress);
        } catch (URISyntaxException e) {
            throw new ConfigurationErrorException(String.format(Messages.Exception.INVALID_URL, serviceAddress));
        }
        uri = UriComponentsBuilder.fromUri(uri).port(servicePort).path(suffix).build(true).toUri();


        String endpoint = uri.toString();
        GenericRequestHttpResponse response = this.client.doGenericRequest("GET", endpoint, new HashMap<>(), new HashMap<>());
        if (response.getHttpCode() > HttpStatus.SC_OK) {
            Throwable e = new HttpResponseException(response.getHttpCode(), response.getContent());
            throw new UnavailableProviderException(e.getMessage(), e);
        } else {
            try {
                publicKey = RSAUtil.getPublicKeyFromString(response.getContent());
            } catch (GeneralSecurityException e) {
                throw new UnexpectedException(cloud.fogbow.ras.constants.Messages.Exception.INVALID_PUBLIC_KEY);
            }
            return publicKey;
        }

    }
}
