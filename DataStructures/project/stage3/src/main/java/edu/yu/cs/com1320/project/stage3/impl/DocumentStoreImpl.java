package edu.yu.cs.com1320.project.stage3.impl;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.function.Consumer;

import edu.yu.cs.com1320.project.HashTable;
import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.stage3.Document;
import edu.yu.cs.com1320.project.stage3.DocumentStore;
import edu.yu.cs.com1320.project.undo.Command;

public class DocumentStoreImpl implements DocumentStore {
    private HashTable <URI, Document> documentStore;
    private StackImpl<Command> stack;

    public DocumentStoreImpl(){
        documentStore = new HashTableImpl<>();
        stack = new StackImpl<>();
    }

    public String setMetadata(URI uri, String key, String value){
        if (uri==null || key==null || uri.toString().isBlank() || key.isBlank() || this.get(uri)==null){
            throw new IllegalArgumentException();
        }
        String oldMetaValue =this.documentStore.get(uri).setMetadataValue(key, value);
        //if no previous metadata, undo deletes the metaData with that key completely
        if (oldMetaValue == null){
            Consumer<URI> deleteFunction = (v) -> this.documentStore.get(uri).setMetadataValue(key,null);
            Command undoNewSet = new Command(uri, deleteFunction);
            this.stack.push(undoNewSet);
        }
        // if had previous metadata, undo reverts the metadata back to its previous state 
        else{
            Consumer<URI> undoReplaceFunction = (v) -> this.documentStore.get(uri).setMetadataValue(key,oldMetaValue);
            Command undoReplace = new Command(uri, undoReplaceFunction);
            this.stack.push(undoReplace);
        }
        return  oldMetaValue;
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
            if (documentStore.get(url)==null){ 
                return 0;
            }
            else{
                int hashcode = documentStore.get(url).hashCode();
                this.delete(url);
                return hashcode;
            }
        }
        else{
            DocumentImpl oldDoc = directPut(input, url, format);
            //if the uri didnt exist, add it to the store with the text. undo function deletes it completely
            if (oldDoc == null){
                Consumer<URI> deleteFunction = (replace) -> this.documentStore.put(replace,null);
                Command undoNewPut = new Command(url, deleteFunction);
                this.stack.push(undoNewPut);
                return 0;
            }
            //if the uri did exist, replace the previous document with the new document. undo function to revert the uri back to previous document 
            else { 
                int hashcode = oldDoc.hashCode(); 
                Consumer<URI> undoReplaceFunction = (replace) -> this.documentStore.put(replace,oldDoc);
                Command undoReplacePut = new Command (url,undoReplaceFunction);
                this.stack.push(undoReplacePut);
                return hashcode;
            }}}

    public Document get(URI url){
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
            //deletes document from the store. undo function adds the document back
            Document oldDoc = documentStore.put(url,null);
            Consumer<URI> putBackFunction = (replace) -> this.documentStore.put(replace,oldDoc);
            Command undoDelete = new Command (url,putBackFunction);
            this.stack.push(undoDelete);
            return true;
        }
    }
    private DocumentImpl directPut (InputStream input, URI uri, DocumentFormat format) throws IOException{
        byte[] value;
        try{
            value = input.readAllBytes();
        }
        catch(IOException e){
            throw new IOException();
        }
        if (format == DocumentFormat.BINARY){
            DocumentImpl byteDoc = new DocumentImpl(uri, value); 
            return (DocumentImpl) documentStore.put(uri, byteDoc);
        }
        else{
            String strValue = new String(value);
            DocumentImpl strDoc = new DocumentImpl(uri, strValue);
            return (DocumentImpl) documentStore.put(uri, strDoc);
        }
    }

    public void undo() throws IllegalStateException{
        if (this.stack.peek()!=null){
            Command undoCommand = this.stack.pop();
            undoCommand.undo();
        }
        //no action to be undone... ie command stack is empty
       else{
            throw new IllegalStateException();
       } 
    }

    public void undo(URI url) throws IllegalStateException{
        StackImpl<Command> tempStack = new StackImpl<>();
        //push stacks at the top to a temp stack until i find the stack with the URI i am looking for
        while((this.stack.peek()!=null) && !this.stack.peek().getUri().equals(url)){
            tempStack.push(this.stack.pop());
        }
        //confirming: 1. not at the end of the stack 2. at the right stack --> if true, undo the stack
        if (this.stack.peek()!= null && this.stack.peek().getUri().equals(url)){
            Command undoCommand = this.stack.pop();
            undoCommand.undo();
            while (tempStack.peek() != null){
                this.stack.push(tempStack.pop());
            }
        }
        //we reached the end of the stack with no matching URI, so put the stacks from tempStack back and throw an exception
        else{
            while (tempStack.peek() != null){
                this.stack.push(tempStack.pop());
            }
            throw new IllegalStateException();
        }
    }
    //for testing...
    private StackImpl<Command> getStack() {
        return this.stack;
    }
}