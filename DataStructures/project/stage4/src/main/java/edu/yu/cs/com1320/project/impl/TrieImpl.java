package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.Trie;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;



public class TrieImpl<Value> implements Trie<Value> {
         
    private Node<Value> root; 

    public TrieImpl (){
        this.root = new Node<Value>();
    }

    private static class Node<Value>
    {
        Set<Value> val;
        Node<Value>[] links;
        
        public Node(){
            links = new Node[256];
            val = new HashSet<Value>();
        }
    }
    
    public void put(String key, Value val){ 
        if (key == null){
            throw new IllegalArgumentException();
        }
        if (val == null)
        {
            return;  
        }
        else
        {
            this.root = put(this.root, key, val, 0); //TODO: make sure im updating the root correctly
        }
    }
    
    private Node<Value> put(Node<Value> x, String key, Value val, int d)
    {
        if (x == null)
        {
            x = new Node<Value>();
        }
        if (d == key.length())
        {
            x.val.add(val);
            return x;
        }
        char c = key.charAt(d);
        x.links[c] = this.put(x.links[c], key, val, d + 1);
        return x;
    }

    public List<Value> getSorted(String key, Comparator<Value> comparator){ 
        if (key == null|| comparator == null){
            throw new IllegalArgumentException();
        }
        Set<Value> matchingValues = this.get(key);
        List<Value> sortedValues = new ArrayList<Value>(matchingValues);
        sortedValues.sort(comparator);
        return (List<Value>)sortedValues;
    }

    public Set<Value> get(String key){
        if (key == null){
            throw new IllegalArgumentException();
        }
        Node<Value> node = this.get(this.root, key, 0);
        Set<Value> emptySet = new HashSet<>();
        if (node == null){
            return emptySet;    // could use Collections.emptySet(), but not sure if im allowed to import Collections.
        }
        return (Set<Value>) node.val;
    }

    private Node<Value> get(Node<Value> x, String key, int d)
    {
        if (key == null){
            throw new IllegalArgumentException();
        }
        if (x == null)
        {
            return null;
        }
        if (d == key.length())
        {
            return x;
        }
        char c = key.charAt(d);
        return this.get(x.links[c], key, d + 1);
    }

    public List<Value> getAllWithPrefixSorted(String prefix, Comparator<Value> comparator){
        if (prefix == null|| comparator == null){
            throw new IllegalArgumentException();
        }
        Set<Value> setValues = new HashSet<>();
        getAllValues(get(this.root,prefix,0), prefix, setValues);
        List<Value> listValues = new ArrayList<>(setValues);
        listValues.sort(comparator);
        return listValues;
    }

    public Set<Value> deleteAllWithPrefix(String prefix){
        if (prefix == null){
            throw new IllegalArgumentException();
        }
        Set<Value> deletedVals = new HashSet<>();
        deleteAllWithPrefix(get(this.root, prefix, 0), prefix, deletedVals);
        return deletedVals; 
    }

    private Node<Value> deleteAllWithPrefix(Node<Value> x, String prefix, Set<Value> set) {
        if (prefix == null){
            throw new IllegalArgumentException();
        }
        if (x == null) {
            return x;
        }
        if (x.val != null) {
            set.addAll(x.val);
            x.val.clear();
        }
        for (char c = 0; c < x.links.length; c++) {
            x.links[c] = deleteAllWithPrefix(x.links[c], prefix + c, set);
        }
        if (x.val != null) { //TODO: is this right? should i delete the last one also?
            return x;
        }
        //remove subtrie rooted at x if it is completely empty
        for (int c = 0; c < x.links.length; c++) {
            if (x.links[c] != null) {
                return x; //not empty
            }
        }
        //empty - set this link to null in the parent
        return null;
    }


    public Set<Value> deleteAll(String key){
        if (key == null){
            throw new IllegalArgumentException();
        }
        Set<Value> deletedVals = new HashSet<>();
        this.root = deleteAll(this.root, key, 0, deletedVals);
        return deletedVals;
    }

    private Node<Value> deleteAll(Node<Value> node, String key, int d, Set<Value> deletedVals){
        if (key == null){
            throw new IllegalArgumentException();
        }
        if (node == null) {
            return null;
        }
        if (d == key.length()) {
            deletedVals.addAll(node.val);
            node.val.clear();
        }
        else {
            char c = key.charAt(d);
            node.links[c] = this.deleteAll(node.links[c], key, d + 1, deletedVals);
        }
        if (node.val != null) {
            return node;
        }
        for (int c = 0; c < node.links.length; c++) {
            if (node.links[c] != null) {
                return node; //not empty
            }
        }
        return null;
    }

    public Value delete(String key, Value val){
        if (key == null){
            throw new IllegalArgumentException();
        }
        Node<Value> node = this.get(this.root,key,0);
        ArrayList<Value> listOfValues = new ArrayList<>(node.val);
        if (node != null && listOfValues.contains(val)){
            this.delete(this.root, key, 0, val);
            return val;
        }
        return null;
    }

    private Node<Value> delete(Node<Value> x, String key, int d, Value val){
        if (key == null){
            throw new IllegalArgumentException();
        }
        if (x == null)
        {
            return null;
        }
        if (d == key.length())
        {
            x.val.remove(val);
        }
        else
        {
            char c = key.charAt(d);
            x.links[c] = this.delete(x.links[c], key, d + 1, val);
        }
        if (!x.val.isEmpty())
        {
            return x;
        }
        for (int c = 0; c <x.links.length; c++)
        {
            if (x.links[c] != null)
            {
                return x; 
            }
        }
        return null;
    }

    private void getAllValues(Node<Value> node, String prefix, Set<Value> values){
        if (prefix == null){
            throw new IllegalArgumentException();
        }
        if (node == null){
            return;
        }
        if (node != null && node.val != null){
            values.addAll(node.val);
        }
        for (char c = 0; c < node.links.length; c++) {
            getAllValues(node.links[c], prefix+c, values);
        }
    }
}

//TODO: what about if they do get and then punctuation? that will be outOFBOUNDS!! fix?
//TODO: add more comments


//NOTE: lots of code from tooSimpleTrie.... see comments there


