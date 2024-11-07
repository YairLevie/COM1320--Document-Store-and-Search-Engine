package edu.yu.cs.com1320.project.stage6.impl;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Consumer;

import edu.yu.cs.com1320.project.impl.BTreeImpl;
import edu.yu.cs.com1320.project.impl.MinHeapImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.impl.TrieImpl;
import edu.yu.cs.com1320.project.stage6.Document;
import edu.yu.cs.com1320.project.stage6.DocumentStore;
import edu.yu.cs.com1320.project.undo.CommandSet;
import edu.yu.cs.com1320.project.undo.GenericCommand;
import edu.yu.cs.com1320.project.undo.Undoable;

public class DocumentStoreImpl implements DocumentStore {
    private BTreeImpl<URI, Document> bTree;
    private StackImpl<Undoable> stack;
    private TrieImpl<URI> trie;
    private Integer maxDocCount;
    private Integer maxDocBytes;
    private int byteCount; //total (sum of) bytes in store
    private int currentDocCount;
    private MinHeapImpl<Node> minHeap;
    private HashSet <URI> disk = new HashSet <> ();
    private TrieImpl <MetaNode> metaTrie;
    

    public DocumentStoreImpl(){
        this.bTree = new BTreeImpl<>();
        this.stack = new StackImpl<>();
        this.trie = new TrieImpl<>();
        this.minHeap = new MinHeapImpl<>();
        this.byteCount = 0;
        this.currentDocCount = 0;
        DocumentPersistenceManager docPM = new DocumentPersistenceManager(null);
        this.bTree.setPersistenceManager(docPM);
        this.metaTrie= new TrieImpl<>();
    }
    public DocumentStoreImpl(File baseDir) {
        this.bTree = new BTreeImpl<>();
        this.stack = new StackImpl<>();
        this.trie = new TrieImpl<>();
        this.minHeap = new MinHeapImpl<>();
        this.byteCount = 0;
        this.currentDocCount = 0;
        DocumentPersistenceManager docPM = new DocumentPersistenceManager(baseDir);
        this.bTree.setPersistenceManager(docPM);
        this.metaTrie= new TrieImpl<>();
    }

    private class MetaNode{ //TODO: overide  equals and hashcode
        private String value;
        private URI uri;
        private MetaNode(String value, URI uri){
            this.value = value;
            this.uri = uri;
        }
        private URI getUri(){
            return this.uri;
        }
        private String getValue(){
            return this.value;
        }
        public boolean equals (Object node){
            if (this == node){
                return true;
            }
            if (node == null || node.getClass()!=this.getClass()){
                return false;
            }
            MetaNode newNode = (MetaNode) node;
            return (newNode.uri.equals(this.uri) && newNode.value.equals(this.value));
        }
    }

    public String setMetadata(URI uri, String key, String value)throws IOException{ //TODO: does this update the time of doc??
        if (uri==null || key==null || uri.toString().isBlank() || key.isBlank() || this.getDocFromTree(uri)==null){
            throw new IllegalArgumentException();
        }
        Document doc = this.getDocFromTree(uri);
        if (doc != null){
            doc.setLastUseTime(System.nanoTime());
            this.minHeap.reHeapify(new Node(doc.getKey(), this.bTree));
            this.disk.remove(doc.getKey()); //TODO: is this all i need to add?
        }
        String oldMetaValue =this.bTree.get(uri).setMetadataValue(key, value);
        MetaNode node = new MetaNode(value,uri);
        this.metaTrie.put(key, node);
        //if no previous metadata, undo deletes the metaData with that key completely
        Set<URI>uris = stayInLimit();
        if (oldMetaValue == null){
            Consumer<URI> deleteFunction = (v) -> {
                this.bTree.get(uri).setMetadataValue(key,null);
                doc.setLastUseTime(System.nanoTime());  
                this.minHeap.reHeapify(new Node(doc.getKey(),this.bTree));
                for (URI url : uris){
                    this.bTree.get(url);
                    this.currentDocCount++;
                    this.byteCount+=this.getBytePerDoc(this.bTree.get(url));
                }
                stayInLimit();
            };
            GenericCommand<URI> undoNewSet = new GenericCommand<>(uri, deleteFunction);
            this.stack.push(undoNewSet);
        }
        // if had previous metadata, undo reverts the metadata back to its previous state 
        else{
            Consumer<URI> undoReplaceFunction = (v) ->{
                this.bTree.get(uri).setMetadataValue(key,oldMetaValue);
                doc.setLastUseTime(System.nanoTime());
                this.minHeap.reHeapify(new Node(doc.getKey(),this.bTree));
                for (URI url : uris){
                    this.bTree.get(url);
                    this.currentDocCount++;
                    this.byteCount+=this.getBytePerDoc(this.bTree.get(url));
                }
                stayInLimit();
            };
            GenericCommand<URI> undoReplace = new GenericCommand<>(uri, undoReplaceFunction);
            this.stack.push(undoReplace);
        }
        return  oldMetaValue;}

