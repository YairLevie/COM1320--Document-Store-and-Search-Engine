package edu.yu.cs.com1320.project.stage2;

import org.junit.jupiter.api.Test;
import edu.yu.cs.com1320.project.HashTable;
import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.stage2.impl.DocumentImpl;
import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;


import org.junit.jupiter.api.BeforeEach;
public class DocumentImplTest {
    private Document uriDoc1;
    private Document byteDoc2;
    //private Document doc3;

@BeforeEach
void setup(){
    //create a doc with URI constructor
    URI uri1 = URI.create("http://Hamlet.com");
    uriDoc1 = new DocumentImpl(uri1, "Sample content");
    //create a doc with byte[] constructor
    URI uri2 = URI.create("http://practice.com");
    byte[] content = "Johnny's arm".getBytes();
    byteDoc2 = new DocumentImpl(uri2, content);

    //URI uri3 = URI.create("http://example.com/path?query=value#fragment");
   // doc3 = new DocumentImpl(uri3, "Johnny's arm");
}
@Test
public void testCreatingDocumentWithInvalidArguments(){
    //create invalid inputs
    URI blankURI = URI.create("");
    byte[] validByte = "Johnny's arm".getBytes();
    byte[] nullByte = null;
    byte[] blankByte = "".getBytes();
    String nullStr = null;
    String blankStr = "";
    URI validURI = URI.create("http://HOTDOGSSSSS.com");
    
    //test null URI for both constructors
    assertThrows(IllegalArgumentException.class, () -> {
        new DocumentImpl(null, "Sample content");
   		});
    assertThrows(IllegalArgumentException.class, () -> {
        new DocumentImpl(null, validByte);
   		});
    //test blank URI for both constructors
    assertThrows(IllegalArgumentException.class, () -> {
        new DocumentImpl(blankURI, "Sample content");
   		});
    assertThrows(IllegalArgumentException.class, () -> {
        new DocumentImpl(blankURI, validByte);
   		});
    //test null and Blank bytes
    assertThrows(IllegalArgumentException.class, () -> {
        new DocumentImpl(validURI, nullByte);
   		});
    assertThrows(IllegalArgumentException.class, () -> {
        new DocumentImpl(validURI, blankByte);
   		});
    // test null and Blank Strings
    assertThrows(IllegalArgumentException.class, () -> {
        new DocumentImpl(validURI, blankStr);
   		});
    assertThrows(IllegalArgumentException.class, () -> {
        new DocumentImpl(validURI, nullStr);
   		}); 
}
@Test
public void testSettingMetaValueWithValidUniqueUriDoc(){
    uriDoc1.setMetadataValue("Title", "Hamlet");
    uriDoc1.setMetadataValue("Author", "Shakespeare");
    uriDoc1.setMetadataValue("Published", "1603");

    HashTable <String,String> metaDataActual = uriDoc1.getMetadata();
    HashTable <String, String> metaDataExpected = new HashTableImpl<>();
    metaDataExpected.put("Title", "Hamlet");
    metaDataExpected.put("Author", "Shakespeare");
    metaDataExpected.put("Published", "1603");

    assertTrue(metaDataActual.size()==metaDataExpected.size());
    //did this manually because i didnt know i can use .equals()
    assertTrue(metaDataActual.get("Title").equals(metaDataExpected.get("Title")));
    assertTrue(metaDataActual.get("Author").equals(metaDataExpected.get("Author")));
    assertTrue(metaDataActual.get("Published").equals(metaDataExpected.get("Published"))); 
    //assertTrue(metaDataActual.equals(metaDataExpected));
}
@Test
public void testSettingMetaValueWithValidUniqueByteDoc(){
    //tests if no old value, returns null
    assertEquals(null,byteDoc2.setMetadataValue("Title", "Hamlet"));
    assertEquals(null,byteDoc2.setMetadataValue("Author", "Shakespeare"));
    assertEquals(null,byteDoc2.setMetadataValue("Published", "1603"));

    //tests if returns old value if replace a value
    assertEquals("1603", byteDoc2.setMetadataValue("Published", "1602"));

    //uses getMetadata() to copy over the metadata
    HashTable <String,String> metaDataActual = byteDoc2.getMetadata();
    HashTable <String, String> metaDataExpected = new HashTableImpl<>();
    metaDataExpected.put("Title", "Hamlet");
    metaDataExpected.put("Author", "Shakespeare");
    metaDataExpected.put("Published", "1602");

    //tests if it properly adds to metavalue, uses .getMetadataValue() method
    assertTrue(metaDataActual.size()==metaDataExpected.size());
    assertTrue(metaDataActual.get("Title").equals(byteDoc2.getMetadataValue("Title")));
    assertTrue(metaDataActual.get("Author").equals(byteDoc2.getMetadataValue("Author")));
    assertTrue(metaDataActual.get("Published").equals(byteDoc2.getMetadataValue("Published")));
    assertNull(byteDoc2.getMetadataValue("harold")); 
}
@Test
public void testSettingMetaValueInvalidArguments(){
    assertThrows(IllegalArgumentException.class, () -> {
        uriDoc1.setMetadataValue(null, "Chazara is KEY");
   		}); 
    assertThrows(IllegalArgumentException.class, () -> {
        byteDoc2.setMetadataValue("", "Veharev Na");
   		}); 
}
@Test
public void testGetMetadataValue(){
    uriDoc1.setMetadataValue("Producer", "Hashem");
    assertEquals("Hashem", uriDoc1.getMetadataValue("Producer"));
    assertThrows(IllegalArgumentException.class, () -> {
        uriDoc1.getMetadataValue("");
   		}); 
    assertThrows(IllegalArgumentException.class, () -> {
        uriDoc1.getMetadataValue(null);
   		}); 
    assertNull(uriDoc1.getMetadataValue("Director"));
}
@Test
public void testGetMetadata(){
    //setting metadata to byte doc
    assertEquals(null,byteDoc2.setMetadataValue("Title", "Hamlet"));
    assertEquals(null,byteDoc2.setMetadataValue("Author", "Shakespeare"));
    assertEquals(null,byteDoc2.setMetadataValue("Published", "1603"));

    HashTable <String,String> copiedValues = byteDoc2.getMetadata();
    copiedValues.put("Hello", "Goodbye");
    assertNotEquals(copiedValues.size(), byteDoc2.getMetadata().size());
    copiedValues.put("Title", "Macbeth");
    assertNotEquals(byteDoc2.getMetadataValue("Title"), copiedValues.get("Title"));
}
@Test
public void testGetters(){
    assertEquals("Sample content", uriDoc1.getDocumentTxt());
    assertNull(uriDoc1.getDocumentBinaryData());
    byte[] expectedByteArray = "Johnny's arm".getBytes();
    assertArrayEquals(expectedByteArray, byteDoc2.getDocumentBinaryData());
    assertNull(byteDoc2.getDocumentTxt());
    URI expectedUri = URI.create("http://Hamlet.com");
    assertEquals(expectedUri, uriDoc1.getKey());
}
@Test
public void testHashcode(){
    URI uri = URI.create("http://Hamlet.com");
    Document newDoc = new DocumentImpl(uri, "Sample contents");
    assertNotEquals(newDoc.hashCode(), uriDoc1.hashCode());
    URI anotherUri = URI.create("http://Hamlet.com");
    Document anotherDoc = new DocumentImpl(anotherUri, "Sample content");
    assertEquals(anotherDoc.hashCode(), uriDoc1.hashCode());
}
}