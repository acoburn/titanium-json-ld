package com.apicatalog.jsonld.processor;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;

import com.apicatalog.jsonld.api.JsonLdError;
import com.apicatalog.jsonld.api.JsonLdErrorCode;
import com.apicatalog.jsonld.api.JsonLdOptions;
import com.apicatalog.jsonld.compaction.CompactionBuilder;
import com.apicatalog.jsonld.compaction.UriCompactionBuilder;
import com.apicatalog.jsonld.context.ActiveContext;
import com.apicatalog.jsonld.context.ActiveContextBuilder;
import com.apicatalog.jsonld.document.RemoteDocument;
import com.apicatalog.jsonld.expansion.UriExpansionBuilder;
import com.apicatalog.jsonld.flattening.NodeMap;
import com.apicatalog.jsonld.flattening.NodeMapBuilder;
import com.apicatalog.jsonld.framing.Frame;
import com.apicatalog.jsonld.framing.FramingBuilder;
import com.apicatalog.jsonld.framing.FramingState;
import com.apicatalog.jsonld.json.JsonUtils;
import com.apicatalog.jsonld.lang.Keywords;
import com.apicatalog.jsonld.lang.Version;
import com.apicatalog.jsonld.loader.LoadDocumentOptions;

/**
 * 
 * @see <a href="https://www.w3.org/TR/json-ld11-framing/#dom-jsonldprocessor-frame">JsonLdProcessor.frame()</a>
 *
 */
public final class FramingProcessor {

    private FramingProcessor() {
    }
    
    public static final JsonObject frame(final RemoteDocument input, final RemoteDocument frame, final JsonLdOptions options) throws JsonLdError {
        
        // 4.
        final JsonLdOptions expansionOptions = new JsonLdOptions(options);
        expansionOptions.setOrdered(false);
        input.setDocumentUrl(null); //TODO needs revision
        
        JsonArray expandedInput = ExpansionProcessor.expand(input, expansionOptions);

        // 7.
        expansionOptions.setFrameExpansion(true);
        JsonArray expandedFrame = ExpansionProcessor.expand(frame, expansionOptions);

        
        // 8.
        final JsonObject frameObject = frame.getDocument().asJsonStructure().asJsonObject(); 

        System.out.println("Frame: " + frameObject);
        System.out.println("ExpandedFrame: " + expandedFrame);
        
        JsonValue context = JsonValue.EMPTY_JSON_OBJECT;
        
        if (frameObject.containsKey(Keywords.CONTEXT)    
                ) {
            context = frameObject.get(Keywords.CONTEXT);   
        }
            
        // 9.
        URI contextBase = (frame.getContextUrl() != null)
                                ? frame.getDocumentUrl()
                                : options.getBase();
                                
        // 10.
        ActiveContext activeContext = ActiveContextBuilder
                                        .with(new ActiveContext(options), context, contextBase, options).build();
        // 11.
        //TODO
        
        // 12.
        activeContext.createInverseContext();

        // 13.
        
        boolean frameDefault = options.isFrameDefault();
        
        String frameGraphExpanded = UriExpansionBuilder.with(activeContext, Keywords.GRAPH).vocab(true).build();
        
        
        //TODO expands to GRAPH        
        if (!frameDefault && frameObject.containsKey(frameGraphExpanded)) {
            frameDefault = true;
        }
        
        // 14.
        final FramingState state = new FramingState();
        
        state.setEmbed(options.getEmbed());     // 14.1.
        state.setEmbedded(false);               // 14.2.
        state.setExplicitInclusion(options.isExplicit());   // 14.3.
        state.setRequireAll(options.isRequiredAll());       // 14.4.
        state.setOmitDefault(options.isOmitDefault());      // 14.5.
        
        state.setGraphMap(NodeMapBuilder.with(expandedInput, new NodeMap()).build());   // 14.7.
        
        if (frameDefault) {
            state.setGraphName(Keywords.DEFAULT); // 14.6.

        } else {
            state.setGraphName(Keywords.MERGED);
            state.getGraphMap().merge();
        }
        
        //TODO
        
        // 15.
        Map<String, JsonValue> resultMap = new LinkedHashMap<>();
        
        // 16.
        FramingBuilder.with(state, 
                            new ArrayList<>(state.getGraphMap().subjects(state.getGraphName())), 
                            Frame.of(expandedFrame), 
                            resultMap, 
                            null
                            ).build();
        
        System.out.println("Result: " + resultMap);
        
        
        // 17.
        //TODO      
        
        // 18.
        List<JsonValue> result = removePreserve(resultMap.values());
        //TODO
        
        // 19.
        JsonValue compactedResults = CompactionBuilder
                                        .with(activeContext, null, JsonUtils.toJsonArray(result))
                                        .compactArrays(options.isCompactArrays())
                                        .ordered(options.isOrdered())
                                        .build();

        System.out.println("Compacted: " + compactedResults);
        // 19.1.
        if (JsonUtils.isEmptyArray(compactedResults)) {
            compactedResults = JsonValue.EMPTY_JSON_OBJECT;
            
        // 19.2.
        } else if (JsonUtils.isArray(compactedResults)) {
            
            String key = UriCompactionBuilder.with(activeContext, Keywords.GRAPH).vocab(true).build();
            
            compactedResults = Json.createObjectBuilder()
                                    .add(key, compactedResults).build();
            
        }
        
        // 20.
        compactedResults = replaceNull(compactedResults);
        
        final boolean omitGraph;
        
        if (options.isOmitGraph() == null) {
            
            omitGraph = activeContext.inMode(Version.V1_1);
            
        } else {
            omitGraph = options.isOmitGraph();
        }
        
        
        // 21.
        if (!omitGraph /*&& !compactedResults.asJsonObject().isEmpty()*/) {
            if  (!compactedResults.asJsonObject().containsKey(Keywords.GRAPH)) {
                
                if (compactedResults.asJsonObject().isEmpty()) {
                
                    compactedResults = Json.createObjectBuilder().add(Keywords.GRAPH, 
                            JsonValue.EMPTY_JSON_ARRAY
                            ).build();

                } else {
                
                    compactedResults = Json.createObjectBuilder().add(Keywords.GRAPH, 
                                            Json.createArrayBuilder().add(compactedResults)
                                            ).build();
                }
            }
            //TODO
            
        }
        //TODO

        // 19.3.
//        if (!compactedResults.asJsonObject().isEmpty()) {
        if (JsonUtils.isNotEmptyArray(context) && JsonUtils.isNotEmptyObject(context)) {
            compactedResults = Json.createObjectBuilder(compactedResults.asJsonObject()).add(Keywords.CONTEXT, context).build();
        }
        
        
        return compactedResults.asJsonObject();
    }

