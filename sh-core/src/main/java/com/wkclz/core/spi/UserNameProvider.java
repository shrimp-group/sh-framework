package com.wkclz.core.spi;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public interface UserNameProvider {
    default Map<String, String> getNamesByUserCodes(Set<String> userCodes) {
        return Collections.emptyMap();
    }

}
