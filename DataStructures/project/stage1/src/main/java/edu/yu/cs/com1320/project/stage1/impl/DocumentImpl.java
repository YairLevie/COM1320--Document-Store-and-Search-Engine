package edu.yu.cs.com1320.project.stage1.impl;


import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import edu.yu.cs.com1320.project.stage1.Document;

public class DocumentImpl implements Document {
    private URI uri;
    private String txt;
    private byte[] binaryData;
    private Map <String,String> MetaValue;

    public DocumentImpl(URI uri, String txt){ // what does a "blank" URI mean? does reg URI need to be in URI form?Should it not work otherwise? 
        if (txt == null || uri == null || txt.isBlank() || uri.toString().isBlank() ){
            throw new IllegalArgumentException();
        }
        else{
            this.uri = uri;
            this.txt = txt;
            this.MetaValue = new HashMap<>();
        }
    }
    public DocumentImpl(URI uri, byte[] binaryData){ 
        if (uri == null || binaryData == null || uri.toString().isBlank()  || binaryData.length==0){
            throw new IllegalArgumentException();
        }
        else{
            this.uri = uri;
            this.binaryData = binaryData;
            this.MetaValue = new HashMap<>();
        }
    }
    public String setMetadataValue(String key, String value){
        if (key == null || key.isBlank()){
            throw new IllegalArgumentException();
        }
        else{
           return MetaValue.put(key, value);
        }
    }
    public String getMetadataValue(String key){
        if (key == null || key.isBlank()){
            throw new IllegalArgumentException();
        }
        return MetaValue.get(key); 
    }

    public HashMap<String, String> getMetadata(){
        HashMap <String,String> copiedHashmap = new HashMap<>();
        copiedHashmap.putAll(this.MetaValue);
        return copiedHashmap;
    }     
    public String getDocumentTxt(){
        return this.txt;
    }
    public byte[] getDocumentBinaryData(){ 
        return this.binaryData;
    }
    public URI getKey(){
        return this.uri;
    }
    @Override
    public int hashCode() {
        int result = uri.hashCode();
        result = 31 * result + (this.txt != null ? this.txt.hashCode() : 0); 
        result = 31 * result + Arrays.hashCode(binaryData);
        return result;
        }
    @Override
    public boolean equals(Object other){
        if (this == other){
            return true;
        }
        if (other == null){
            return false;
        }
        else if (!(other instanceof DocumentImpl)){
            return false;
        }
        DocumentImpl comparing = (DocumentImpl) other; 
        if (comparing.hashCode()==this.hashCode()){
            return true;
        }
        else{
            return false;
        }
    }
}

