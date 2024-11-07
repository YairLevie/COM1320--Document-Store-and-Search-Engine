package edu.yu.cs.com1320.project.stage2.impl;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import edu.yu.cs.com1320.project.HashTable;
import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.stage2.Document;
import edu.yu.cs.com1320.project.stage2.DocumentStore;

public class DocumentStoreImpl implements DocumentStore {
    private HashTable <URI, Document> documentStore;

    public DocumentStoreImpl(){
        documentStore = new HashTableImpl<>();
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

    public int put(InputStream input, URI url, DocumentFormat format) throws IOException{
        if (url == null || url.toString().isBlank()||format==null){
            throw new IllegalArgumentException();
        }
        if (input == null){
            if (documentStore.get(url)==null){ // should this be a containsKey()? it says "if there is no doc to delete"--> meaning no key or no value?
                return 0;
            }
            else{
                int hashcode = documentStore.get(url).hashCode();
                this.delete(url);
                return hashcode;
            }
        }
        else if (!(documentStore.containsKey(url))){
            directPut(input, url, format);
            return 0;
        }
        else if (documentStore.get(url)!=null){ 
            int hashcode = documentStore.get(url).hashCode(); 
            directPut(input, url, format);
            return hashcode;
        }
        
        return 0;//fix this in next stage
    }

    public Document get(URI url){ // make sure this is right!!
        if (url == null){
            return null;
        }
        return documentStore.get(url); 
    }

    public boolean delete(URI url){
        if (url == null){
            return false;
        }
        if (!(documentStore.containsKey(url))){
            return false;
        }
        else{
            documentStore.put(url,null);
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



   