    public String getMetadata(URI uri, String key)throws IOException{
        if (uri==null || key==null || uri.toString().isBlank() || key.isBlank() || this.get(uri)==null){
            throw new IllegalArgumentException();
        }
        Document doc = this.bTree.get(uri); 
        if (doc != null){
            doc.setLastUseTime(System.nanoTime());
            this.minHeap.reHeapify(new Node (doc.getKey(),this.bTree));
            this.disk.remove(doc.getKey());
        }
        stayInLimit();
        return this.bTree.get(uri).getMetadataValue(key);
    }

    public int put(InputStream input, URI url, DocumentFormat format) throws IOException{
        if (url == null || url.toString().isBlank()||format==null){
            throw new IllegalArgumentException();
        }
        if (input == null){
            if (this.getDocFromTree(url)==null){ 
                return 0;
            }
            else{
                int hashcode = this.getDocFromTree(url).hashCode();
                this.delete(url);
                return hashcode;
            }
        }
        else{
            DocumentImpl oldDoc = directPut(input, url, format);
            Set<URI> uris =  stayInLimit(); 
            //if the uri didnt exist, add it to the store with the text. undo function deletes it completely
            if (oldDoc == null){
                this.undoNewUpt(url, (DocumentImpl)this.getDocFromTree(url),uris);
                return 0;
            }
            //if the uri did exist, replace the previous document with the new document. undo function to revert the uri back to previous document 
            else { 
                int hashcode = oldDoc.hashCode(); 
                this.undoReplaceUpt(url, (DocumentImpl)this.getDocFromTree(url), oldDoc,uris);
                return hashcode;
            }
        }
    }

    private void undoNewUpt (URI url, DocumentImpl newDoc, Set<URI>uris){
        if (newDoc.getDocumentTxt() != null){
            Consumer<URI> deleteFunction = (replace) -> {
                if (!this.disk.contains(newDoc.getKey())) {
                    this.currentDocCount--;
                    this.byteCount-=newDoc.getDocumentTxt().getBytes().length;
                }
                deleteFromTrie(newDoc);
                HashMap <String,String> oldMetaData= newDoc.getMetadata();
                for (Map.Entry<String, String> entry : oldMetaData.entrySet()){
                    String key = entry.getKey();
                    String value = entry.getValue();
                    MetaNode node = new MetaNode(value, newDoc.getKey());
                    this.metaTrie.delete(key, node);
                }
                this.disk.remove(newDoc.getKey());
                try {
                    deleteFromHeap(newDoc);
                }
                catch (NoSuchElementException e) 
                {}
                this.bTree.put(replace, null);
                for (URI uri : uris){
                    this.bTree.get(uri);
                    this.currentDocCount++;
                    this.byteCount+=this.getBytePerDoc(this.bTree.get(uri));
                }
                stayInLimit();
            };
            GenericCommand<URI> undoNewPut = new GenericCommand<>(url, deleteFunction);
            this.stack.push(undoNewPut);
        }
        else{
            Consumer<URI> deleteFunction = (replace) ->{
                try {
                    deleteFromHeap(newDoc);
                }
                catch (NoSuchElementException e) 
                {}
                HashMap <String,String> oldMetaData= newDoc.getMetadata();
                for (Map.Entry<String, String> entry : oldMetaData.entrySet()){
                    String key = entry.getKey();
                    String value = entry.getValue();
                    MetaNode node = new MetaNode(value, newDoc.getKey());
                    this.metaTrie.delete(key, node);
                }
                if (!this.disk.contains(newDoc.getKey())) {
                    this.currentDocCount--;
                    this.byteCount-= newDoc.getDocumentBinaryData().length;
                }
                 this.disk.remove(newDoc.getKey());
                 this.bTree.put(replace, null);
                 for (URI uri : uris){
                    this.bTree.get(uri);
                    this.currentDocCount++;
                    this.byteCount+=this.getBytePerDoc(this.bTree.get(uri));
                }
                stayInLimit();
            };
            GenericCommand<URI> undoNewPut = new GenericCommand<>(url, deleteFunction);
            this.stack.push(undoNewPut);
        }
    }

