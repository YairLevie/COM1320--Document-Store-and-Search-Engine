package edu.yu.cs.com1320.project.stage1;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import edu.yu.cs.com1320.project.stage1.DocumentStore.DocumentFormat;
import edu.yu.cs.com1320.project.stage1.impl.DocumentStoreImpl;

public class DocumentStoreImplTest {
@BeforeEach
void setup(){
}
@Test
public void testPutNewDocumentTXT() throws IOException {
    DocumentStoreImpl docStore = new DocumentStoreImpl();
    URI uri1 = URI.create("http://example.com/document");
    InputStream inputStream = new ByteArrayInputStream("Document Content".getBytes());
    DocumentFormat format = DocumentFormat.TXT;
    assertEquals(0, docStore.put(inputStream, uri1, format));
}
@Test
public void testPutNewDocumentBinary() throws IOException {
    DocumentStore docStore = new DocumentStoreImpl();
    URI uri1 = URI.create("http://example.com/document");
    InputStream inputStream = new ByteArrayInputStream("Document Content".getBytes());
    DocumentFormat format = DocumentFormat.BINARY;
    assertEquals(0, docStore.put(inputStream, uri1, format));
}
@Test
public void testPutUpdateDocument() throws IOException {
    DocumentStore documentStore = new DocumentStoreImpl();
    URI uri = URI.create("http://example.com/document");
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
    DocumentStore documentStore = new DocumentStoreImpl();
    URI uri = URI.create("http://example.com/document");
    InputStream inputStream = new ByteArrayInputStream("Document Content".getBytes());
    DocumentFormat format = DocumentFormat.TXT;
    // put doc
    documentStore.put(inputStream, uri, format);
    // delete doc
    int result = documentStore.put(null, uri, format);
    // expect hashcode of deleted doc
    assertNotEquals(0, result);
}

@Test
public void testPutDeleteNonExistingDocument() throws IOException {
    DocumentStore documentStore = new DocumentStoreImpl();
    URI uri = URI.create("http://example.com/document");
    DocumentFormat format = DocumentFormat.BINARY;
    // delete non-existing doc
    int result = documentStore.put(null, uri, format);
    // no prev doc to delete
    assertEquals(0, result);
}

@Test
public void testPutInvalidArguments() {
    DocumentStore documentStore = new DocumentStoreImpl();
    URI uri = URI.create("http://example.com/document");
    InputStream inputStream = new ByteArrayInputStream("Document Content".getBytes());
    DocumentFormat format = DocumentFormat.TXT;
    //learned a new format for testing exceptions:)
    assertThrows(IllegalArgumentException.class, () -> documentStore.put(inputStream, null, format));
    assertThrows(IllegalArgumentException.class, () -> documentStore.put(inputStream, URI.create(""), format));
    assertThrows(IllegalArgumentException.class, () -> documentStore.put(inputStream, uri, null));
}
@Test
public void testSetMetadata() throws IOException {
    DocumentStore documentStore = new DocumentStoreImpl();
    URI uri = URI.create("http://example.com/document");
    String key = "Author";
    String value = "moishe Doe";
    InputStream inputStream = new ByteArrayInputStream("Document Content".getBytes());
    documentStore.put(inputStream,uri, DocumentFormat.TXT);
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

}

