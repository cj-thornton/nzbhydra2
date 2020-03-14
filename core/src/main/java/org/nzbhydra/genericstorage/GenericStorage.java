package org.nzbhydra.genericstorage;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.nzbhydra.Jackson;
import org.nzbhydra.config.ConfigProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

@Component
public class GenericStorage {

    @Autowired
    private ConfigProvider configProvider;

    public <T extends Serializable> void save(String key, T value) {
        Map<String, String> genericStorage = configProvider.getBaseConfig().getGenericStorage();
        try {
            genericStorage.put(key, Jackson.JSON_MAPPER.writeValueAsString(value));
            configProvider.getBaseConfig().save(true);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error writing data as JSON", e);
        }
    }

    public <T extends Serializable> void remove(String key) {
        Map<String, String> genericStorage = configProvider.getBaseConfig().getGenericStorage();
        genericStorage.remove(key);
        configProvider.getBaseConfig().save(true);
    }

    public <T> Optional<T> get(String key, Class<T> clazz) {
        if (configProvider.getBaseConfig().getGenericStorage().containsKey(key)) {
            String json = configProvider.getBaseConfig().getGenericStorage().get(key);
            try {
                return Optional.of(Jackson.JSON_MAPPER.readValue(json, clazz));
            } catch (Exception e) {
                throw new RuntimeException("Error reading data from " + json, e);
            }
        }
        return Optional.empty();
    }

}