    private void undoReplaceUpt (URI url, DocumentImpl newDoc, DocumentImpl oldDoc, Set<URI>uris){
        if (newDoc.getDocumentTxt() != null){
            Consumer<URI> replaceFunction = (replace) -> {
                this.bTree.put(replace, oldDoc);
                if (this.disk.contains(newDoc.getKey())) {
                    this.currentDocCount++;
                    
                }else{
                    this.byteCount-=newDoc.getDocumentTxt().getBytes().length;
                }
                addToTrie(oldDoc);
                deleteFromTrie(newDoc);
                byteCount+=oldDoc.getDocumentTxt().getBytes().length;
                try {
                    deleteFromHeap(newDoc);
                }
                catch (NoSuchElementException e) 
                {}
                oldDoc.setLastUseTime(System.nanoTime());
                this.minHeap.insert(new Node (oldDoc.getKey(),this.bTree));
                for (URI uri : uris){
                    this.bTree.get(uri);
                    this.currentDocCount++;
                    this.byteCount+=this.getBytePerDoc(this.bTree.get(uri));
                }
                stayInLimit();
            };
            GenericCommand<URI> undoReplacePut = new GenericCommand<>(url, replaceFunction);
            this.stack.push(undoReplacePut);
        }
        else{
            Consumer<URI> undoReplaceFunction = (replace) -> {
                this.bTree.put(replace,oldDoc);
                try {
                    deleteFromHeap(newDoc);
                }
                catch (NoSuchElementException e) 
                {}                if (this.disk.contains(newDoc.getKey())) {
                    this.currentDocCount++;
                    
                }else{
                    this.byteCount-=this.getBytePerDoc(newDoc);
                }
                this.byteCount+=oldDoc.getDocumentBinaryData().length;
                oldDoc.setLastUseTime(System.nanoTime());
                this.minHeap.insert(new Node (oldDoc.getKey(),this.bTree));
                for (URI uri : uris){
                    this.bTree.get(uri);
                    this.currentDocCount++;
                    this.byteCount+=this.getBytePerDoc(this.bTree.get(uri));
                }
                stayInLimit();
            };
            GenericCommand<URI> undoReplacePut = new GenericCommand<>(url, undoReplaceFunction);
            this.stack.push(undoReplacePut);
        }
    }

    public Document get(URI url)throws IOException{
        if (url == null){
            return null;
        }
        DocumentImpl docToGet;
        if (this.disk.contains(url)){
            docToGet = (DocumentImpl) this.bTree.get(url);
            this.byteCount+=this.getBytePerDoc(docToGet);
            this.currentDocCount++;
            docToGet.setLastUseTime(System.nanoTime());
            this.minHeap.insert(new Node(docToGet.getKey(),this.bTree));
            this.disk.remove(url);
        }else{
            docToGet = (DocumentImpl) this.bTree.get(url);
            if (docToGet!=null){
                docToGet.setLastUseTime(System.nanoTime());
                try{
                this.minHeap.reHeapify(new Node(docToGet.getKey(),this.bTree));
                }catch(NoSuchElementException e){}
            }
        }
        stayInLimit();
        return (Document) docToGet;
    }

    public boolean delete(URI url){
        if (url == null){
            return false;
        }
        DocumentImpl deletedDoc = (DocumentImpl) this.getDocFromTree(url);
        if (deletedDoc != null) {
            try {
                deleteFromHeap(deletedDoc);
            }
            catch (NoSuchElementException e) 
            {}
        }
        DocumentImpl oldDoc = (DocumentImpl)bTree.put(url,null);
        if (oldDoc != null){
            if (!this.disk.contains(oldDoc.getKey())){
                this.currentDocCount--;
                this.byteCount-= this.getBytePerDoc(oldDoc);
            }
            //deletes document from the store. undo function adds the document back
            deleteFromTrie(oldDoc);
            HashMap <String,String> oldMetaData= oldDoc.getMetadata();
            for (Map.Entry<String, String> entry : oldMetaData.entrySet()){
                String key = entry.getKey();
                String value = entry.getValue();
                MetaNode node = new MetaNode(value, oldDoc.getKey());
                this.metaTrie.delete(key, node);
            }
            boolean onDisk = this.disk.remove(oldDoc.getKey());
            Consumer<URI> putBackFunction = (replace) -> {  //TODO: Make sure doing right thing if the doc bringing back is bigger than limit... im doing nothing, but should i move it to disk?
                if ((this.maxDocBytes != null && this.getBytePerDoc(oldDoc) <= this.maxDocBytes) || this.maxDocBytes == null){
                    if (onDisk){
                        this.bTree.put(replace, oldDoc);
                        this.addToTrie(oldDoc);
                        HashMap <String,String> newMetaData= oldDoc.getMetadata();
                        for (Map.Entry<String, String> entry : newMetaData.entrySet()){
                          String key = entry.getKey();
                          String value = entry.getValue();
                          MetaNode node = new MetaNode(value, oldDoc.getKey());
                          this.metaTrie.put(key, node);
                        }
                        try{
                            this.bTree.moveToDisk(replace);
                        }catch(IOException e){}
                    }else{
                    this.bTree.put(replace,oldDoc);
                    this.currentDocCount++;
                    this.addToTrie(oldDoc);
                    HashMap <String,String> newMetaData= oldDoc.getMetadata();
                    for (Map.Entry<String, String> entry : newMetaData.entrySet()){
                        String key = entry.getKey();
                        String value = entry.getValue();
                        MetaNode node = new MetaNode(value, oldDoc.getKey());
                        this.metaTrie.put(key, node);
                    }
                    byteCount+=this.getBytePerDoc(oldDoc);
                    oldDoc.setLastUseTime(System.nanoTime());
                    this.minHeap.insert(new Node(oldDoc.getKey(),this.bTree));
                    stayInLimit();
                }
                }
            };
            GenericCommand<URI> undoDelete = new GenericCommand<>(url, putBackFunction);
            this.stack.push(undoDelete);
            return true;
        }
        else{
            return false;
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
            int space = value.length;
            if (this.maxDocBytes != null && this.maxDocBytes<space){
                throw new IllegalArgumentException();
            }
            Document oldDoc =(DocumentImpl) bTree.put(uri, byteDoc);
            this.byteCount+=space;
            byteDoc.setLastUseTime(System.nanoTime());
            this.minHeap.insert(new Node(byteDoc.getKey(),this.bTree));
            this.currentDocCount++;
            if (oldDoc == null){
                return (DocumentImpl) oldDoc;
            }
            else{
                if (!this.disk.contains(oldDoc.getKey())){
                    this.currentDocCount--; //TODO: depends if on disk!
                    this.byteCount -= this.getBytePerDoc(oldDoc);
                }
                try {
                    deleteFromHeap(oldDoc);
                }
                catch (NoSuchElementException e) 
                {}
                return (DocumentImpl) oldDoc;
            }
        }
        else{
            String strValue = new String(value);
            DocumentImpl strDoc = new DocumentImpl(uri, strValue, null); // IS NULL CORRECT
            int space = strDoc.getDocumentTxt().getBytes().length;
            if (this.maxDocBytes != null && this.maxDocBytes<space){
                throw new IllegalArgumentException();
            }
            DocumentImpl oldDoc = (DocumentImpl) this.bTree.put(uri, strDoc);
            this.byteCount+=space;
            strDoc.setLastUseTime(System.nanoTime());
            this.minHeap.insert(new Node (strDoc.getKey(),this.bTree));
            addToTrie(strDoc);
            this.currentDocCount++;
            if (oldDoc == null){
                return oldDoc;
            }
            else{
                if (!this.disk.contains(oldDoc.getKey())){
                    this.currentDocCount--; //TODO: depends if on disk!
                    this.byteCount -= this.getBytePerDoc(oldDoc);
                    try {
                        deleteFromHeap(oldDoc);
                    }
                    catch (NoSuchElementException e) 
                    {}
                    deleteFromTrie(oldDoc);
                }
                return oldDoc;
            }
            
        }
    }

