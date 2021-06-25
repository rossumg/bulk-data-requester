package org.itech.common.util;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class ConfigurationListenerServiceImpl implements ConfigurationListenerService {

    @Override
    public List<ConfigurationListener> getConfigurationListeners() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void refreshConfigurations() {
        // TODO Auto-generated method stub
        
    }

//    @Autowired
//    private List<ConfigurationListener> configurationListener;
//
//    @Override
//    public List<ConfigurationListener> getConfigurationListeners() {
//        return configurationListener;
//    }
//
//    @Override
//    @Async
//    public void refreshConfigurations() {
//        List<ConfigurationListener> configurationListeners = getConfigurationListeners();
//        for (ConfigurationListener configurationListener : configurationListeners) {
//            configurationListener.refreshConfiguration();
//        }
//    }

}
