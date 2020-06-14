package com.apicatalog.jsonld.framing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.json.JsonArray;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;

import com.apicatalog.jsonld.api.JsonLdError;
import com.apicatalog.jsonld.json.JsonUtils;
import com.apicatalog.jsonld.lang.Keywords;
import com.apicatalog.jsonld.lang.NodeObject;

public final class FrameMatcher {

    // required
    private FramingState state;
    private Frame frame;
    private boolean requireAll;
    
    private FrameMatcher(FramingState state, Frame frame, boolean requireAll) {
        this.state = state;
        this.frame = frame;
        this.requireAll = requireAll; 
    }
    
    public static final FrameMatcher with(FramingState state, Frame frame, boolean requireAll) {
        return new FrameMatcher(state, frame, requireAll);
    }
    
    public List<String> match(final List<String> subjects) throws JsonLdError {
   
        // 1. if frame is empty then all subject match
        if (frame.isEmpty()) {
            return subjects;
        }
        
        final List<String> result = new ArrayList<>();
        
        for (final String subject : subjects) {

            if (match(state.getGraphMap().get(state.getGraphName(), subject))) {
                result.add(subject);
            }
        }
        
        return result;
    }
    
    public boolean match(final Map<String, JsonValue> node) throws JsonLdError {

        int count = 0;
        
        for (final String property : frame.keys()) {

            JsonValue nodeValue = node.get(property);
            // 2.1.
            if (Keywords.ID.equals(property)) {

                nodeValue = JsonUtils.toJsonArray(nodeValue);
                
                if (JsonUtils.toJsonArray(frame.get(property)).stream().anyMatch(nodeValue.asJsonArray()::contains)
//                        || frame.isWildCard(Keywords.TYPE) 
//                        || frame.isNone(Keywords.TYPE)
                        || frame.isWildCard(Keywords.TYPE) 
                        || frame.isNone(Keywords.TYPE)
                        ) {
              
                    if (requireAll) {
                        count++;
                        continue;
                    }
                    return true;                    
                }
                return false;
                
            // 2.2.
            } else if (Keywords.TYPE.equals(property)) {

                if ((JsonUtils.isNotNull(nodeValue) && !nodeValue.asJsonArray().isEmpty() && frame.isWildCard(property))
                        || ((JsonUtils.isNull(nodeValue) || nodeValue.asJsonArray().isEmpty()) && frame.isNone(property))
                        || frame.isDefault(property)
                        || (JsonUtils.isNotNull(nodeValue) && frame.getArray(property).stream().anyMatch(nodeValue.asJsonArray()::contains))
                        ){

                    if (requireAll) {

                        count++;
                        continue;
                    }
                    return true;
                }
                return false;

            // skip other keywords
            } else if (Keywords.matchForm(property)) {
                continue;
            }
         
            
            JsonValue propertyValue = frame.get(property);
            final Frame propertyFrame;

            if (JsonUtils.isNotNull(propertyValue) 
                    && JsonUtils.isArray(propertyValue) && JsonUtils.isNotEmptyArray(propertyValue)) {
                propertyFrame = Frame.of((JsonStructure)propertyValue);
            } else {
                propertyFrame = null;
            }

            final JsonArray nodeValues = nodeValue != null 
                                            ? JsonUtils.toJsonArray(nodeValue)
                                            : JsonValue.EMPTY_JSON_ARRAY;
            
                                        
            // 2.5.
            if (nodeValues.isEmpty() 
                    && propertyFrame != null 
                    && propertyFrame.contains(Keywords.DEFAULT) //TODO only default
                    ) {
                continue;
            }
            
            // 2.6.
            if (nodeValues.isEmpty() && frame.isNone(property)) {
                
                if (requireAll) {
                    count++;
                    continue;
                }
                return true;
                
            } else if (!nodeValues.isEmpty() && frame.isNone(property)) {
                return false;
            }

            // 2.7.
            if (!nodeValues.isEmpty() && propertyFrame != null && propertyFrame.isEmpty()) {
                
                if (requireAll) {
                    count++;
                    continue;
                }
                return true;                
            }
            
            // 2.8.
            if (propertyFrame != null 
                    && propertyFrame.isValuePattern()
                    && nodeValues.stream().anyMatch(propertyFrame::matchValue)
                    ) {
                
                if (requireAll) {
                    count++;
                    continue;
                }
                return true;
            }

            // 2.9. //TODO for any???
            if (propertyFrame != null
                    && propertyFrame.isNodePattern()
                    && !nodeValues.isEmpty()
                    
                    ) {
                
                if (propertyFrame.isNodeReference()) {

                    boolean match = false;
                    for (JsonValue values : nodeValues) {
                        
                        match = propertyFrame.matchNode(state, values, requireAll);
                        if (match) {
                            break;
                        }
                    }
                                            
                    if (match) {
                        if (requireAll) {
                            count++;
                            continue;
                        }
                        return true;
                    }
                    
                } else if (!propertyFrame.isEmpty()) {
                    if (requireAll) {
                        count++;
                        continue;
                    }
                    return true;  
                }
            }


            if (requireAll) {
                return false;
            }
        }
        System.out.println("     : " + count);
        return count > 0;
    }
    
}