        //**********STAGE 4 ADDITIONS

    public List<Document> search(String keyword)throws IOException{
        Comparator<URI> comparator = new CustomComparator<URI>(keyword, false);
        List <URI> sortedURIs = this.trie.getSorted(keyword, comparator);
        List<Document> docs = new ArrayList<>();
        for (URI uri: sortedURIs) {
            docs.add(this.bTree.get(uri));
        }
        this.setUpdatedTimes(docs);
        return docs;
    }

    public List<Document> searchByPrefix(String keywordPrefix)throws IOException{
        Comparator<URI> comparator = new CustomComparator<URI>(keywordPrefix, true);
        List <URI> sortedURIs = this.trie.getAllWithPrefixSorted(keywordPrefix, comparator);
        List<Document> docs = new ArrayList<>();
        for (URI uri: sortedURIs) {
            docs.add(this.bTree.get(uri));
        }
        this.setUpdatedTimes(docs);
        return docs;
    }

    public Set<URI> deleteAll(String keyword){
        Set<URI> deletedURIs = this.trie.deleteAll(keyword);
        Set<Document> deletedDocs = new HashSet<>();
        for (URI uri: deletedURIs) {
            deletedDocs.add(this.bTree.get(uri));
        }
        if (!deletedDocs.isEmpty()){
            for(Document doc: deletedDocs){
                if (!this.disk.contains(doc.getKey())) {
                    this.currentDocCount--;
                    this.byteCount -= this.getBytePerDoc(doc);
                }
                deleteFromTrie((DocumentImpl) doc);
                HashMap <String,String> oldMetaData= doc.getMetadata();
                for (Map.Entry<String, String> entry : oldMetaData.entrySet()){
                    String key = entry.getKey();
                    String value = entry.getValue();
                    MetaNode node = new MetaNode(value, doc.getKey());
                    this.metaTrie.delete(key, node);
                }
                try {
                    deleteFromHeap(doc);
                }
                catch (NoSuchElementException e) 
                {}
                this.bTree.put(doc.getKey(), null);
                this.disk.remove(doc.getKey());
            }
            this.carryOutUndo(deletedDocs);
        }
        return deletedURIs;
    }

