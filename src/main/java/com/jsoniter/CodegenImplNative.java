package com.jsoniter;

import com.jsoniter.spi.Decoder;
import com.jsoniter.spi.TypeLiteral;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

class CodegenImplNative {
    final static Map<String, String> NATIVE_READS = new HashMap<String, String>() {{
        put("float", "iter.readFloat()");
        put("double", "iter.readDouble()");
        put("boolean", "iter.readBoolean()");
        put("byte", "iter.readShort()");
        put("short", "iter.readShort()");
        put("int", "iter.readInt()");
        put("char", "iter.readInt()");
        put("long", "iter.readLong()");
        put(Float.class.getName(), "Float.valueOf(iter.readFloat())");
        put(Double.class.getName(), "Double.valueOf(iter.readDouble())");
        put(Boolean.class.getName(), "Boolean.valueOf(iter.readBoolean())");
        put(Byte.class.getName(), "Byte.valueOf((byte)iter.readShort())");
        put(Character.class.getName(), "Character.valueOf((char)iter.readShort())");
        put(Short.class.getName(), "Short.valueOf(iter.readShort())");
        put(Integer.class.getName(), "Integer.valueOf(iter.readInt())");
        put(Long.class.getName(), "Long.valueOf(iter.readLong())");
        put(BigDecimal.class.getName(), "iter.readBigDecimal()");
        put(BigInteger.class.getName(), "iter.readBigInteger()");
        put(String.class.getName(), "iter.readString()");
        put(Object.class.getName(), "iter.readAnyObject()");
        put(Any.class.getName(), "iter.readAny()");
    }};

    public static String genNative(String nativeReadKey) {
        if ("boolean".equals(nativeReadKey)) {
            nativeReadKey = Boolean.class.getName();
        } else if ("byte".equals(nativeReadKey)) {
            nativeReadKey = Byte.class.getName();
        } else if ("char".equals(nativeReadKey)) {
            nativeReadKey = Character.class.getName();
        } else if ("short".equals(nativeReadKey)) {
            nativeReadKey = Short.class.getName();
        } else if ("int".equals(nativeReadKey)) {
            nativeReadKey = Integer.class.getName();
        } else if ("long".equals(nativeReadKey)) {
            nativeReadKey = Long.class.getName();
        } else if ("float".equals(nativeReadKey)) {
            nativeReadKey = Float.class.getName();
        } else if ("double".equals(nativeReadKey)) {
            nativeReadKey = Double.class.getName();
        }
        String op = NATIVE_READS.get(nativeReadKey);
        if (op == null) {
            throw new JsonException("do not know how to read: " + nativeReadKey);
        }
        return "return " + op + ";";
    }

    public static String genReadOp(Type type) {
        if (type instanceof Class) {
            Class clazz = (Class) type;
            String nativeRead = NATIVE_READS.get(clazz.getCanonicalName());
            if (nativeRead != null) {
                return nativeRead;
            }
        }
        String cacheKey = TypeLiteral.generateDecoderCacheKey(type);
        Decoder decoder = Codegen.getDecoder(cacheKey, type);// set the decoder to cache
        if (cacheKey.equals(decoder.getClass().getCanonicalName())) {
            return String.format("%s.decode_(iter)", cacheKey);
        } else {
            // can not use static "decode_" method to access, go through codegen cache
            return String.format("com.jsoniter.CodegenAccess.read(\"%s\", iter);", cacheKey);
        }
    }

    public static String getTypeName(Type fieldType) {
        if (fieldType instanceof Class) {
            Class clazz = (Class) fieldType;
            return clazz.getCanonicalName();
        } else if (fieldType instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) fieldType;
            Class clazz = (Class) pType.getRawType();
            return clazz.getCanonicalName();
        } else {
            throw new JsonException("unsupported type: " + fieldType);
        }
    }

    private static void append(StringBuilder lines, String str) {
        lines.append(str);
        lines.append("\n");
    }

    public static boolean isNative(Type valueType) {
        if (valueType instanceof  Class) {
            Class clazz  = (Class) valueType;
            return NATIVE_READS.containsKey(clazz.getCanonicalName());
        }
        return false;
    }
}
