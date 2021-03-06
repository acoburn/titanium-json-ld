package com.apicatalog.jsonld.api;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.URI;

import javax.json.Json;
import javax.json.JsonArray;

import org.junit.Assert;
import org.junit.Test;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.http.media.MediaType;
import com.apicatalog.jsonld.lang.Version;

public class ExpansionApiTest {

    public static final MockLoader MOCK_LOADER = new MockLoader(Json.createArrayBuilder().build());
    
    @Test    
    public void test1() throws JsonLdError {
        JsonArray expanded = JsonLd.expand(JsonDocument.of(Json.createObjectBuilder().build())).get();
        Assert.assertNotNull(expanded);
        Assert.assertEquals(Json.createArrayBuilder().build(), expanded);
    }
    
    @Test    
    public void test2() throws JsonLdError {
        JsonArray expanded = JsonLd.expand(JsonDocument.of(MediaType.JSON, new ByteArrayInputStream(Json.createObjectBuilder().build().toString().getBytes()))).get();
        Assert.assertNotNull(expanded);
        Assert.assertEquals(Json.createArrayBuilder().build(), expanded);
    }
    
    @Test    
    public void test3() throws JsonLdError {
        JsonArray expanded = JsonLd.expand("https://example.com").loader(MOCK_LOADER).base("").get();
        Assert.assertNotNull(expanded);
        Assert.assertEquals(Json.createArrayBuilder().build(), expanded);
    }

    @Test    
    public void test4() throws JsonLdError {
        JsonArray expanded = JsonLd.expand(URI.create("https://example.com")).loader(MOCK_LOADER).mode(Version.V1_0).get();
        Assert.assertNotNull(expanded);
        Assert.assertEquals(Json.createArrayBuilder().build(), expanded);
    }

    @Test    
    public void test5() throws JsonLdError {
        JsonArray expanded = JsonLd.expand("\thttps://example.com  ").loader(MOCK_LOADER).ordered().get();
        Assert.assertNotNull(expanded);
        Assert.assertEquals(Json.createArrayBuilder().build(), expanded);
    }
    
    @Test    
    public void test6() throws JsonLdError {
        JsonArray expanded = JsonLd.expand("\thttps://example.com").context(JsonDocument.of(Json.createObjectBuilder().build())).loader(MOCK_LOADER).ordered().get();
        Assert.assertNotNull(expanded);
        Assert.assertEquals(Json.createArrayBuilder().build(), expanded);
    }

    @Test
    public void test7() throws JsonLdError {
        JsonArray expanded = JsonLd.expand("\thttps://example.com").context(JsonDocument.of(MediaType.JSON, new InputStreamReader(new ByteArrayInputStream(Json.createObjectBuilder().build().toString().getBytes())))).loader(MOCK_LOADER).ordered().get();
        Assert.assertNotNull(expanded);
        Assert.assertEquals(Json.createArrayBuilder().build(), expanded);
    }    

    @Test    
    public void test8() throws JsonLdError {
        JsonArray expanded = JsonLd.expand("\thttps://example.com").context(Json.createObjectBuilder().build()).loader(MOCK_LOADER).ordered().get();
        Assert.assertNotNull(expanded);
        Assert.assertEquals(Json.createArrayBuilder().build(), expanded);
    }
}
