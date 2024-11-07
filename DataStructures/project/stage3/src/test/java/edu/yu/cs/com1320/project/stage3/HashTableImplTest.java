package edu.yu.cs.com1320.project.stage3;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import edu.yu.cs.com1320.project.HashTable;
import edu.yu.cs.com1320.project.impl.HashTableImpl;
import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class HashTableImplTest {
    HashTable<String,String> stringHashTable;
    HashTable<URI, String> uriHashTable; //lichora should be document and not string but wtvr
    URI uri1;
    URI uri2;
    URI uri3;
    URI uri4;
    URI uri5;
    URI uri6;
    URI uri7;

    @BeforeEach
    public void setup(){
        stringHashTable = new HashTableImpl<String,String>();
        uriHashTable = new HashTableImpl<URI,String>();
        uri1 = URI.create("http://TORAHISLIFE");
        uri2 = URI.create("http://Hellothere");
        uri3 = URI.create("http://Imbored");
        uri4 = URI.create("http://thisismindnumbing");
        uri5 = URI.create("http://isthisevenneccessary?");
        uri6 = URI.create("http://idontthinkso");
        uri7 = URI.create("http://owell");

    }
    @Test
    public void testPutUnique(){
        assertNull(stringHashTable.put("I love", "Mitzvos"));
        assertNull(stringHashTable.put("MyFavorite thing is", "Torah"));
        assertNull(stringHashTable.put("Torah>", "Everything"));
        assertNull(stringHashTable.put("Chazara", "IS THE REAL KEY"));
        assertNull(stringHashTable.put("I really love", "Rav Sobolofsky's shiur"));
        assertNull(stringHashTable.put("Family", "is also very important to me"));
        assertNull(stringHashTable.put("Last", "check"));
        

        assertEquals(stringHashTable.get("I love"), "Mitzvos");
        assertEquals(stringHashTable.get("MyFavorite thing is"), "Torah");
        assertEquals(stringHashTable.get("Torah>"), "Everything");
        assertEquals(stringHashTable.get("Chazara"), "IS THE REAL KEY");
        assertEquals(stringHashTable.get("I really love"), "Rav Sobolofsky's shiur");
        assertEquals(stringHashTable.get("Family"), "is also very important to me");
        assertEquals(stringHashTable.get("Last"), "check");
    }
    @Test
    public void testPutCopies(){
        assertNull(uriHashTable.put(uri1,"apple"));
        assertNull(uriHashTable.put(uri2,"orange"));
        assertNull(uriHashTable.put(uri3,"banana"));

        assertEquals("apple", uriHashTable.put(uri1,"peach"));
        assertEquals("orange", uriHashTable.put(uri2,"plum"));
        assertEquals("banana", uriHashTable.put(uri3,"starfruit"));

        assertEquals(uriHashTable.get(uri1),"peach");
        assertEquals(uriHashTable.get(uri2),"plum");
        assertEquals(uriHashTable.get(uri3),"starfruit");
    }
    @Test
    public void testDelete(){
        assertNull(stringHashTable.put("I love", "Mitzvos"));
        assertNull(stringHashTable.put("MyFavorite thing is", "Torah"));
        assertNull(stringHashTable.put("Torah>", "Everything"));
        assertNull(stringHashTable.put("Chazara", "IS THE REAL KEY"));
        assertNull(stringHashTable.put("I really love", "Rav Sobolofsky's shiur"));
        assertNull(stringHashTable.put("Family", "is also very important to me"));
        assertNull(stringHashTable.put("Last", "check"));
        assertNull(stringHashTable.put("more", "tests"));
        assertNull(stringHashTable.put("to", "make"));
        assertNull(stringHashTable.put("sure", "im"));
        assertNull(stringHashTable.put("not", "crazy"));

        stringHashTable.put("sure",null);
        assertNull(stringHashTable.get("sure"));
        stringHashTable.put("more", null);
        stringHashTable.put("Last", null);
        stringHashTable.put("sure", "im");
        stringHashTable.put("Family", null);
        stringHashTable.put("not", null);
        stringHashTable.put("Chazara", null);
        stringHashTable.put("I love", null);
        assertEquals("im", stringHashTable.get("sure"));
        assertNull(stringHashTable.get("more"));
        assertNull(stringHashTable.get("Last"));
        assertNull(stringHashTable.get("not"));
        assertNull(stringHashTable.get("Family"));
        assertNull(stringHashTable.get("Chazara"));
        assertNull(stringHashTable.get("I love"));
        assertEquals(5, stringHashTable.size());
        assertNull(stringHashTable.put("LOL why you tryna delete something that doenst exist", null));
}
    @Test
    public void testContainsKey(){
        assertNull(uriHashTable.put(uri1,"apple"));
        assertNull(uriHashTable.put(uri2,"orange"));
        assertNull(uriHashTable.put(uri3,"banana"));
        assertNull(uriHashTable.put(uri4,"starfruit"));
        assertNull(uriHashTable.put(uri5,"kangaroo"));
        assertNull(uriHashTable.put(uri6,"laptop"));

        assertTrue(uriHashTable.containsKey(uri1));
        assertTrue(uriHashTable.containsKey(uri6));
        assertFalse(uriHashTable.containsKey(uri7));
        assertEquals("laptop", uriHashTable.put(uri6,null));
        assertFalse(uriHashTable.containsKey(uri6));
    }
    @Test
    public void testKeySet(){
        assertNull(stringHashTable.put("I love", "Mitzvos"));
        assertNull(stringHashTable.put("MyFavorite thing is", "Torah"));
        assertNull(stringHashTable.put("Torah>", "Everything"));
        assertNull(stringHashTable.put("Chazara", "IS THE REAL KEY"));
        assertNull(stringHashTable.put("I really love", "Rav Sobolofsky's shiur"));
        assertNull(stringHashTable.put("Family", "is also very important to me"));
        assertNull(stringHashTable.put("Last", "check"));

        Set<String> keys = stringHashTable.keySet();
        assertTrue(keys.contains("Family"));
        assertTrue(keys.contains("MyFavorite thing is"));
        assertTrue(keys.contains("I love"));
        assertTrue(keys.contains("Torah>"));
        assertTrue(keys.contains("Chazara"));
        assertTrue(keys.contains("I really love"));
        assertTrue(keys.contains("Last"));
        assertFalse(keys.contains("Shalom Aleichem"));
        assertEquals(7, keys.size());
        
}
    @Test
    public void testGetAllValues(){
        assertNull(uriHashTable.put(uri1,"apple"));
        assertNull(uriHashTable.put(uri2,"orange"));
        assertNull(uriHashTable.put(uri3,"banana"));
        assertNull(uriHashTable.put(uri4,"starfruit"));
        assertNull(uriHashTable.put(uri5,"kangaroo"));
        assertNull(uriHashTable.put(uri6,"kangaroo"));
        
        Collection<String> values = uriHashTable.values();
        assertTrue(values.contains("apple"));
        assertTrue(values.contains("orange"));
        assertTrue(values.contains("banana"));
        assertTrue(values.contains("starfruit"));
        assertTrue(values.contains("kangaroo"));
        assertFalse(values.contains("Donkey"));
        boolean hasDuplicate = values.size() != values.stream().distinct().count();
        boolean isDuplicate = Collections.frequency(values, "kangaroo") > 1;
        assertTrue(hasDuplicate);
        assertTrue(isDuplicate);
        assertEquals(6, values.size());
    }
    @Test
    public void testGetSize(){
        //shouldve used for loop
        assertNull(stringHashTable.put("I love", "Mitzvos"));
        assertNull(stringHashTable.put("MyFavorite thing is", "Torah"));
        assertNull(stringHashTable.put("Torah>", "Everything"));
        assertNull(stringHashTable.put("Chazara", "IS THE REAL KEY"));
        assertEquals(4, stringHashTable.size());
        assertNull(stringHashTable.put("I really love", "Rav Sobolofsky's shiur"));
        assertNull(stringHashTable.put("Family", "is also very important to me"));
        assertNull(stringHashTable.put("Last", "check"));
        assertNull(stringHashTable.put("more", "tests"));
        assertNull(stringHashTable.put("to", "make"));
        assertNull(stringHashTable.put("sure", "im"));
        assertNull(stringHashTable.put("not", "crazy"));
        assertEquals(11, stringHashTable.size());
        assertEquals("check",stringHashTable.put("Last",null));
        assertEquals(10, stringHashTable.size());
        assertNull(stringHashTable.put("shalom",null));
        assertEquals(10, stringHashTable.size());
        
        stringHashTable.put("more",null);
        stringHashTable.put("MyFavorite thing is",null);
        stringHashTable.put("I love",null);
        stringHashTable.put("Torah>",null);
        stringHashTable.put("Chazara",null);
        stringHashTable.put("Family",null);
        stringHashTable.put("Family",null);
        stringHashTable.put("to",null);
        stringHashTable.put("sure",null);
        stringHashTable.put("not",null);
        assertEquals(1, stringHashTable.size());
        stringHashTable.put("I really love",null);
        assertEquals(0, stringHashTable.size());
        
    }
    @Test
    public void testArrayDoubling(){
        //shouldve used for loop
        //assertEquals(5, ((HashTableImpl<String, String>) stringHashTable).sizeOfHashTable());
        assertNull(stringHashTable.put("I love", "Mitzvos"));
        assertNull(stringHashTable.put("MyFavorite thing is", "Torah"));
        assertNull(stringHashTable.put("Torah>", "Everything"));
        assertNull(stringHashTable.put("Chazara", "IS THE REAL KEY"));
        assertNull(stringHashTable.put("I really love", "Rav Sobolofsky's shiur"));
        assertNull(stringHashTable.put("Family", "is also very important to me"));
        assertNull(stringHashTable.put("Last", "check"));
        assertNull(stringHashTable.put("more", "tests"));
        assertNull(stringHashTable.put("to", "make"));
        assertNull(stringHashTable.put("sure", "im"));
        assertNull(stringHashTable.put("I lovea", "Mitzvos"));
        assertNull(stringHashTable.put("MyFavoraite thing is", "Torah"));
        assertNull(stringHashTable.put("Torah>a", "Everything"));
        assertNull(stringHashTable.put("Chazaraa", "IS THE REAL KEY"));
        assertNull(stringHashTable.put("I reaally love", "Rav Sobolofsky's shiur"));
        assertNull(stringHashTable.put("Familay", "is also very important to me"));
        assertNull(stringHashTable.put("Lasta", "check"));
        assertNull(stringHashTable.put("morea", "tests"));
        assertNull(stringHashTable.put("toa", "make"));
        assertNull(stringHashTable.put("suare", "im"));

       // assertEquals(5, ((HashTableImpl<String, String>) stringHashTable).sizeOfHashTable());
        assertNull(stringHashTable.put("suasre", "im"));
        //assertEquals(10, ((HashTableImpl<String, String>) stringHashTable).sizeOfHashTable());
        assertEquals("make",stringHashTable.get("to"));
        assertEquals("im",stringHashTable.get("suare"));
    }
}
