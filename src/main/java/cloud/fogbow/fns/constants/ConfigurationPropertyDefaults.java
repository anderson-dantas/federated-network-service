package cloud.fogbow.fns.constants;

import java.util.concurrent.TimeUnit;

public class ConfigurationPropertyDefaults {
    // FNS CONF DEFAULTS
    public static final String BUILD_NUMBER = "[testing mode]";
    public static final String XMPP_TIMEOUT = Long.toString(TimeUnit.SECONDS.toMillis(5));
}
