package com.jsoniter.demo;


import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.DslJson;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jsoniter.output.JsonStream;
import com.jsoniter.spi.TypeLiteral;
import org.junit.Test;
import org.openjdk.jmh.Main;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.Blackhole;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@State(Scope.Thread)
public class ObjectOutput {

    private ByteArrayOutputStream baos;
    private ObjectMapper objectMapper;
    private JsonStream stream;
    private byte[] buffer;
    private DslJson dslJson;
    private TestObject testObject;
    private TypeLiteral typeLiteral;

    @CompiledJson
    public static class TestObject {
        public String field1;
        public String field2;
    }

    public static void main(String[] args) throws Exception {
        Main.main(new String[]{
                "ObjectOutput",
                "-i", "5",
                "-wi", "5",
                "-f", "1",
        });
    }

    @Test
    public void test() throws IOException {
        benchSetup(null);
        jsoniter();
        System.out.println(baos.toString());
        jackson();
        System.out.println(baos.toString());
        dsljson();
        System.out.println(baos.toString());
        System.out.println(JsonStream.serialize(testObject));
    }

    @Setup(Level.Trial)
    public void benchSetup(BenchmarkParams params) {
        baos = new ByteArrayOutputStream(1024 * 64);
        objectMapper = new ObjectMapper();
        objectMapper.getFactory().configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, true);
        stream = new JsonStream(baos, 4096);
        buffer = new byte[4096];
        dslJson = new DslJson();
        testObject = new TestObject();
        testObject.field1 = "hello";
        testObject.field2 = "world";
        typeLiteral = TypeLiteral.create(TestObject.class);
    }

    @Benchmark
    public void jsoniter() throws IOException {
        baos.reset();
        stream.reset(baos);
        stream.writeVal(typeLiteral, testObject);
        stream.flush();
    }

    @Benchmark
    public void jsoniter_easy_mode(Blackhole bh) throws IOException {
        bh.consume(JsonStream.serialize(testObject));
    }

    @Benchmark
    public void jackson() throws IOException {
        baos.reset();
        objectMapper.writeValue(baos, testObject);
    }

    @Benchmark
    public void dsljson() throws IOException {
        baos.reset();
        dslJson.serialize(testObject, baos);
    }
}