    public Set<URI> deleteAllWithPrefix(String keywordPrefix){
        Set<URI> URIs = this.trie.deleteAllWithPrefix(keywordPrefix);
        Set<Document> deletedDocs = new HashSet<>();
        for (URI uri: URIs) {
            deletedDocs.add(this.bTree.get(uri));
        }
        if (!deletedDocs.isEmpty()){
            for(Document doc: deletedDocs){
                if (!this.disk.contains(doc.getKey())) {
                    this.currentDocCount--;
                    this.byteCount -= this.getBytePerDoc(doc);
                }
                deleteFromTrie((DocumentImpl) doc);
                HashMap <String,String> oldMetaData= doc.getMetadata();
                for (Map.Entry<String, String> entry : oldMetaData.entrySet()){
                    String key = entry.getKey();
                    String value = entry.getValue();
                    MetaNode node = new MetaNode(value, doc.getKey());
                    this.metaTrie.delete(key, node);
                }
                try{
                    deleteFromHeap(doc);
                }catch(NoSuchElementException e){}
                this.bTree.put(doc.getKey(), null);
                this.disk.remove(doc.getKey());
            }
            this.carryOutUndo(deletedDocs);
        }
        return URIs;
    }

    
    public List<Document> searchByMetadata(Map<String,String> keysValues)throws IOException{
        Set<Document> setOfDocs = new HashSet<>();
        for (Map.Entry<String, String> entry : keysValues.entrySet()){
            String key = entry.getKey();
            String value = entry.getValue();
            if (!this.metaTrie.get(key).isEmpty()){
                Set<MetaNode> nodes = this.metaTrie.get(key);
                for (MetaNode node:nodes){
                    if (node.getValue().equals(value)){
                        setOfDocs.add(this.getDocFromTree(node.getUri()));
                    }
                }
            }
        }
        List<Document> listOfDocs = new ArrayList<>();
        listOfDocs.addAll(setOfDocs);
        this.setUpdatedTimes(listOfDocs);
        stayInLimit();
        return listOfDocs;
    }


    public List<Document> searchByKeywordAndMetadata(String keyword, Map<String,String> keysValues)throws IOException{
        Comparator<URI> comparator = new CustomComparator<URI>(keyword, false);
        List <URI> listOfURIs = this.trie.getSorted(keyword, comparator);
        List<Document> listOfDocs = new ArrayList<>();
        for (Map.Entry<String, String> entry : keysValues.entrySet()){
            String key = entry.getKey();
            String value = entry.getValue();
            if (!this.metaTrie.get(key).isEmpty()){
                Set<MetaNode> nodes = this.metaTrie.get(key);
                for (MetaNode node:nodes){
                    if (node.getValue().equals(value) && listOfURIs.contains(node.getUri())){
                        listOfDocs.add(this.getDocFromTree(node.getUri()));
                    }
                }
            }
        }
        this.setUpdatedTimes(listOfDocs);
        stayInLimit();
        return listOfDocs;
    }

    public List<Document> searchByPrefixAndMetadata(String keywordPrefix,Map<String,String> keysValues)throws IOException{
        Comparator<URI> comparator = new CustomComparator<URI>(keywordPrefix, true);
        List <URI> listOfURIs = this.trie.getAllWithPrefixSorted(keywordPrefix, comparator);
        List<Document> listOfDocs = new ArrayList<>();
        for (Map.Entry<String, String> entry : keysValues.entrySet()){
            String key = entry.getKey();
            String value = entry.getValue();
            if (!this.metaTrie.get(key).isEmpty()){
                Set<MetaNode> nodes = this.metaTrie.get(key);
                for (MetaNode node:nodes){
                    if (node.getValue().equals(value) && listOfURIs.contains(node.getUri())){
                        listOfDocs.add(this.getDocFromTree(node.getUri()));
                    }
                }
            }
        }
        this.setUpdatedTimes(listOfDocs);
        stayInLimit();
        return listOfDocs;
    }

    public Set<URI> deleteAllWithMetadata(Map<String,String> keysValues)throws IOException{
        Set<URI> deletedURIs = new HashSet<>();
        List<Document> listOfDocs = new ArrayList<>();
        for (Map.Entry<String, String> entry : keysValues.entrySet()){
            String key = entry.getKey();
            String value = entry.getValue();
            if (!this.metaTrie.get(key).isEmpty()){
                Set<MetaNode> nodes = this.metaTrie.get(key);
                for (MetaNode node:nodes){
                    if (node.getValue().equals(value)){
                        listOfDocs.add(this.getDocFromTree(node.getUri()));
                    }
                }
            }
        }
        for (int i = 0; i<listOfDocs.size();i++){
            Document doc = listOfDocs.get(i);
            deletedURIs.add(doc.getKey());
            deleteFromTrie((DocumentImpl) doc);
            HashMap <String,String> oldMetaData= doc.getMetadata();
            for (Map.Entry<String, String> entry : oldMetaData.entrySet()){
                String key = entry.getKey();
                String value = entry.getValue();
                MetaNode node = new MetaNode(value, doc.getKey());
                this.metaTrie.delete(key, node);
            }
            try {
                deleteFromHeap(doc);
            }
            catch (NoSuchElementException e) 
            {}
            if (!this.disk.contains(doc.getKey())){
                this.byteCount -= this.getBytePerDoc(doc);
                this.currentDocCount--;
            }
            this.disk.remove(doc.getKey());
            this.bTree.put(doc.getKey(), null);
        }
        Set<Document> deletedDocs = new HashSet<>();
        deletedDocs.addAll(listOfDocs);
        this.carryOutUndo(deletedDocs);
        return deletedURIs;
    }

