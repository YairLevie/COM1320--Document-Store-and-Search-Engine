package edu.yu.cs.com1320.project.stage4;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import edu.yu.cs.com1320.project.stage4.DocumentStore.DocumentFormat;
import edu.yu.cs.com1320.project.stage4.impl.DocumentImpl;
import edu.yu.cs.com1320.project.stage4.impl.DocumentStoreImpl;

public class DocumentStoreImplTest {
    DocumentStoreImpl documentStore;
    InputStream inputStream;
    URI uri;
    DocumentFormat text;
    DocumentFormat binary;
@BeforeEach
void setup(){
    documentStore = new DocumentStoreImpl();
    uri = URI.create("http://example.com/document");
    inputStream = new ByteArrayInputStream("Document Content".getBytes());
    text = DocumentFormat.TXT;
    binary = DocumentFormat.BINARY;
}
@Test
public void testPutNewDocumentTXT() throws IOException {
    assertEquals(0, documentStore.put(inputStream, uri, text));
}
@Test
public void testPutNewDocumentBinary() throws IOException {
    assertEquals(0, documentStore.put(inputStream, uri, binary));
}
@Test
public void testPutUpdateDocument() throws IOException {
    InputStream inputStream1 = new ByteArrayInputStream("Document Content 1".getBytes());
    InputStream inputStream2 = new ByteArrayInputStream("Document Content 2".getBytes());
    DocumentFormat format = DocumentFormat.TXT;
    // put initial doc
    documentStore.put(inputStream1, uri, format);
    // update doc
    int result = documentStore.put(inputStream2, uri, format);
    // expect hashcode of deleted doc
    assertNotEquals(0, result);
}
@Test
public void testPutDeleteDocument() throws IOException {
    // put doc
    documentStore.put(inputStream, uri, text);
    // delete doc
    int result = documentStore.put(null, uri, text);
    // expect hashcode of deleted doc
    assertNotEquals(0, result);
}

@Test
public void testPutDeleteNonExistingDocument() throws IOException {
    // delete non-existing doc
    int result = documentStore.put(null, uri, binary);
    // no prev doc to delete
    assertEquals(0, result);
}

@Test
public void testPutInvalidArguments() {
    //learned a new format for testing exceptions:)
    assertThrows(IllegalArgumentException.class, () -> documentStore.put(inputStream, null, text));
    assertThrows(IllegalArgumentException.class, () -> documentStore.put(inputStream, URI.create(""), text));
    assertThrows(IllegalArgumentException.class, () -> documentStore.put(inputStream, uri, null));
}
@Test
public void testSetMetadata() throws IOException {
    String key = "Author";
    String value = "moishe Doe";
    documentStore.put(inputStream,uri, text);
    assertEquals(null,documentStore.setMetadata(uri, key, value));
    // add doc
    documentStore.put(new ByteArrayInputStream("Document Content".getBytes()), uri, DocumentFormat.TXT);
    // Set metadata existing doc
    assertNull(documentStore.setMetadata(uri, key, value));
    // Set metadata with a different value/key
    assertEquals("moishe Doe", documentStore.setMetadata(uri, key, "Yoisef Doe"));
    assertNull(documentStore.setMetadata(uri, "Year", "2022"));
    // invalid arguments:
    assertThrows(IllegalArgumentException.class, () -> documentStore.setMetadata(null, key, value));
    assertThrows(IllegalArgumentException.class, () -> documentStore.setMetadata(URI.create(""), key, value));
    assertThrows(IllegalArgumentException.class, () -> documentStore.setMetadata(uri, null, value));
    assertThrows(IllegalArgumentException.class, () -> documentStore.setMetadata(uri, "", value));
    assertThrows(IllegalArgumentException.class, () -> documentStore.setMetadata(URI.create("http://example.com/nonexistent"), key, value));
}
@Test
public void testGetMetadata() throws IOException {
    // Set up a doc store + add a doc with metadata
    documentStore.put(inputStream,uri, text);
    documentStore.setMetadata(uri,"author", "John Doe");
    documentStore.setMetadata(uri,"year", "2022");

    assertEquals("John Doe", documentStore.getMetadata(uri, "author"));
    assertEquals("2022", documentStore.getMetadata(uri, "year"));

    //invalid arguments
    assertNull(documentStore.getMetadata(uri, "unknownKey"));
    URI unknownUri = URI.create("http://example.com/unknown");
    assertThrows(IllegalArgumentException.class, () -> {
        documentStore.getMetadata(unknownUri, "author");
    });
    assertThrows(IllegalArgumentException.class, () -> {
        documentStore.getMetadata(null, "author");
    });
    URI blankUri = URI.create("");
    assertThrows(IllegalArgumentException.class, () -> {
        documentStore.getMetadata(blankUri, "author");
    });
    assertThrows(IllegalArgumentException.class, () -> {
        documentStore.getMetadata(uri, null);
    });
    assertThrows(IllegalArgumentException.class, () -> {
        documentStore.getMetadata(uri, "");
    });
    }
    @Test
    public void testGet() throws IOException {
        documentStore.put(inputStream,uri, DocumentFormat.TXT);
        DocumentImpl document = new DocumentImpl(uri,"Document Content");
        // test retrieving the doc using get
        assertEquals(document, documentStore.get(uri));
        // test handling unknown URI
        URI unknownUri = URI.create("http://example.com/unknown");
        assertNull(documentStore.get(unknownUri));
        // test handling null URI
        assertNull(documentStore.get(null));
        // test handling blank URI
        URI blankUri = URI.create("");
        assertNull(documentStore.get(blankUri));
    }

