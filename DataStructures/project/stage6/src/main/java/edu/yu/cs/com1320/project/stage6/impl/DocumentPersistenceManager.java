package edu.yu.cs.com1320.project.stage6.impl;
import java.io.File;
import com.google.gson.*;
import edu.yu.cs.com1320.project.stage6.Document;
import edu.yu.cs.com1320.project.stage6.PersistenceManager;

import java.io.*;
import java.lang.reflect.Type;
import java.net.*;
import java.util.*;

public class DocumentPersistenceManager implements PersistenceManager<URI, Document> {
    File baseDir;

    public DocumentPersistenceManager(File baseDir){
        if(baseDir == null){
            this.baseDir = new File(System.getProperty("user.dir"));
        }
        else{
            this.baseDir = baseDir;
        }
    }

    @Override
    public void serialize(URI key, Document val) throws IOException {
        if (key == null) {
            throw new IllegalArgumentException();
        }
        Gson gson = new Gson();
        JsonSerializer<Document> serializer = new JsonSerializer<Document>()
        {
            @Override
            public JsonElement serialize(Document doc, Type type, JsonSerializationContext context)
            {
                JsonObject jsonDocument = new JsonObject();
                jsonDocument.addProperty("uri", doc.getKey().toString());
                if(!doc.getMetadata().isEmpty()){
                    jsonDocument.add("metaData", gson.toJsonTree(doc.getMetadata()));
                }
                if (doc.getDocumentTxt() != null){
                    jsonDocument.addProperty("text", doc.getDocumentTxt());
                    jsonDocument.add("wordMap", gson.toJsonTree(doc.getWordMap()));
                }
                else{
                    String bytes = Base64.getEncoder().encodeToString(doc.getDocumentBinaryData());
                    jsonDocument.addProperty("bytes", bytes);
                }
                return jsonDocument;
            }
        };
        JsonElement jsonElement = serializer.serialize(val, null, null);
        String jsonDoc = gson.toJson(jsonElement);

        String pathForURI = "";
        if (key.getScheme() != null) {
            pathForURI += "/" + key.getHost() + key.getPath();
        }
        else {
            pathForURI += key.toString();
        }
        File pathForFile = new File(this.baseDir, pathForURI + ".json");
        pathForFile.getParentFile().mkdirs();

        try (FileWriter fileWriter = new FileWriter(pathForFile)) {
            fileWriter.write(jsonDoc);
        }
    }

    @Override
    public Document deserialize(URI key) throws IOException {
        if (key == null) {
            throw new IllegalArgumentException();
        } 
        Gson gson = new Gson();
            JsonDeserializer<Document> deserializer = new JsonDeserializer<Document>(){
                @Override
                public Document deserialize(JsonElement element, Type type, JsonDeserializationContext context) throws JsonParseException
                {
                    JsonObject jsonObject = element.getAsJsonObject();
                    URI uri = URI.create(jsonObject.get("uri").getAsString());
                    String text = null;
                    byte[] binaryData = null;
                    DocumentImpl newDoc;
                    if (jsonObject.has("bytes")) {
                        binaryData = Base64.getDecoder().decode(jsonObject.get("bytes").getAsString());
                        newDoc = new DocumentImpl(uri, binaryData);
                    } else {
                        text = jsonObject.get("text").getAsString();
                        JsonObject mapObject = jsonObject.get("wordMap").getAsJsonObject();
                        HashMap <String, Integer> wordMap = new HashMap <> ();
                        for (Map.Entry <String, JsonElement> entry: mapObject.entrySet()) {
                            String key = entry.getKey();
                            Integer value = entry.getValue().getAsInt();
                            wordMap.put(key, value);
                        }
                        newDoc = new DocumentImpl(uri, text, wordMap);
                    }
                    if (jsonObject.has("metaData")){
                        JsonObject mapObject = jsonObject.get("metaData").getAsJsonObject();
                        HashMap <String, String> metaData = new HashMap <> ();
                        for (Map.Entry <String, JsonElement> entry: mapObject.entrySet()) {
                            String key = entry.getKey();
                            String value = entry.getValue().toString();
                            metaData.put(key, value);
                        }
                        newDoc.setMetadata(metaData);
                    }
                    return newDoc;
                }
            };
            String pathForURI = "";
            if (key.getScheme() != null) {
                pathForURI += "/" + key.getHost() + key.getPath();
            }
            else {
                pathForURI += key.toString();
            }
            File pathForFile = new File(this.baseDir, pathForURI + ".json");
            try (FileReader reader = new FileReader(pathForFile)) {
                JsonElement jsonElement = gson.fromJson(reader, JsonElement.class);
                return deserializer.deserialize(jsonElement, null, null);
            } 
        }

    @Override
    public boolean delete(URI key) throws IOException {
        String pathForURI = "";
        if (key.getScheme() != null) {
            pathForURI += "/" + key.getHost() + key.getPath();
        }
        else {
            pathForURI += key.toString();
        }
        File file = new File(this.baseDir, pathForURI + ".json");
        return file.delete();
    }
}

//when do delete?
