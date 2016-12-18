package com.jsoniter.spi;

import java.lang.reflect.Type;

public class EmptyExtension implements Extension {

    @Override
    public Decoder createDecoder(String cacheKey, Type type) {
        return null;
    }

    @Override
    public void updateClassDescriptor(ClassDescriptor desc) {
    }
}