    @Test
    public void testDelete() throws IOException {
        documentStore.put(inputStream,uri, DocumentFormat.TXT);
        // test deleting the doc
        assertTrue(documentStore.delete(uri));
        assertNull(documentStore.get(uri));
        URI unknownUri = URI.create("http://example.com/unknown");
        assertFalse(documentStore.delete(unknownUri));
        assertFalse(documentStore.delete(null));
        URI blankUri = URI.create("");
        assertFalse(documentStore.delete(blankUri));
    }
    /*  
    ALL COMMENTED TESTS use a method i made private to test functionality of the STACK within documentStore
    @Test
    public void testPutAddsToStack() throws IOException{
        documentStore.put(inputStream,uri, text);
        StackImpl<Command> stack = documentStore.getStack();
        assertEquals(1, stack.size());
        Command undoCommand = stack.peek();
        assertNotNull(undoCommand);
        assertEquals(uri, undoCommand.getUri());
        documentStore.put(new ByteArrayInputStream("Document Content".getBytes()),uri, text);
        assertEquals(2, stack.size());
        documentStore.put(new ByteArrayInputStream("Document Content".getBytes()),URI.create("www.newURI.com"), text);
        assertEquals(3, stack.size());
    }
    @Test
    public void testDeleteAddsToStack() throws IOException{
        StackImpl<Command> stack = documentStore.getStack();
        documentStore.put(inputStream,uri, text);
        assertEquals(1, stack.size());
        documentStore.put(null,uri,text);
        assertEquals(2, stack.size());
    }
    @Test
    public void testSetMetaDataAddsToStack() throws IOException{
        StackImpl<Command> stack = documentStore.getStack();
        documentStore.put(inputStream,uri, text);
        String key = "Author";
        String value = "moishe Doe";
        documentStore.setMetadata(uri, key, value);
        assertEquals(2, stack.size());
        documentStore.setMetadata(uri, key, "Yosef");
        assertEquals(3, stack.size());
    }
    @Test
    public void testGenericPutNewUndoRemovesFromStack() throws IOException{
        //put a new doc in the store
        documentStore.put(inputStream,uri, text);
        StackImpl<Command> stack = documentStore.getStack();
        //make sure it added to stack
        assertEquals(1, stack.size());
        //make sure the stack is holding the correct values
        Command undoCommand = stack.peek();
        assertNotNull(undoCommand);
        assertEquals(uri, undoCommand.getUri());
        //update the Document value of the fist doc
        documentStore.put(new ByteArrayInputStream("Document Content 2".getBytes()),uri, text);
        //hold the new doc in a variable for future testing
        Command newCommand = stack.peek();
        //make sure it added a stack
        assertEquals(2, stack.size());
        //add another doc and make sure it added to stack
        URI newURI = URI.create("www.newURI.com");
        documentStore.put(new ByteArrayInputStream("Document Content".getBytes()),newURI, text);
        assertEquals(3, stack.size());
        //make sure the new addition exists in my document store before i delete it
        DocumentImpl expectedDocument = new DocumentImpl (URI.create("www.newURI.com"),"Document Content");
        DocumentImpl actualDoc = (DocumentImpl) documentStore.get(newURI);
        assertEquals(expectedDocument,actualDoc);
        //delete the document i just put in
        documentStore.undo();
        //make sure it was deleted from the document store and the Command Stack
        assertEquals(2, stack.size());
        assertEquals(newCommand, stack.peek()); 
        assertNull(documentStore.get(newURI));
    }
    */
    @Test
    //tests when you undo a put replace (ie when you put it initally there was already a doc there) return the document back to its previous value
    public void testGenericPutReplaceUndo() throws IOException{
         //put a new doc in the store
         documentStore.put(inputStream,uri, text);
         //add a new doc with the same uri... check it updates the document is holds and adds to stack
         documentStore.put(new ByteArrayInputStream("Document Content 2".getBytes()),uri, text);
         
         DocumentImpl expectedDoc = new DocumentImpl(uri, "Document Content 2");
         assertEquals(expectedDoc, documentStore.get(uri));
         //test that an undo removes from stack and reverts the Document stored at the URI to the previous Document
         documentStore.undo();
         DocumentImpl initialDoc = new DocumentImpl(uri, "Document Content");
         assertEquals(initialDoc,documentStore.get(uri));
    }
    @Test
    //tests when you undo a put replace (ie when you put it initally there was already a doc there) return the document back to its previous value
    public void testSpecificPutNewUndo() throws IOException{
         //put a new doc in the store
         documentStore.put(inputStream,uri, text);
         //add a new doc with the different uri... check it updates the document is holds and adds to stack
         URI newURI = URI.create("www.newURI.com");
         documentStore.put(new ByteArrayInputStream("Document Content 2".getBytes()),newURI, text);
       
         DocumentImpl expectedDoc = new DocumentImpl(newURI, "Document Content 2");
         assertEquals(expectedDoc, documentStore.get(newURI));
         //test that specific undo to inital document. Document store should now ONLY hold the second document
         documentStore.undo(uri);
         assertNull(documentStore.get(uri));
    }
    @Test
public void testSpecificPutReplaceUndo() throws IOException {
    // Put a new document in the store
    documentStore.put(inputStream, uri, text);
    // Add another document with the same URI (replace)
    URI newURI = URI.create("www.newURI.com");
    documentStore.put(new ByteArrayInputStream("Document Content 2".getBytes()), uri, text);
    // Add a new document with a different URI
    documentStore.put(new ByteArrayInputStream("Document Content 3".getBytes()), newURI, text);
    // Ensure the document store contains the updated document (the second put)
    DocumentImpl expectedDoc = new DocumentImpl(uri, "Document Content 2");
    assertEquals(expectedDoc, documentStore.get(uri));
    // Test specific undo to the initial document. It should revert the Document back to its initial state ie before the second put
    documentStore.undo(uri);
    assertNotNull(documentStore.get(uri));
}
@Test
public void testUndoDelete() throws IOException {
    // Put two documents in the store
    documentStore.put(inputStream, uri, text);
    //add another doc
    URI anotherUri = URI.create("http://example.com/another-document");
    InputStream anotherInputStream = new ByteArrayInputStream("Another Document Content".getBytes());
    documentStore.put(anotherInputStream, anotherUri, text);
    // delete the first document
    documentStore.put(null,uri,text);
    assertNull(documentStore.get(uri));
    // Ensure the document store does not contain the deleted document
    assertNull(documentStore.get(uri));
    // Ensure the second document is still present
    assertNotNull(documentStore.get(anotherUri));
    // test undoing the delete operation
    documentStore.undo();
    // ensure both documents are present after undo
    assertNotNull(documentStore.get(uri));
    assertNotNull(documentStore.get(anotherUri));
    //delete the first doc again
    documentStore.put(null,uri,text);
    //add another doc
    URI thirdUri = URI.create("http://third.com/document");
    InputStream thirdInputStream = new ByteArrayInputStream("third Document Content".getBytes());
    documentStore.put(thirdInputStream, thirdUri, text);
    //check specific undo
    assertNull(documentStore.get(uri));
    documentStore.undo(uri);
    assertNotNull(documentStore.get(uri));
}
@Test
public void testGenericUndoSetMetadata() throws IOException {
    // put a new document in the store
    documentStore.put(inputStream, uri, text);
    // set metadata for the document
    String key = "Author";
    String value = "Moishe Doe";
    documentStore.setMetadata(uri, key, value);
    // ensure the metadata is correctly set
    assertEquals(value, documentStore.get(uri).getMetadataValue(key));
    // test undoing the setMetadata operation
    documentStore.undo();
    // ensure metadata is reverted to its previous state after undo
    assertNull(documentStore.get(uri).getMetadataValue(key));
    //put back metadata
    documentStore.setMetadata(uri, key, value);
    //replace metadata
    documentStore.setMetadata(uri, key, "Yoisef");
    //undo the replace
    documentStore.undo();
    //make sure metadata was reverted
    assertEquals(value,documentStore.get(uri).getMetadataValue(key));
}
@Test
public void testSpecificUndoSetMetadataNew() throws IOException {
    // put a new document in the store
    documentStore.put(inputStream, uri, text);
    // set metadata for the document
    String key = "Author";
    String value = "Moishe Doe";
    documentStore.setMetadata(uri, key, value);
    // ensure the metadata is correctly set
    assertEquals(value, documentStore.get(uri).getMetadataValue(key));
    //add another doc to the store
    URI anotherUri = URI.create("http://example.com/another-document");
    InputStream anotherInputStream = new ByteArrayInputStream("Another Document Content".getBytes());
    documentStore.put(anotherInputStream, anotherUri, text);
    // test specific URI undoing the setMetadata operation
    documentStore.undo(uri);
    // Ensure metadata is reverted to its previous state after undo
    assertNull(documentStore.get(uri).getMetadataValue(key));
}
@Test
public void testSpecificUndoSetMetadataReplace() throws IOException {
        // put a new document in the store
        documentStore.put(inputStream, uri, text);
        // set metadata for the document
        String key = "Author";
        String value = "Moishe Doe";
        documentStore.setMetadata(uri, key, value);
        //replace the metadata
        documentStore.setMetadata(uri, key, "Yoisef");
        // ensure the metadata is correctly set
        assertEquals("Yoisef", documentStore.get(uri).getMetadataValue(key));
        //add another doc to the store
        URI anotherUri = URI.create("http://example.com/another-document");
        InputStream anotherInputStream = new ByteArrayInputStream("Another Document Content".getBytes());
        documentStore.put(anotherInputStream, anotherUri, text);
        // Test specific URI undoing the setMetadata operation
        documentStore.undo(uri);
        // Ensure metadata is reverted to its previous state after undo
        assertEquals("Moishe Doe",documentStore.get(uri).getMetadataValue(key));
        documentStore.undo();
        assertNull(documentStore.get(anotherUri));
        documentStore.undo();
        assertNull(documentStore.get(uri).getMetadataValue(key));
}
@Test
public void testDeleteDocWithMetaData() throws IOException {
    // put a new document in the store
    documentStore.put(inputStream, uri, text);
    // set metadata for the document
    String key = "Author";
    String value = "Moishe Doe";
    documentStore.setMetadata(uri, key, value);
    documentStore.put(null, uri, text);
    assertNull(documentStore.get(uri));
    documentStore.undo();
    assertNotNull(documentStore.get(uri));
    assertEquals("Moishe Doe", documentStore.get(uri).getMetadataValue(key));
}

@Test
public void testSearchByKeywordAndMetadata() throws IOException {
    // put a new document in the store
    documentStore.put(inputStream, uri, text);
    DocumentImpl doc = new DocumentImpl(uri, "Document Content");

    // set metadata for the document
    String key = "Author";
    String value = "Moishe Doe";

    documentStore.setMetadata(uri, key, value);
    documentStore.setMetadata(uri,"Yair", "Levie");

    // ensure the metadata is correctly set
    assertEquals(value, documentStore.get(uri).getMetadataValue(key));
    //add another doc to the store
    URI anotherUri = URI.create("http://example.com/another-document");
    InputStream anotherInputStream = new ByteArrayInputStream("Another Document Content".getBytes());
    documentStore.put(anotherInputStream, anotherUri, text);
    DocumentImpl doc2 = new DocumentImpl(anotherUri, "Another Document Content");
    documentStore.setMetadata(anotherUri,"Yair", "Levie");
    documentStore.setMetadata(anotherUri, key, value);
    // test specific URI undoing the setMetadata operation
    
    HashMap<String, String> map = new HashMap<>();
    map.put("Author","Moishe Doe");
    map.put("Yair", "Levie");
    
    List<Document> actual = new ArrayList<> (documentStore.searchByKeywordAndMetadata("Content",map));
    // Ensure metadata is reverted to its previous state after undo
    List<DocumentImpl> expected = new ArrayList<>();
    expected.add(doc);
    expected.add(doc2);
    assertEquals(expected.size(),actual.size());
}

@Test
public void testSearchByPrefixAndMetadata() throws IOException {
    // put a new document in the store
    documentStore.put(inputStream, uri, text);
    DocumentImpl doc = new DocumentImpl(uri, "Document Content");

    // set metadata for the document
    String key = "Author";
    String value = "Moishe Doe";

    documentStore.setMetadata(uri, key, value);
    documentStore.setMetadata(uri,"Yair", "Levie");

    // ensure the metadata is correctly set
    assertEquals(value, documentStore.get(uri).getMetadataValue(key));
    //add another doc to the store
    URI anotherUri = URI.create("http://example.com/another-document");
    InputStream anotherInputStream = new ByteArrayInputStream("Another Document Content".getBytes());
    documentStore.put(anotherInputStream, anotherUri, text);
    DocumentImpl doc2 = new DocumentImpl(anotherUri, "Another Document Content");
    documentStore.setMetadata(anotherUri,"Yair", "Levie");
    documentStore.setMetadata(anotherUri, key, value);

    URI thirdUri = URI.create("http://example.com/another-document");
    InputStream thirdInputStream = new ByteArrayInputStream("Another Document".getBytes());
    documentStore.put(thirdInputStream, thirdUri, text);
    documentStore.setMetadata(thirdUri,"Yair", "Levie");
    documentStore.setMetadata(thirdUri, key, value);
    
    HashMap<String, String> map = new HashMap<>();
    map.put("Author","Moishe Doe");
    map.put("Yair", "Levie");
    
    List<Document> actual = new ArrayList<> (documentStore.searchByPrefixAndMetadata("Con",map));
    // Ensure metadata is reverted to its previous state after undo
    List<DocumentImpl> expected = new ArrayList<>();
    expected.add(doc);
    expected.add(doc2);
    assertEquals(expected.size(),actual.size());
    assertEquals(uri, expected.get(0).getKey());
    assertEquals(anotherUri, expected.get(1).getKey());
}
@Test
public void testDeleteAllWithPrefixAndMetadata() throws IOException {
    // put a new document in the store
    documentStore.put(inputStream, uri, text);
    DocumentImpl doc = new DocumentImpl(uri, "Document Content");

    // set metadata for the document
    String key = "Author";
    String value = "Moishe Doe";

    documentStore.setMetadata(uri, key, value);
    documentStore.setMetadata(uri,"Yair", "Levie");

    // ensure the metadata is correctly set
    assertEquals(value, documentStore.get(uri).getMetadataValue(key));
    //add another doc to the store
    URI anotherUri = URI.create("http://example.com/another-document");
    InputStream anotherInputStream = new ByteArrayInputStream("Another Document Content".getBytes());
    documentStore.put(anotherInputStream, anotherUri, text);
    DocumentImpl doc2 = new DocumentImpl(anotherUri, "Another Document Content");
    documentStore.setMetadata(anotherUri,"Yair", "Levie");
    documentStore.setMetadata(anotherUri, key, value);

    URI thirdUri = URI.create("http://example.com/another-document");
    InputStream thirdInputStream = new ByteArrayInputStream("Another Document".getBytes());
    documentStore.put(thirdInputStream, thirdUri, text);
    documentStore.setMetadata(thirdUri,"Yair", "Levie");
    documentStore.setMetadata(thirdUri, key, value);
    
    HashMap<String, String> map = new HashMap<>();
    map.put("Author","Moishe Doe");
    map.put("Yair", "Levie");
    
    Set<URI> actual = new HashSet<URI> ();
    actual.addAll(documentStore.deleteAllWithPrefixAndMetadata("Con",map));
    // Ensure metadata is reverted to its previous state after undo
    Set<URI> expected = new HashSet<>();
    expected.add(anotherUri);
    expected.add(uri);
    assertEquals(expected.size(),actual.size());
    assertTrue(expected.contains(uri));
    assertTrue(expected.contains(anotherUri));
}
@Test
public void testSearchByMetaData() throws IOException {
    // put a new document in the store
    documentStore.put(inputStream, uri, text);
    // set metadata for the document
    String key = "Author";
    String value = "Moishe Doe";

    documentStore.setMetadata(uri, key, value);

    // ensure the metadata is correctly set
    assertEquals(value, documentStore.get(uri).getMetadataValue(key));
    //add another doc to the store
    URI anotherUri = URI.create("http://example.com/another-document");
    InputStream anotherInputStream = new ByteArrayInputStream("Another Document Content".getBytes());
    documentStore.put(anotherInputStream, anotherUri, text);
    
    //create 
    HashMap<String, String> map = new HashMap<>();
    map.put("Author","Moishe Doe");
    
    List<Document> matching = documentStore.searchByMetadata(map);
    // Ensure metadata is reverted to its previous state after undo
    assertNotNull(matching);
}
}