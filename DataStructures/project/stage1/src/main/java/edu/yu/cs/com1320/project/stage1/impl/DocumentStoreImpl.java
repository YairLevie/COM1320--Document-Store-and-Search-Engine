package edu.yu.cs.com1320.project.stage1.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import edu.yu.cs.com1320.project.stage1.Document;
import edu.yu.cs.com1320.project.stage1.DocumentStore;

public class DocumentStoreImpl implements DocumentStore{
    private Map <URI, Document> documentStore;
    
    public DocumentStoreImpl(){
        documentStore = new HashMap<>();
    }
    
    public String setMetadata(URI uri, String key, String value){
        if (uri==null || key==null || uri.toString().isBlank() || key.isBlank() || this.get(uri)==null){
            throw new IllegalArgumentException();
        }
        return this.documentStore.get(uri).setMetadataValue(key, value); 
    }

    public String getMetadata(URI uri, String key){
        if (uri==null || key==null || uri.toString().isBlank() || key.isBlank() || this.get(uri)==null){
            throw new IllegalArgumentException();
        }
        return this.documentStore.get(uri).getMetadataValue(key);
    }

    // There is probably a way to do this just by looking at the value returned by the put hashMap method and just do stuff based on what that returns instead of making a whole separate method
    @Override
    public int put(InputStream input, URI uri, DocumentFormat format) throws IOException{ 
        if (uri == null || uri.toString().isBlank()||format==null){
            throw new IllegalArgumentException();
        }
        if (input == null){
            if (documentStore.get(uri)==null){ // should this be a containsKey()? it says "if there is no doc to delete"--> meaning no key or no value?
                return 0;
            }
            else{
                int hashcode = documentStore.get(uri).hashCode();
                this.delete(uri);
                return hashcode;
            }
        }
        else if (!(documentStore.containsKey(uri))){
            directPut(input, uri, format);
            return 0;
        }
        else if (documentStore.get(uri)!=null){ 
            int hashcode = documentStore.get(uri).hashCode(); 
            directPut(input, uri, format);
            return hashcode;
        }
        
        return 0;//fix this in next stage
    }
    @Override
    public Document get(URI url){
       return documentStore.get(url);
    }
    public boolean delete(URI url){
        if (!(documentStore.containsKey(url))){
            return false;
        }
        else{
            documentStore.remove(url);
            return true;
        }
    }
    private void directPut (InputStream input, URI uri, DocumentFormat format) throws IOException{
        byte[] value;
        try{
            value = input.readAllBytes();
        }
        catch(Exception e){
            throw new IOException();
        }
        if (format == DocumentFormat.BINARY){
            DocumentImpl byteDoc = new DocumentImpl(uri, value); 
            documentStore.put(uri, byteDoc);
        }
        else if (format == DocumentFormat.TXT){
            String strValue = new String(value);
            DocumentImpl strDoc = new DocumentImpl(uri, strValue);
            documentStore.put(uri, strDoc);
        }
    }
}