    public static final JsonObject frame(final URI input, final URI frame, final JsonLdOptions options) throws JsonLdError {
        return frame(getDocument(input, options), getDocument(frame, options), options);
    }
    
    private static RemoteDocument getDocument(final URI document, final JsonLdOptions options) throws JsonLdError {
        if (options.getDocumentLoader() == null) {
            throw new JsonLdError(JsonLdErrorCode.LOADING_DOCUMENT_FAILED);
        }

        final RemoteDocument remoteDocument = 
                                options
                                    .getDocumentLoader()
                                    .loadDocument(document,
                                            new LoadDocumentOptions()
                                                    .setExtractAllScripts(options.isExtractAllScripts()));

        if (remoteDocument == null) {
            throw new JsonLdError(JsonLdErrorCode.LOADING_DOCUMENT_FAILED, "Cannot load document [" + document + "].");
        }
        
        return remoteDocument;
    }
    
    
    private static final List<JsonValue> removePreserve(Collection<JsonValue> array) {
        return array.stream().map(FramingProcessor::removePreserveValue).collect(Collectors.toList());
    }
    
    private static final JsonValue removePreserveValue(JsonValue value) {
        
        if (JsonUtils.isScalar(value)) {
            return value;
        }
        if (JsonUtils.isArray(value)) {
            return JsonUtils.toJsonArray(removePreserve(value.asJsonArray()));
        }
        
        JsonObjectBuilder object = Json.createObjectBuilder();
        
        for (Entry<String, JsonValue> entry : value.asJsonObject().entrySet()) {
            
            if (Keywords.PRESERVE.equals(entry.getKey())) {
                
                return entry.getValue().asJsonArray().get(0);
            }
            object.add(entry.getKey(), removePreserveValue(entry.getValue()));
        }
        
        return object.build();
    }
    
    private static final JsonValue replaceNull(JsonValue value) {
        
        if (JsonUtils.isString(value) && Keywords.NULL.equals(((JsonString)value).getString())) {
            return JsonValue.NULL;
            
        } else if (JsonUtils.isScalar(value)) {
            return value;
            
        } else if (JsonUtils.isArray(value)) {
            
            JsonArrayBuilder array = Json.createArrayBuilder();
            
            value.asJsonArray().stream().map(FramingProcessor::replaceNull).forEach(array::add);
            
            JsonArray result = array.build();
            
            return result.size() != 1 || JsonUtils.isNotNull(result.get(0)) ? result : JsonValue.EMPTY_JSON_ARRAY;
        }
        
        JsonObjectBuilder object = Json.createObjectBuilder();
        
        for (Entry<String, JsonValue> entry : value.asJsonObject().entrySet()) {
            
            object.add(entry.getKey(), replaceNull(entry.getValue()));
            
        }
        return object.build();
    }
    
}