    public Set<URI> deleteAllWithKeywordAndMetadata(String keyword,Map<String,String> keysValues)throws IOException{
        Set<URI> deletedURIs = new HashSet<>();
        Set <URI> setOfURIs = this.trie.get(keyword);
        List<Document> listOfDocs = new ArrayList<>();
        for (Map.Entry<String, String> entry : keysValues.entrySet()){
            String key = entry.getKey();
            String value = entry.getValue();
            if (!this.metaTrie.get(key).isEmpty()){
                Set<MetaNode> nodes = this.metaTrie.get(key);
                for (MetaNode node:nodes){
                    if (node.getValue().equals(value) && setOfURIs.contains(node.getUri())){
                        listOfDocs.add(this.getDocFromTree(node.getUri()));
                    }
                }
            }
        }
        for (int i = 0; i<listOfDocs.size();i++){
            Document doc = listOfDocs.get(i);
            deletedURIs.add(doc.getKey());
            if (!this.disk.contains(doc.getKey())) {
                this.currentDocCount--;
                this.byteCount -= this.getBytePerDoc(doc);
            }
            deleteFromTrie((DocumentImpl) doc);
            HashMap <String,String> oldMetaData= doc.getMetadata();
                for (Map.Entry<String, String> entry : oldMetaData.entrySet()){
                    String key = entry.getKey();
                    String value = entry.getValue();
                    MetaNode node = new MetaNode(value, doc.getKey());
                    this.metaTrie.delete(key, node);
                }
                try {
                    deleteFromHeap(doc);
                }
                catch (NoSuchElementException e) 
                {}
            
            this.bTree.put(doc.getKey(), null);
            this.disk.remove(doc.getKey());
        }
        Set<Document> deletedDocs = new HashSet<>();
        deletedDocs.addAll(listOfDocs);
        this.carryOutUndo(deletedDocs);
        return deletedURIs;
    }

    public Set<URI> deleteAllWithPrefixAndMetadata(String keywordPrefix,Map<String,String> keysValues)throws IOException{ //TODO: just coppoed from above method, make sure correct
        Set<URI> deletedURIs = new HashSet<>();
        Comparator<URI> comparator = new CustomComparator<URI>(keywordPrefix, true);
        List <URI> listOfURIs = this.trie.getAllWithPrefixSorted(keywordPrefix, comparator);
        List<Document> listOfDocs = new ArrayList<>();
        for (Map.Entry<String, String> entry : keysValues.entrySet()){
            String key = entry.getKey();
            String value = entry.getValue();
            if (!this.metaTrie.get(key).isEmpty()){
                Set<MetaNode> nodes = this.metaTrie.get(key);
                for (MetaNode node:nodes){
                    if (node.getValue().equals(value) && listOfURIs.contains(node.getUri())){
                        listOfDocs.add(this.getDocFromTree(node.getUri()));
                    }
                }
            }
        }
        for (int i = 0; i<listOfDocs.size();i++){
            Document doc = listOfDocs.get(i);
            deletedURIs.add(doc.getKey());
            if (!this.disk.contains(doc.getKey())) {
                this.currentDocCount--;
                this.byteCount -= this.getBytePerDoc(doc);
            }
            deleteFromTrie((DocumentImpl) doc);
            HashMap <String,String> oldMetaData= doc.getMetadata();
                for (Map.Entry<String, String> entry : oldMetaData.entrySet()){
                    String key = entry.getKey();
                    String value = entry.getValue();
                    MetaNode node = new MetaNode(value, doc.getKey());
                    this.metaTrie.delete(key, node);
                }
                try {
                    deleteFromHeap(doc);
                }
                catch (NoSuchElementException e) 
                {}
            
            this.bTree.put(doc.getKey(), null);
            this.disk.remove(doc.getKey());
        }
        Set<Document> deletedDocs = new HashSet<>();
        deletedDocs.addAll(listOfDocs);
        this.carryOutUndo(deletedDocs);
        return deletedURIs;
    }

    // private void equalsMetaData(List<Document> listOfDocs, Map<String, String> keysValues) {
    //     List<Document> matchingDocs = new ArrayList<>();
    //     for (Document doc : listOfDocs) {
    //         boolean flag = true;
    //         for (Map.Entry<String, String> entry : keysValues.entrySet()) {
    //             String key = entry.getKey();
    //             String value = entry.getValue();
    //             String docValue = doc.getMetadataValue(key);
    //             if (docValue == null || !docValue.equals(value)) {
    //                 flag = false;
    //                 break;
    //             }
    //         }
    //         if (flag) {
    //             matchingDocs.add(doc);
    //         }
    //     }
    //     listOfDocs.clear();
    //     listOfDocs.addAll(matchingDocs);
    // }

