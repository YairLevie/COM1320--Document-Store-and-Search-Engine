package edu.yu.cs.com1320.project.stage6;

import edu.yu.cs.com1320.project.stage6.Document;
import edu.yu.cs.com1320.project.stage6.impl.DocumentImpl;
import edu.yu.cs.com1320.project.stage6.impl.DocumentPersistenceManager;

import org.junit.jupiter.api.*;
import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DocumentPersistenceManagerTest {

    private File baseDir;
    private DocumentPersistenceManager dpm;

    @BeforeEach
    public void setUp() {
        baseDir = new File("/Users/yairlevie/Desktop/ComputerScience/disk");
        baseDir.mkdirs();
        dpm = new DocumentPersistenceManager(baseDir);
    }

    @AfterEach
    public void tearDown() {
        deleteDirectory(baseDir);
    }

    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }

    @Test
    public void testSerializeAndDeserialize() throws Exception {
        URI uri = new URI("http://www.example.com/doc");
        String text = "This is a test document. This document is a test.";
        HashMap<String, Integer> wordMap = new HashMap<>();
        wordMap.put("This", 2);
        wordMap.put("is", 2);
        wordMap.put("a", 2);
        wordMap.put("test", 2);
        wordMap.put("document", 2);
        Document doc = new DocumentImpl(uri, text, wordMap);

        dpm.serialize(uri, doc);

        Document deserializedDoc = dpm.deserialize(uri);

        assertEquals(doc.getKey(), deserializedDoc.getKey());
        assertEquals(doc.getDocumentTxt(), deserializedDoc.getDocumentTxt());
        assertEquals(doc.getWordMap(), deserializedDoc.getWordMap());
        System.out.println(deserializedDoc.getWordMap().get("a"));
    }

    @Test
    public void testDelete() throws Exception {
        URI uri = new URI("http://www.example.com/doc");
        String text = "This is a test document.";
        HashMap<String, Integer> wordMap = new HashMap<>();
        wordMap.put("This", 1);
        wordMap.put("is", 1);
        wordMap.put("a", 1);
        wordMap.put("test", 1);
        wordMap.put("document", 1);
        Document doc = new DocumentImpl(uri, text, wordMap);

        dpm.serialize(uri, doc);

        assertTrue(dpm.delete(uri));

        File file = new File(baseDir, "/www.example.com/doc.json");
        assertFalse(file.exists());
    }

    @Test
    public void testFileCreation() throws Exception {
        URI uri = new URI("http://www.example.com/doc");
        String text = "This is a test document.";
        HashMap<String, Integer> wordMap = new HashMap<>();
        wordMap.put("This", 1);
        wordMap.put("is", 1);
        wordMap.put("a", 1);
        wordMap.put("test", 1);
        wordMap.put("document", 1);
        Document doc = new DocumentImpl(uri, text, wordMap);

        dpm.serialize(uri, doc);

        File file = new File(baseDir, "/www.example.com/doc.json");
        assertTrue(file.exists());

        String content = new String(Files.readAllBytes(Paths.get(file.getPath())));
        assertTrue(content.contains("This is a test document."));
    }

    @Test
    public void testBinaryDataSerialization() throws Exception {
        URI uri = new URI("http://www.example.com/docBinary");
        byte[] binaryData = {1, 2, 3, 4, 5};
        Document doc = new DocumentImpl(uri, binaryData);

        dpm.serialize(uri, doc);

        Document deserializedDoc = dpm.deserialize(uri);

        assertEquals(doc.getKey(), deserializedDoc.getKey());
        assertArrayEquals(doc.getDocumentBinaryData(), deserializedDoc.getDocumentBinaryData());
    }

    @Test
    public void testEmptyMetadataSerialization() throws Exception {
        URI uri = new URI("http://www.example.com/docEmptyMeta");
        String text = "This is a test document.";
        Document doc = new DocumentImpl(uri, text, new HashMap<>());

        dpm.serialize(uri, doc);

        Document deserializedDoc = dpm.deserialize(uri);

        assertEquals(doc.getKey(), deserializedDoc.getKey());
        assertEquals(doc.getDocumentTxt(), deserializedDoc.getDocumentTxt());
        assertTrue(deserializedDoc.getMetadata().isEmpty());
    }
}
