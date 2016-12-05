package com.github.jsoniter;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;

import java.lang.reflect.Field;
import java.util.*;

class Codegen {
    static Map<Class, Decoder> cache = new HashMap<>();
    static ClassPool pool = ClassPool.getDefault();

    static Decoder gen(Class clazz) {
        Decoder decoder = cache.get(clazz);
        if (decoder != null) {
            return decoder;
        }
        try {
            CtClass ctClass = pool.makeClass("codegen." + clazz.getName().replace("[", "array_"));
            ctClass.setInterfaces(new CtClass[]{pool.get(Decoder.class.getName())});
            String source = null;
            if (clazz.isArray()) {
                source = genArray(clazz);
            } else {
                source = genObject(clazz);
            }
            CtMethod method = CtNewMethod.make(source, ctClass);
            ctClass.addMethod(method);
            decoder = (Decoder) ctClass.toClass().newInstance();
            cache.put(clazz, decoder);
            return decoder;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String genObject(Class clazz) {
        Map<Integer, Object> map = new HashMap<Integer, Object>();
        for (Field field : clazz.getFields()) {
            byte[] fieldName = field.getName().getBytes();
            Map<Byte, Object> current = (Map<Byte, Object>) map.get(fieldName.length);
            if (current == null) {
                current = new HashMap<>();
                map.put(fieldName.length, current);
            }
            for (int i = 0; i < fieldName.length - 1; i++) {
                byte b = fieldName[i];
                Map<Byte, Object> next = (Map<Byte, Object>) current.get(b);
                if (next == null) {
                    next = new HashMap<>();
                    current.put(b, next);
                }
                current = next;
            }
            current.put(fieldName[fieldName.length - 1], field);
        }
        StringBuilder lines = new StringBuilder();
        append(lines, "public Object decode(Class clazz, com.github.jsoniter.Jsoniter iter) {");
        append(lines, "{{clazz}} obj = new {{clazz}}();");
        append(lines, "for (com.github.jsoniter.Slice field = iter.ReadObject(); field != null; field = iter.ReadObject()) {");
        append(lines, "switch (field.len) {");
        for (Map.Entry<Integer, Object> entry : map.entrySet()) {
            Integer len = entry.getKey();
            append(lines, "case " + len + ": ");
            Map<Byte, Object> current = (Map<Byte, Object>) entry.getValue();
            addFieldDispatch(lines, len, 0, current);
            append(lines, "break;");
        }
        append(lines, "}");
        append(lines, "throw iter.err('bind object', 'field not found');".replace('\'', '"'));
        append(lines, "}");
        append(lines, "return obj;");
        append(lines, "}");
        return lines.toString().replace("{{clazz}}", clazz.getName());
    }

    private static void addFieldDispatch(StringBuilder lines, int len, int i, Map<Byte, Object> current) {
        for (Map.Entry<Byte, Object> entry : current.entrySet()) {
            Byte b = entry.getKey();
            append(lines, String.format("if (field.data[%d]==%s) {", i, b));
            if (i == len - 1) {
                Field field = (Field) entry.getValue();
                append(lines, String.format("obj.%s = iter.ReadString().toString();", field.getName()));
                append(lines, "continue;");
            } else {
                addFieldDispatch(lines, len, i+1, (Map<Byte, Object>) entry.getValue());
            }
            append(lines, "}");
        }
    }

    private static String genArray(Class clazz) {
        StringBuilder lines = new StringBuilder();
        append(lines, "public Object decode(Class clazz, com.github.jsoniter.Jsoniter iter) {");
        append(lines, "if (!iter.ReadArray()) {");
        append(lines, "return new {{comp}}[0];");
        append(lines, "}");
        append(lines, "{{comp}} a1 = ({{comp}}) iter.ReadUnsignedInt();");
        append(lines, "if (!iter.ReadArray()) {");
        append(lines, "return new {{comp}}[]{ a1 };");
        append(lines, "}");
        append(lines, "{{comp}} a2 = ({{comp}}) iter.ReadUnsignedInt();");
        append(lines, "if (!iter.ReadArray()) {");
        append(lines, "return new {{comp}}[]{ a1, a2 };");
        append(lines, "}");
        append(lines, "{{comp}} a3 = ({{comp}}) iter.ReadUnsignedInt();");
        append(lines, "if (!iter.ReadArray()) {");
        append(lines, "return new {{comp}}[]{ a1, a2, a3 };");
        append(lines, "}");
        append(lines, "{{comp}} a4 = ({{comp}}) iter.ReadUnsignedInt();");
        append(lines, "{{comp}}[] arr = new {{comp}}[8];");
        append(lines, "arr[0] = a1;");
        append(lines, "arr[1] = a2;");
        append(lines, "arr[2] = a3;");
        append(lines, "arr[3] = a4;");
        append(lines, "int i = 4;");
        append(lines, "while (iter.ReadArray()) {");
        append(lines, "if (i == arr.length) {");
        append(lines, "{{comp}}[] newArr = new {{comp}}[arr.length * 2];");
        append(lines, "System.arraycopy(arr, 0, newArr, 0, arr.length);");
        append(lines, "arr = newArr;");
        append(lines, "}");
        append(lines, "arr[i++] = ({{comp}}) iter.ReadUnsignedInt();");
        append(lines, "}");
        append(lines, "{{comp}}[] result = new {{comp}}[i];");
        append(lines, "System.arraycopy(arr, 0, result, 0, i);");
        append(lines, "return result;");
        append(lines, "}");
        return lines.toString().replace("{{comp}}", clazz.getComponentType().getName());
    }

    private static void append(StringBuilder lines, String str) {
        lines.append(str);
        lines.append("\n");
    }
}