    private void undoSet(Set<Document> docs){
        CommandSet<URI> commandSet = new CommandSet<>();
        for(Document doc: docs){
            Consumer<URI> putBack = (replace) -> {
                if ((this.maxDocBytes != null && this.getBytePerDoc(doc) <= this.maxDocBytes) || this.maxDocBytes == null){
                    DocumentImpl deletedDoc = (DocumentImpl) doc;
                    this.bTree.put(replace, deletedDoc);
                    this.currentDocCount++;
                    addToTrie(deletedDoc);
                    HashMap <String,String> newMetaData= doc.getMetadata();
                    for (Map.Entry<String, String> entry : newMetaData.entrySet()){
                        String key = entry.getKey();
                        String value = entry.getValue();
                        MetaNode node = new MetaNode(value, doc.getKey());
                        this.metaTrie.put(key, node);
                    }
                    this.byteCount+= this.getBytePerDoc(doc);
                    doc.setLastUseTime(System.nanoTime());
                    this.minHeap.insert(new Node(doc.getKey(),this.bTree));
                    this.stayInLimit();
                }
            };
            GenericCommand<URI> undoDelete = new GenericCommand<>(doc.getKey(), putBack);
            commandSet.addCommand(undoDelete);
        }
        this.stack.push(commandSet);
    }

    private void undoOneDoc(Set<Document> docs){
        List<Document> gettingDoc = new ArrayList<>();
        gettingDoc.addAll(docs);
        DocumentImpl doc = (DocumentImpl)gettingDoc.get(0); //is this an issue?
        Consumer<URI> putBack = (replace) ->{
            if ((this.maxDocBytes != null && this.getBytePerDoc(doc) <= this.maxDocBytes) || this.maxDocBytes == null){
                this.bTree.put(replace, doc);
                this.currentDocCount++;
                addToTrie(doc);
                HashMap <String,String> newMetaData= doc.getMetadata();
                for (Map.Entry<String, String> entry : newMetaData.entrySet()){
                    String key = entry.getKey();
                    String value = entry.getValue();
                    MetaNode node = new MetaNode(value, doc.getKey());
                    this.metaTrie.put(key, node);
                }
                this.byteCount+= this.getBytePerDoc(doc);
                doc.setLastUseTime(System.nanoTime());
                this.minHeap.insert(new Node(doc.getKey(),this.bTree));
                this.stayInLimit();
            }
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
            trie.put(word,document.getKey());
        }
    }
    private void deleteFromTrie(DocumentImpl document){
        Set<String> docWords = document.getWords();
        for (String word: docWords){
            trie.delete(word,document.getKey());
        }
    }

    private class CustomComparator<URI> implements Comparator<URI>{
       
        private String keyWord;
        private boolean isPrefix;

        private CustomComparator(String keyWord, boolean isPrefix){
            this.keyWord = keyWord;
            this.isPrefix=isPrefix;
        }
        @Override
        public int compare(URI doc1, URI doc2) {
            if (isPrefix){
                return compareForPrefix(doc1,doc2);
            }
            else{
                if (((Document) getDocFromTree((java.net.URI) doc1)).wordCount(keyWord) < ((Document) getDocFromTree((java.net.URI) doc2)).wordCount(keyWord)) {
                    return 1;
                }
                if (((Document) getDocFromTree((java.net.URI) doc1)).wordCount(keyWord) > ((Document) getDocFromTree((java.net.URI) doc2)).wordCount(keyWord)) {
                    return -1;
                }
                return 0;
            }
        }

