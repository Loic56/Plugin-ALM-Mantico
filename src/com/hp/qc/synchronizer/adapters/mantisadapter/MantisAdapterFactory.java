package com.hp.qc.synchronizer.adapters.mantisadapter;

import java.util.ArrayList;
import java.util.List;

import com.hp.qc.synchronizer.adapters.core.AdapterContext;
import com.hp.qc.synchronizer.adapters.spi.Adapter;
import com.hp.qc.synchronizer.adapters.spi.AdapterFactory;

public class MantisAdapterFactory implements AdapterFactory {
    
    private static final String MANTIS_ADAPTER_NAME = "MANTICO";
    
    public Adapter createAdapter(String adapterType, AdapterContext context) {
        Adapter adapter = null;
        if (adapterType.toUpperCase().equals(MANTIS_ADAPTER_NAME)) {
            adapter = new MantisAdapter(context.getLogger());
        }
        return adapter;
    }
    
    public List<String> getAdapterTypes() {
        List<String> adaptersList = new ArrayList<String>();
        adaptersList.add(MANTIS_ADAPTER_NAME);
        return adaptersList;
    }
    
}
