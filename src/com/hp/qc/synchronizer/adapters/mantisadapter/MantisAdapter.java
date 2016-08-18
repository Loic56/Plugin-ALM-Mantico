package com.hp.qc.synchronizer.adapters.mantisadapter;

import java.util.HashMap;
import java.util.Map;

import com.hp.qc.synchronizer.adapters.core.AdapterConnectionData;
import com.hp.qc.synchronizer.adapters.core.AdapterLogger;
import com.hp.qc.synchronizer.adapters.core.EntityType;
import com.hp.qc.synchronizer.adapters.spi.Adapter;
import com.hp.qc.synchronizer.adapters.spi.AdapterConnection;

public class MantisAdapter implements Adapter {
    
    private static final String MANTIS_ADAPTER_VERSION = "1.1";
    
    private AdapterLogger logger;
    
    public MantisAdapter(AdapterLogger logger) {
        this.logger = logger;
    }
    
    public AdapterConnection connect(AdapterConnectionData connData) {
        logger.info("connect() called");
        MantisAdapterConnection conn = new MantisAdapterConnection(logger);
        conn.connect(connData);
        logger.info("connect() completed");
        return conn;
    }
    
    public Map<String, String> getConnectionParams() {
        Map<String, String> params = new HashMap<String, String>();
        params.put(MantisAdapterConnection.URL_SERVER_MANTIS, "");
        params.put(MantisAdapterConnection.MANTIS_PROJECT, "");
        return params;
    }
    
    public Map<String, String> getEndpointParams(EntityType arg0) {
        return null;
    }
    
    public String getVersion() {
        return MANTIS_ADAPTER_VERSION;
    }
}
