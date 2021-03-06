package com.apicatalog.jsonld.suite;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import com.apicatalog.jsonld.api.JsonLdErrorCode;
import com.apicatalog.jsonld.api.JsonLdOptions;
import com.apicatalog.jsonld.http.media.MediaType;
import com.apicatalog.jsonld.json.JsonUtils;
import com.apicatalog.jsonld.lang.Keywords;
import com.apicatalog.jsonld.loader.DocumentLoader;
import com.apicatalog.jsonld.suite.loader.UriBaseRewriter;
import com.apicatalog.jsonld.suite.loader.ZipResourceLoader;

public final class JsonLdTestCase {

    public String id;
    
    public String name;
    
    public URI input;
    
    public URI context;
    
    public URI expect;
    
    public URI frame;
    
    public JsonLdErrorCode expectErrorCode;
    
    public String baseUri;
    
    public String uri;
    
    public Set<String> type;
    
    public JsonLdTestCaseOptions options;

    public MediaType contentType;
    
    public URI redirectTo;
    
    public Integer httpStatus;
    
    public Set<String> httpLink;
    
    private final String testsBase;
    
    public JsonLdTestCase(final String testsBase) {
        this.testsBase = testsBase;
    }

    public static final JsonLdTestCase of(JsonObject o, String manifestUri, String manifestBase, String baseUri) {
        
        final JsonLdTestCase testCase = new JsonLdTestCase(manifestBase);
        
        testCase.id = o.getString(Keywords.ID);
        
        testCase.uri = baseUri + manifestUri.substring(0, manifestUri.length() - ".jsonld".length()) + testCase.id;
        
        testCase.type = o.get(Keywords.TYPE).asJsonArray().stream()
                            .map(JsonString.class::cast)
                            .map(JsonString::getString)
                            .collect(Collectors.toSet());
        
        testCase.name = o.getString("name");
        
        testCase.input = o.containsKey("input")
                            ? URI.create(baseUri + o.getString("input"))
                            : null;
        
        testCase.context = o.containsKey("context")
                                ? URI.create(baseUri + o.getString("context"))
                                : null;
                                
        testCase.expect = o.containsKey("expect")
                                ? URI.create(baseUri + o.getString("expect"))
                                : null;

        testCase.frame = o.containsKey("frame")
                                ? URI.create(baseUri + o.getString("frame"))
                                : null;

        testCase.expectErrorCode = o.containsKey("expectErrorCode")
                                            ? errorCode((o.getString("expectErrorCode")))
                                            : null;
        
        testCase.options = o.containsKey("option")
                                ? JsonLdTestCaseOptions.of(o.getJsonObject("option"), baseUri)
                                : new JsonLdTestCaseOptions();
                                
        testCase.baseUri = baseUri;
        
        
        testCase.contentType = o.containsKey("option") && o.getJsonObject("option").containsKey("contentType") 
                                    ? MediaType.of(o.getJsonObject("option").getString("contentType"))
                                    : null;
        
        if (testCase.contentType == null && testCase.input != null) {
            
            if (testCase.input.toString().endsWith(".jsonld")) {
                testCase.contentType = MediaType.JSON_LD;
                
            } else if (testCase.input.toString().endsWith(".json")) {
                testCase.contentType = MediaType.JSON;
                
            } else if (testCase.input.toString().endsWith(".html")) {
                testCase.contentType = MediaType.HTML;
            }
        }
        
        testCase.redirectTo = o.containsKey("option") && o.getJsonObject("option").containsKey("redirectTo")
                                ? URI.create(baseUri + o.getJsonObject("option").getString("redirectTo"))
                                : null;
        
        testCase.httpStatus = o.containsKey("option")  
                                    ? o.getJsonObject("option").getInt("httpStatus", 301)
                                    : null
                                    ;

        if (o.containsKey("option") &&  o.getJsonObject("option").containsKey("httpLink")) {
            
            JsonValue links = o.getJsonObject("option").get("httpLink");
            
            if (JsonUtils.isArray(links)) {
                testCase.httpLink = links.asJsonArray().stream()
                                            .map(JsonString.class::cast)
                                            .map(JsonString::getString)
                                            .collect(Collectors.toSet());
            } else {
                testCase.httpLink = new HashSet<>();
                testCase.httpLink.add(((JsonString)links).getString());
            }
        }
        
        return testCase;
    }
        
    public JsonLdOptions getOptions() {
        
        final DocumentLoader loader = 
                new UriBaseRewriter(
                            baseUri, 
                            testsBase,
                            new ZipResourceLoader()
                        );
        
        JsonLdOptions jsonLdOptions = new JsonLdOptions(loader);
        jsonLdOptions.setOrdered(true);
        
        options.setup(jsonLdOptions);
        
        return jsonLdOptions;
    }
    
    public static final JsonLdErrorCode errorCode(String errorCode) {
        
        if (errorCode == null || errorCode.isBlank()) {
            return null;
        }
        
        /*
         * Because scoped contexts can lead to contexts being reloaded, 
         * replace the recursive context inclusion error with a context overflow error.
         * 
         * @see <a href="https://www.w3.org/TR/json-ld11-api/#changes-from-cg">Changes since JSON-LD Community Group Final Report</a>
         */
        if ("recursive context inclusion".equalsIgnoreCase(errorCode)) {
            return JsonLdErrorCode.CONTEXT_OVERFLOW;
        }
        if ("list of lists".equalsIgnoreCase(errorCode)) {
            return JsonLdErrorCode.UNSPECIFIED;
        }
        if ("compaction to list of lists".equalsIgnoreCase(errorCode)) {
            return JsonLdErrorCode.UNSPECIFIED;
        }

        return JsonLdErrorCode.valueOf(errorCode.strip().toUpperCase().replace(" ", "_").replace("-", "_").replaceAll("\\_\\@", "_KEYWORD_" )); 
    }
}
