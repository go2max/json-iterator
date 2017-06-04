package com.jsoniter.spi;

import com.jsoniter.annotation.JsoniterConfig;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsoniterSpi {

    // registered at startup
    private static List<Extension> extensions = new ArrayList<Extension>();
    private static Map<Class, Class> typeImpls = new HashMap<Class, Class>();
    private static Map<Type, MapKeyCodec> globalMapKeyDecoders = new HashMap<Type, MapKeyCodec>();
    private static Config defaultConfig;
    // TODO: add encoder/decoders

    // runtime state
    private static int configIndex = 0;
    private static ThreadLocal<Config> currentConfig = new ThreadLocal<Config>() {
        @Override
        protected Config initialValue() {
            return defaultConfig;
        }
    };
    private static volatile Map<Object, String> configNames = new HashMap<Object, String>();
    // TODO: split to encoder/decoder
    private static volatile Map<String, MapKeyCodec> mapKeyCodecs = new HashMap<String, MapKeyCodec>();
    private static volatile Map<String, Encoder> encoders = new HashMap<String, Encoder>();
    private static volatile Map<String, Decoder> decoders = new HashMap<String, Decoder>();
    private static volatile Map<Class, Extension> objectFactories = new HashMap<Class, Extension>();

    static {
        defaultConfig = JsoniterConfig.INSTANCE;
    }

    public static String assignConfigName(Object obj) {
        String configName = configNames.get(obj);
        if (configName != null) {
            return configName;
        }
        return assignNewConfigName(obj);
    }

    private synchronized static String assignNewConfigName(Object obj) {
        String configName = configNames.get(obj);
        if (configName != null) {
            return configName;
        }
        configIndex++;
        configName = "jsoniter_codegen.cfg" + configIndex + ".";
        copyGlobalSettings(configName);
        HashMap<Object, String> newCache = new HashMap<Object, String>(configNames);
        newCache.put(obj, configName);
        configNames = newCache;
        return configName;
    }

    private static void copyGlobalSettings(String configName) {
        for (Map.Entry<Type, MapKeyCodec> entry : globalMapKeyDecoders.entrySet()) {
            copyGlobalMapKeyCodec(configName, entry.getKey(), entry.getValue());
        }
    }

    private static void copyGlobalMapKeyCodec(String configName, Type type, MapKeyCodec codec) {
        String cacheKey = TypeLiteral.create(type).getDecoderCacheKey(configName);
        addNewMapCodec(cacheKey, codec);
    }

    public static void registerExtension(Extension extension) {
        if (!extensions.contains(extension)) {
            extensions.add(extension);
        }
    }

    // TODO: use composite pattern
    public static List<Extension> getExtensions() {
        ArrayList<Extension> combined = new ArrayList<Extension>(extensions);
        combined.add(currentConfig.get());
        return combined;
    }

    public static void registerMapKeyDecoder(Type mapKeyType, MapKeyCodec mapKeyCodec) {
        globalMapKeyDecoders.put(mapKeyType, mapKeyCodec);
        copyGlobalMapKeyCodec(getCurrentConfig().configName(), mapKeyType, mapKeyCodec);
    }

    public synchronized static void addNewMapCodec(String cacheKey, MapKeyCodec mapKeyCodec) {
        HashMap<String, MapKeyCodec> newCache = new HashMap<String, MapKeyCodec>(mapKeyCodecs);
        newCache.put(cacheKey, mapKeyCodec);
        mapKeyCodecs = newCache;
    }

    public static MapKeyCodec getMapKeyDecoder(String cacheKey) {
        return mapKeyCodecs.get(cacheKey);
    }

    public static void registerTypeImplementation(Class superClazz, Class implClazz) {
        typeImpls.put(superClazz, implClazz);
    }

    public static Class getTypeImplementation(Class superClazz) {
        return typeImpls.get(superClazz);
    }

    public static void registerTypeDecoder(Class clazz, Decoder decoder) {
        addNewDecoder(TypeLiteral.create(clazz).getDecoderCacheKey(), decoder);
    }

    public static void registerTypeDecoder(TypeLiteral typeLiteral, Decoder decoder) {
        addNewDecoder(typeLiteral.getDecoderCacheKey(), decoder);
    }

    public static void registerPropertyDecoder(Class clazz, String field, Decoder decoder) {
        addNewDecoder(field + "@" + TypeLiteral.create(clazz).getDecoderCacheKey(), decoder);
    }

    public static void registerPropertyDecoder(TypeLiteral typeLiteral, String field, Decoder decoder) {
        addNewDecoder(field + "@" + typeLiteral.getDecoderCacheKey(), decoder);
    }

    public static void registerTypeEncoder(Class clazz, Encoder encoder) {
        addNewEncoder(TypeLiteral.create(clazz).getEncoderCacheKey(), encoder);
    }

    public static void registerTypeEncoder(TypeLiteral typeLiteral, Encoder encoder) {
        addNewEncoder(typeLiteral.getDecoderCacheKey(), encoder);
    }

    public static void registerPropertyEncoder(Class clazz, String field, Encoder encoder) {
        addNewEncoder(field + "@" + TypeLiteral.create(clazz).getEncoderCacheKey(), encoder);
    }

    public static void registerPropertyEncoder(TypeLiteral typeLiteral, String field, Encoder encoder) {
        addNewEncoder(field + "@" + typeLiteral.getDecoderCacheKey(), encoder);
    }

    public static Decoder getDecoder(String cacheKey) {
        return decoders.get(cacheKey);
    }

    public synchronized static void addNewDecoder(String cacheKey, Decoder decoder) {
        HashMap<String, Decoder> newCache = new HashMap<String, Decoder>(decoders);
        newCache.put(cacheKey, decoder);
        decoders = newCache;
    }

    public static Encoder getEncoder(String cacheKey) {
        return encoders.get(cacheKey);
    }

    public synchronized static void addNewEncoder(String cacheKey, Encoder encoder) {
        HashMap<String, Encoder> newCache = new HashMap<String, Encoder>(encoders);
        newCache.put(cacheKey, encoder);
        encoders = newCache;
    }

    public static boolean canCreate(Class clazz) {
        if (objectFactories.containsKey(clazz)) {
            return true;
        }
        for (Extension extension : getExtensions()) {
            if (extension.canCreate(clazz)) {
                addObjectFactory(clazz, extension);
                return true;
            }
        }
        return false;
    }

    public static Object create(Class clazz) {
        return getObjectFactory(clazz).create(clazz);
    }

    public static Extension getObjectFactory(Class clazz) {
        return objectFactories.get(clazz);
    }

    private synchronized static void addObjectFactory(Class clazz, Extension extension) {
        HashMap<Class, Extension> copy = new HashMap<Class, Extension>(objectFactories);
        copy.put(clazz, extension);
        objectFactories = copy;
    }

    public static void setCurrentConfig(Config val) {
        currentConfig.set(val);
    }

    public static void clearCurrentConfig() {
        currentConfig.set(defaultConfig);
    }

    public static Config getCurrentConfig() {
        return currentConfig.get();
    }

    public static void setDefaultConfig(Config val) {
        defaultConfig = val;
    }
}
