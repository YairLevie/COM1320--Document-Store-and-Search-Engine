package edu.yu.cs.com1320.project.stage4.impl;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import edu.yu.cs.com1320.project.HashTable;
import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.impl.TrieImpl;
import edu.yu.cs.com1320.project.stage4.Document;
import edu.yu.cs.com1320.project.stage4.DocumentStore;
import edu.yu.cs.com1320.project.undo.CommandSet;
import edu.yu.cs.com1320.project.undo.GenericCommand;
import edu.yu.cs.com1320.project.undo.Undoable;

public class DocumentStoreImpl implements DocumentStore {
    private HashTable <URI, Document> documentStore;
    private StackImpl<Undoable> stack;
    private TrieImpl<Document> trie;

    public DocumentStoreImpl(){
        documentStore = new HashTableImpl<>();
        stack = new StackImpl<>();
        trie = new TrieImpl<>();
    }

    public String setMetadata(URI uri, String key, String value){
        if (uri==null || key==null || uri.toString().isBlank() || key.isBlank() || this.get(uri)==null){
            throw new IllegalArgumentException();
        }
        String oldMetaValue =this.documentStore.get(uri).setMetadataValue(key, value);
        //if no previous metadata, undo deletes the metaData with that key completely
        if (oldMetaValue == null){
            Consumer<URI> deleteFunction = (v) -> this.documentStore.get(uri).setMetadataValue(key,null);
            GenericCommand<URI> undoNewSet = new GenericCommand<>(uri, deleteFunction);
            this.stack.push(undoNewSet);
        }
        // if had previous metadata, undo reverts the metadata back to its previous state 
        else{
            Consumer<URI> undoReplaceFunction = (v) -> this.documentStore.get(uri).setMetadataValue(key,oldMetaValue);
            GenericCommand<URI> undoReplace = new GenericCommand<>(uri, undoReplaceFunction);
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
                this.undoNewUpt(url, (DocumentImpl)this.documentStore.get(url));
                return 0;
            }
            //if the uri did exist, replace the previous document with the new document. undo function to revert the uri back to previous document 
            else { 
                int hashcode = oldDoc.hashCode(); 
                this.undoReplaceUpt(url, (DocumentImpl)this.documentStore.get(url), oldDoc);
                return hashcode;
            }
        }
    }

    private void undoNewUpt (URI url, DocumentImpl newDoc){
        if (newDoc.getDocumentTxt() != null){
            Consumer<URI> deleteFunction = (replace) -> {
                this.documentStore.put(replace, null);
                deleteFromTrie(newDoc);
            };
            GenericCommand<URI> undoNewPut = new GenericCommand<>(url, deleteFunction);
            this.stack.push(undoNewPut);
        }
        else{
            Consumer<URI> deleteFunction = (replace) -> this.documentStore.put(replace, null);
            GenericCommand<URI> undoNewPut = new GenericCommand<>(url, deleteFunction);
            this.stack.push(undoNewPut);
        }
    }
    private void undoReplaceUpt (URI url, DocumentImpl newDoc, DocumentImpl oldDoc){
        if (newDoc.getDocumentTxt() != null){
            Consumer<URI> replaceFunction = (replace) -> {
                this.documentStore.put(replace, oldDoc);
                deleteFromTrie(newDoc);
                addToTrie(oldDoc); //TODO: switch?
            };
            GenericCommand<URI> undoReplacePut = new GenericCommand<>(url, replaceFunction);
            this.stack.push(undoReplacePut);
           // deleteFromTrie(oldDoc);
        }
        else{
            Consumer<URI> undoReplaceFunction = (replace) -> this.documentStore.put(replace,oldDoc);
            GenericCommand<URI> undoReplacePut = new GenericCommand<>(url, undoReplaceFunction);
            this.stack.push(undoReplacePut);
        }
    }

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
            DocumentImpl oldDoc = (DocumentImpl)documentStore.put(url,null);
            Consumer<URI> putBackFunction = (replace) -> {
                this.documentStore.put(replace,oldDoc);
                this.addToTrie(oldDoc);
            };
            GenericCommand<URI> undoDelete = new GenericCommand<>(url, putBackFunction);
            this.stack.push(undoDelete);
            deleteFromTrie(oldDoc);
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
            addToTrie(strDoc);
            return (DocumentImpl) documentStore.put(uri, strDoc);
        }
    }
    
        //**********STAGE 4 ADDITIONS

    public List<Document> search(String keyword){
        Comparator<Document> comparator = new CustomComparator<Document>(keyword, false);
        List <Document> sortedDocuments = this.trie.getSorted(keyword, comparator);
        return sortedDocuments;
    }

    public List<Document> searchByPrefix(String keywordPrefix){
        Comparator<Document> comparator = new CustomComparator<Document>(keywordPrefix, true);
        List <Document> sortedDocuments = this.trie.getAllWithPrefixSorted(keywordPrefix, comparator);
        return sortedDocuments;
    }

    public Set<URI> deleteAll(String keyword){
        Set<Document> deletedDocuments = this.trie.deleteAll(keyword);
        Set<URI> deletedURIs = new HashSet<>();
        if (!deletedDocuments.isEmpty()){
            for(Document doc: deletedDocuments){
                deletedURIs.add(doc.getKey());
                deleteFromTrie((DocumentImpl) doc);
                documentStore.put(doc.getKey(), null);
            }
            this.carryOutUndo(deletedDocuments);
        }
        return deletedURIs;
    }

    public Set<URI> deleteAllWithPrefix(String keywordPrefix){
        Set<Document> deletedDocuments = this.trie.deleteAllWithPrefix(keywordPrefix);
        Set<URI> deletedURIs = new HashSet<>();
        if (!deletedDocuments.isEmpty()){
            for(Document doc: deletedDocuments){
                deletedURIs.add(doc.getKey());
                deleteFromTrie((DocumentImpl) doc);
                documentStore.put(doc.getKey(), null);
            }
            this.carryOutUndo(deletedDocuments);
        }
        return deletedURIs;
    }

    public List<Document> searchByMetadata(Map<String,String> keysValues){
        List<Document> listOfDocs = new ArrayList<>();
        listOfDocs.addAll(this.documentStore.values());
        this.equalsMetaData(listOfDocs, keysValues);
        return listOfDocs;
    }

    public List<Document> searchByKeywordAndMetadata(String keyword, Map<String,String> keysValues){
        Comparator<Document> comparator = new CustomComparator<Document>(keyword, false);
        List <Document> listOfDocs = this.trie.getSorted(keyword, comparator);
        this.equalsMetaData(listOfDocs, keysValues);
        return listOfDocs;
    }

    public List<Document> searchByPrefixAndMetadata(String keywordPrefix,Map<String,String> keysValues){
        Comparator<Document> comparator = new CustomComparator<Document>(keywordPrefix, true);
        List <Document> listOfDocs = this.trie.getAllWithPrefixSorted(keywordPrefix, comparator);
        this.equalsMetaData(listOfDocs, keysValues);
        return listOfDocs;
    }

    public Set<URI> deleteAllWithMetadata(Map<String,String> keysValues){
        Set<URI> deletedURIs = new HashSet<>();
        List<Document> listOfDocs = new ArrayList<>();
        listOfDocs.addAll(this.documentStore.values());
        this.equalsMetaData(listOfDocs, keysValues);
        for (int i = 0; i<listOfDocs.size();i++){
            deletedURIs.add(listOfDocs.get(i).getKey());
            deleteFromTrie((DocumentImpl) listOfDocs.get(i));
            documentStore.put(listOfDocs.get(i).getKey(), null);
        }
        Set<Document> deletedDocs = new HashSet<>();
        deletedDocs.addAll(listOfDocs);
        this.carryOutUndo(deletedDocs);
        return deletedURIs;
    }

    public Set<URI> deleteAllWithKeywordAndMetadata(String keyword,Map<String,String> keysValues){
        Set<URI> deletedURIs = new HashSet<>();
        Set <Document> setOfDocs = this.trie.get(keyword);
        List<Document> listOfDocs = new ArrayList<>();
        listOfDocs.addAll(setOfDocs);
        this.equalsMetaData(listOfDocs, keysValues);
        for (int i = 0; i<listOfDocs.size();i++){
            deletedURIs.add(listOfDocs.get(i).getKey());
            deleteFromTrie((DocumentImpl) listOfDocs.get(i));
            documentStore.put(listOfDocs.get(i).getKey(), null);
        }
        Set<Document> deletedDocs = new HashSet<>();
        deletedDocs.addAll(listOfDocs);
        this.carryOutUndo(deletedDocs);
        return deletedURIs;
    }

    public Set<URI> deleteAllWithPrefixAndMetadata(String keywordPrefix,Map<String,String> keysValues){
        Set<URI> deletedURIs = new HashSet<>();
        Comparator<Document> comparator = new CustomComparator<Document>(keywordPrefix, true);
        List <Document> listOfDocs = this.trie.getAllWithPrefixSorted(keywordPrefix, comparator);
        this.equalsMetaData(listOfDocs, keysValues);
        for (int i = 0; i<listOfDocs.size();i++){
            deletedURIs.add(listOfDocs.get(i).getKey());
            deleteFromTrie((DocumentImpl) listOfDocs.get(i));
            documentStore.put(listOfDocs.get(i).getKey(), null);
        }
        Set<Document> deletedDocs = new HashSet<>();
        deletedDocs.addAll(listOfDocs);
        this.carryOutUndo(deletedDocs);
        return deletedURIs;
    }

    private void equalsMetaData(List<Document> listOfDocs, Map<String, String> keysValues) {
        List<Document> matchingDocs = new ArrayList<>();
    
        for (Document doc : listOfDocs) {
            boolean flag = true;
            for (Map.Entry<String, String> entry : keysValues.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                String docValue = doc.getMetadataValue(key);
                if (docValue == null || !docValue.equals(value)) {
                    flag = false;
                    break;
                }
            }
            if (flag) {
                matchingDocs.add(doc);
            }
        }
        listOfDocs.clear();
        listOfDocs.addAll(matchingDocs);
    }

    private void undoSet(Set<Document> docs){
        CommandSet<URI> commandSet = new CommandSet<>();
        for(Document doc: docs){
            Consumer<URI> putBack = (replace) ->
            {
                DocumentImpl deletedDoc = (DocumentImpl) doc;
                this.documentStore.put(replace, deletedDoc);
                addToTrie(deletedDoc);
            };
            GenericCommand<URI> undoDelete = new GenericCommand<>(doc.getKey(), putBack);
            commandSet.addCommand(undoDelete);
        }
        this.stack.push(commandSet);
    }
    private void undoOneDoc(Set<Document> docs){
        List<Document> gettingDoc = new ArrayList<>();
        gettingDoc.addAll(docs);
        DocumentImpl doc = (DocumentImpl)gettingDoc.get(0);
        Consumer<URI> putBack = (replace) ->{
            this.documentStore.put(replace, doc);
            addToTrie(doc);
        };
        GenericCommand<URI> undoDelete = new GenericCommand<>(doc.getKey(), putBack);
        this.stack.push(undoDelete);
    }

    private void carryOutUndo(Set<Document> deletedDocs){
        if (deletedDocs.size()>1){
            this.undoSet(deletedDocs);
        }
        else{
            this.undoOneDoc(deletedDocs);
        }
    }

    public void undo() throws IllegalStateException{
        if (this.stack.peek()!=null){
            Undoable undoCommand = this.stack.pop();
            undoCommand.undo();
        }
        //no action to be undone... ie command stack is empty
       else{
            throw new IllegalStateException();
       } 
    }


    public void undo(URI url) throws IllegalStateException{
        StackImpl<Undoable> tempStack = new StackImpl<>();
        int type = this.getPeekToUndo(url, tempStack);
        if (type==0){
            while (tempStack.peek() != null) {
                this.stack.push(tempStack.pop());
            }
            throw new IllegalStateException();
        }
        else{
            if (type==1){
                CommandSet<URI> commandSetUndo = (CommandSet<URI>) this.stack.pop();
                commandSetUndo.undo(url);
                if (commandSetUndo.size() > 0) {
                    this.stack.push(commandSetUndo);
                }
            }
            else{
                GenericCommand<URI> undo = (GenericCommand<URI>) this.stack.pop();
                undo.undo();
            }
            while (tempStack.peek() != null) {
                this.stack.push(tempStack.pop());
            }
        }
    }
    private int getPeekToUndo(URI url, StackImpl<Undoable> tempStack){
        //ensures that this.stack.peek() is the desired stack that you want to undo. returns 1 if CommandSet, 2 if genericCommand, 0 if not found
        while(this.stack.peek()!=null){
            if (this.stack.peek() instanceof CommandSet){
                CommandSet<URI> commands = (CommandSet<URI>)this.stack.peek();
                if (commands.containsTarget(url)){
                    return 1;
                }
                else{
                    tempStack.push(this.stack.pop());
                }
            }
            else{
                GenericCommand<URI> command = (GenericCommand<URI>) this.stack.peek();
                if (command.getTarget().equals(url)){
                    return 2;
                }
                else{
                    tempStack.push(this.stack.pop());
                }
            }
        }
        return 0;
    }
    private void addToTrie(DocumentImpl document){
        Set<String> docWords = document.getWords();
        for (String word: docWords){
            trie.put(word,document);
        }
    }
    private void deleteFromTrie(DocumentImpl document){
        Set<String> docWords = document.getWords();
        for (String word: docWords){
            trie.delete(word,document);
        }
    }

    private class CustomComparator<DocumentImpl> implements Comparator<DocumentImpl>{
       
        private String keyWord;
        private boolean isPrefix;

        private CustomComparator(String keyWord, boolean isPrefix){
            this.keyWord = keyWord;
            this.isPrefix=isPrefix;
        }
        @Override
        public int compare(DocumentImpl doc1, DocumentImpl doc2) {
            if (isPrefix){
                return compareForPrefix(doc1,doc2);
            }
            else{
                if (((Document) doc1).wordCount(keyWord) < ((Document) doc2).wordCount(keyWord)) {
                    return 1;
                }
                if (((Document) doc1).wordCount(keyWord) > ((Document) doc2).wordCount(keyWord)) {
                    return -1;
                }
                return 0;
            }
        }

        private int compareForPrefix(DocumentImpl doc1, DocumentImpl doc2){
            Set<String> set1 = ((Document) doc1).getWords();
            int count1 = 0;
            int count2 = 0;
            for(String word: set1) {
                if(word.startsWith(keyWord)) {
                    count1+=((Document) doc1).wordCount(word); //is this correct?
                }
            }
            Set<String> set2 = ((Document) doc2).getWords();
            for(String word: set2) {
                if(word.startsWith(keyWord)) {
                    count2+=((Document) doc2).wordCount(word); //is this correct
                }
            }
            if (count1 < count2) {
                return 1;
            }
            if (count1 > count2) {
                return -1;
            }
            return 0;
        }
    }
}


//TODO: make sure my counting increment is correct in compare method
//TODO: test, monster method
//TODO: gitIGnore tooSimpleTrie
//TODO: for deleting a specific val, make sure i delete the val from the trie and not just what they passed in