        private int compareForPrefix(URI doc1, URI doc2){
            Set<String> set1 = ((Document) getDocFromTree((java.net.URI) doc1)).getWords();
            int count1 = 0;
            int count2 = 0;
            for(String word: set1) {
                if(word.startsWith(keyWord)) {
                    count1+=((Document) getDocFromTree((java.net.URI) doc1)).wordCount(word); 
                }
            }
            Set<String> set2 = ((Document) getDocFromTree((java.net.URI) doc2)).getWords();
            for(String word: set2) {
                if(word.startsWith(keyWord)) {
                    count2+=((Document) getDocFromTree((java.net.URI) doc2)).wordCount(word); 
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
        //**********STAGE 5 ADDITIONS
    
    /**
     * set maximum number of documents that may be stored
     * @param limit
     * @throws IllegalArgumentException if limit < 1
     */
    public void setMaxDocumentCount(int limit){
        if (limit < 1){
            throw new IllegalArgumentException();
        }
        this.maxDocCount = limit;
        this.stayInLimit();
    }

    /**
     * set maximum number of bytes of memory that may be used by all the documents in memory combined
     * @param limit
     * @throws IllegalArgumentException if limit < 1
     */
    public void setMaxDocumentBytes(int limit){
        if (limit < 1){
            throw new IllegalArgumentException();
        }
        this.maxDocBytes = limit;
        this.stayInLimit();
    }

    private Set<URI> stayInLimit(){
        Set<URI> uris = new HashSet<>();
        if (this.maxDocBytes != null){
            while (this.byteCount>this.maxDocBytes){
                Node nodeToDelete = this.minHeap.remove();
                URI deletedDocURI = nodeToDelete.uri;
                uris.add(deletedDocURI);
                this.byteCount-= this.getBytePerDoc(this.bTree.get(deletedDocURI));
                try {
                    this.bTree.moveToDisk(deletedDocURI);
                    this.disk.add(deletedDocURI);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                this.currentDocCount--;
                //this.bTree.put(deletedDocURI, null);
                this.removeDeleteFromUndoStack(deletedDocURI); //TODO: go over this
            }
        }
        if (this.maxDocCount != null){
            while(this.currentDocCount > this.maxDocCount) {
                Node nodeToDelete = this.minHeap.remove();
                URI deletedDocURI = nodeToDelete.uri;
                uris.add(deletedDocURI);
                this.byteCount-= this.getBytePerDoc(this.bTree.get(deletedDocURI));
                try {
                    this.bTree.moveToDisk(deletedDocURI);
                    this.disk.add(deletedDocURI);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                this.currentDocCount--;
                //this.bTree.put(deletedDocURI, null);
                this.removeDeleteFromUndoStack(deletedDocURI); //TODO: go over this
            }
        }
        return uris;
    }
    private void removeDeleteFromUndoStack(URI uri){ 
        StackImpl<Undoable> tempStack = new StackImpl<>();
        while (this.getPeekToUndo(uri, tempStack) != 0){ 
            if (this.getPeekToUndo(uri, tempStack) == 2){
                this.stack.pop();
            }
            else{
                CommandSet<URI> commands = (CommandSet<URI>)this.stack.pop();
                if (!commands.isEmpty()){
                    Iterator<GenericCommand<URI>> iterator = commands.iterator();
                    while (iterator.hasNext()) {
                        GenericCommand<URI> generic = iterator.next();
                        if (generic.getTarget().equals(uri)){
                            iterator.remove();
                            break;
                        }
                    }
                }
                this.stack.push(commands);
            }
        }
        while (tempStack.peek() != null) {
            this.stack.push(tempStack.pop());
        }
    }
    private void deleteFromHeap(Document doc){
        doc.setLastUseTime(0);
        try{
            this.minHeap.reHeapify(new Node(doc.getKey(),this.bTree));
            }catch(NoSuchElementException e){}
        this.minHeap.remove();
    }
    private void setUpdatedTimes(List<Document> listOfDocs){
        long time = System.nanoTime();
        if (!listOfDocs.isEmpty()){
            for(Document doc: listOfDocs) {
                if (this.disk.contains(doc.getKey())) {
                    this.currentDocCount++;
                    this.byteCount+=this.getBytePerDoc(doc);
                    doc.setLastUseTime(time);
                    try{
                        this.minHeap.reHeapify(new Node(doc.getKey(),this.bTree));
                        }catch(NoSuchElementException e){}
                    this.disk.remove(doc.getKey());
                }
                else{
                    doc.setLastUseTime(time);
                    try{
                        this.minHeap.reHeapify(new Node(doc.getKey(),this.bTree));
                        }catch(NoSuchElementException e){}
                }
            }
        }
        stayInLimit();
    }

    private boolean isTextDoc(Document doc){
        if (doc.getDocumentTxt() != null){
            return true;
        }
        else{
            return false;
        }
    }
    private int getBytePerDoc(Document doc){
        if (this.isTextDoc(doc)){
            return doc.getDocumentTxt().getBytes().length;
        }
        else{
            return doc.getDocumentBinaryData().length;
        }
    }
    private Document getDocFromTree(URI uri) {
        return this.bTree.get(uri);
    }
    private class Node implements Comparable<Node> {
        private long lastTimeUsed;
        private URI uri;
        private BTreeImpl<URI, Document> BTreeReference;
        private Node(URI uri, BTreeImpl<URI, Document> bTree) {
            this.uri = uri;
            this.BTreeReference = bTree;
            lastTimeUsed = System.nanoTime();
        }
        private URI getUri(){
            return this.uri;
        }
        private long getLastUseTime() {
            updateLastUseTime();
            return this.lastTimeUsed;
        }
        private void updateLastUseTime() {
            this.lastTimeUsed = this.BTreeReference.get(this.uri).getLastUseTime();
        }

        @Override
        public int compareTo(Node node) { //TODO: is this taking it out of disk and therefore an issue?
            if (node == null) {
                throw new NullPointerException();
            }
            updateLastUseTime();
            node.updateLastUseTime();
            if (this.lastTimeUsed > node.getLastUseTime()) {
                return 1;
            }
            if (this.lastTimeUsed < node.getLastUseTime()) {
                return -1;
            }
            return 0;
        }

        @Override
        public boolean equals (Object node){
            if (this == node){
                return true;
            }
            if (node == null || this.getClass() != node.getClass()) {
                return false;
            }
            Node compare = (Node) node;
            return this.getUri().toString().equals(compare.getUri().toString());
        }
    }
   
}