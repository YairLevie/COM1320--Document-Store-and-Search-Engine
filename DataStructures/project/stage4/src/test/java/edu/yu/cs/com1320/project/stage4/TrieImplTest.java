package edu.yu.cs.com1320.project.stage4;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import edu.yu.cs.com1320.project.impl.TrieImpl;

public class TrieImplTest {
TrieImpl<Integer> trie;

@BeforeEach
public void setUp() {
    this.trie = new TrieImpl<>();
}

@Test
public void testPutAndGet() {
    this.trie.put("Apple", 1);
    this.trie.put("Peanut", 2);
    this.trie.put("rebbe", 3);
    this.trie.put("Apple", 4);
    this.trie.put("Apple", 5);
    assertTrue(this.trie.get("Apple").contains(4));
    assertTrue(this.trie.get("Apple").contains(5));
    assertTrue(this.trie.get("Apple").contains(1));
    assertTrue(this.trie.get("Apple").size() == 3);
    assertTrue(this.trie.get("Peanut").size() == 1 && this.trie.get("Peanut").contains(2));
}
@Test
public void testGetSorted(){
    this.trie.put("Apple", 4);
    this.trie.put("Peanut", 2);
    this.trie.put("rebbe", 3);
    this.trie.put("Apple", 5);
    this.trie.put("Apple", 1);
    Comparator<Integer> comparator = new Comparator<Integer>() {
        @Override
        public int compare(Integer o1, Integer o2) {
            if (o1 > o2) {
                return 1;
            }
            if (o1 < o2) {
                return -1;
            }
            return 0;
        }
    };
    List<Integer> actual = this.trie.getSorted("Apple", comparator);
    List<Integer> expected = new ArrayList<>();
    expected.add(1);
    expected.add(4);
    expected.add(5);
    assertEquals(expected, actual);
}

@Test
public void testgetAllWithPrefixSorted() {
    this.trie.put("Apple", 4);
    this.trie.put("Peanut", 2);
    this.trie.put("rebbe", 3);
    this.trie.put("Appilation", 5);
    this.trie.put("App", 1);
    Comparator<Integer> comparator = new Comparator<Integer>() {
        @Override
        public int compare(Integer o1, Integer o2) {
            if (o1 > o2) {
                return 1;
            }
            if (o1 < o2) {
                return -1;
            }
            return 0;
        }
    };
    List<Integer> actual = this.trie.getAllWithPrefixSorted("App", comparator);
    List<Integer> expected = new ArrayList<>();
    expected.add(1);
    expected.add(4);
    expected.add(5);
    assertEquals(expected, actual);
}

@Test
public void testDeleteAll() {
    this.trie.put("App", 4);
    this.trie.put("Peanut", 2);
    this.trie.put("rebbe", 3);
    this.trie.put("App", 5);
    this.trie.put("App", 1);
    this.trie.put("Ap", 100);
    this.trie.put("Apple", 200);
    Set<Integer> actual = this.trie.deleteAll("App");
    Set<Integer> expected = new HashSet<>();
    expected.add(1);
    expected.add(4);
    expected.add(5);
    assertEquals(expected, actual);
    assertEquals(Collections.emptySet(), this.trie.get("App"));
    Set<Integer> test = new HashSet<>();
    test.add(100);
    assertEquals(test, this.trie.get("Ap"));
    Set<Integer> anotherTest = new HashSet<>();
    anotherTest.add(200);
    assertEquals(anotherTest, this.trie.get("Apple"));
}


@Test
public void testdeleteAllWithPrefix() {
    this.trie.put("Apple", 4);
    this.trie.put("Peanut", 2);
    this.trie.put("rebbe", 3);
    this.trie.put("Appilation", 5);
    this.trie.put("App", 1);
    this.trie.put("Ap", 100);
    Set<Integer> actual = this.trie.deleteAllWithPrefix("App");
    Set<Integer> expected = new HashSet<>();
    expected.add(1);
    expected.add(5);
    expected.add(4);
    assertEquals(expected, actual);
    Comparator<Integer> comparator = new Comparator<Integer>() {
        @Override
        public int compare(Integer o1, Integer o2) {
            if (o1 > o2) {
                return 1;
            }
            if (o1 < o2) {
                return -1;
            }
            return 0;
        }
    };
    Set<Integer> test = new HashSet<>();
    test.add(100);
    assertEquals(test, this.trie.get("Ap"));   
    assertEquals(Collections.emptyList(), this.trie.getSorted("App", comparator));
}

@Test
public void testdelete() {
    this.trie.put("Apple", 4);
    this.trie.put("Peanut", 2);
    this.trie.put("rebbe", 3);
    this.trie.put("Apple", 5);
    this.trie.put("Apple", 1);
    this.trie.put("Ap", 100);
    Integer deleted = this.trie.delete("Apple", 5);
    assertEquals(5, deleted);
    Comparator<Integer> comparator = new Comparator<Integer>() {
        @Override
        public int compare(Integer o1, Integer o2) {
            if (o1 > o2) {
                return 1;
            }
            if (o1 < o2) {
                return -1;
            }
            return 0;
        }
    };

    List<Integer> expected = new ArrayList<>();
    expected.add(1);
    expected.add(4);
    assertEquals(expected, this.trie.getSorted("Apple", comparator));
    assertNull(this.trie.delete("Apple", 5));
    List<Integer> expected2 = new ArrayList<>();
    expected2.add(1);
    expected2.add(4);
    expected2.add(100);
    assertEquals(expected2, this.trie.getAllWithPrefixSorted("Ap", comparator));
}
}

//TODO: how to check if ancestor is there when do a delete
//TODO: check strip punctuation