package edu.yu.cs.com1320.project.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections; 
import java.util.HashSet; 
import java.util.List;
import java.util.Set;

import edu.yu.cs.com1320.project.HashTable;

public class HashTableImpl<Key, Value> implements HashTable<Key,Value> {
    
    private class Node<K, V> {
        Node<K, V> next;
        K key;
        V value;
    
        private Node(K key, V value) {
            this.key = key;
            this.value = value;
            this.next = null;
        }
        private Node(K key){
            this.key=key;
            this.value = null;
            this.next=null;
        }
        private boolean equalsNode(Object o){
            if (o == null){
                return false;
            }
            else if (this==o){
                return true;
            }
            else if (o.getClass() == this.getClass()){
                Node<K,V> node = (Node<K,V>) o;
                if (node.key == this.key){
                    return true;
                }
                else{
                    return false;
                }
            }
            return false;
        }
    }
    private class LinkedList<K,V>{
        Node<K,V> head;    
        private void addToList(K key, V value){
            Node<K,V> node = new Node<>(key,value);
            //check if it is the first variable added to list. if so, connect it to the head
            if (head == null){
                head = node;
            }
            //otherwise, find the last variable in the list and add it to the end
            else{
                Node<K,V> check = head;
                while(check.next!=null){
                    check = check.next;
                }
                check.next = node; 
            }
        }
    }
    
    LinkedList<Key,Value> [] hashTable;
    int numberOfKeys;
    public HashTableImpl(){
        this.hashTable = new LinkedList[5]; 
        for (int i =0; i<5;i++){
            hashTable[i]=new LinkedList<>();
        }
    }

    @Override
    public Value put(Key k, Value v){
        int index = hashFunction(k);
        if (v == null){
            return deleteEntry(k);
        }
        LinkedList<Key,Value> list = (LinkedList<Key,Value>) hashTable[index];
        if (this.containsKey(k)){
            Value oldValue = this.get(k);
            this.findNode(k).value = v;
            return oldValue;
        }
        else{    //some kinda increment?
            numberOfKeys++;
            list.addToList(k,v);
            return null;
        }

    }

    private Value deleteEntry(Key k){
        if (k == null){
            return null;
        }
        else if (!this.containsKey(k)){
            return null;
        }
        else{
            int index = hashFunction(k);
            LinkedList<Key,Value> list = (LinkedList<Key,Value>) hashTable[index];
            Node<Key,Value> temp = new Node<Key,Value>(k);
            Node<Key,Value> iteratingNode = list.head;
            if (iteratingNode.equalsNode(temp)){ 
                list.head = list.head.next;
                numberOfKeys--;
                return iteratingNode.value;
            } 
            else{
                while(iteratingNode.next != null){
                    if (iteratingNode.next.equalsNode(temp)){
                        Value oldValue = iteratingNode.next.value;
                        numberOfKeys--;
                        iteratingNode.next = iteratingNode.next.next; 
                        return oldValue;
                    }
                    iteratingNode = iteratingNode.next;
                }
            }
            numberOfKeys--;
            return iteratingNode.value;
        }}

    @Override
    public Value get(Key k){
        if (this.containsKey(k) == false){
            return null;
        }
        else{
            return this.findNode(k).value;
        }
    }

    @Override
    public boolean containsKey(Key key){
        if (key == null){
            throw new NullPointerException();
        }
        else if (this.findNode(key) == null){
            return false;
        }
        return true;
    }

    private Node<Key,Value> findNode(Key key){
        int index = hashFunction(key);
        //false if never initialized
        if (hashTable[index] == null) {
            return null;
        }
        LinkedList<Key,Value> list = (LinkedList<Key,Value>) hashTable[index];
        Node<Key,Value> temp = new Node<Key,Value>(key);
        Node<Key,Value> iteratingNode = list.head;
        //false if head is null
        if (iteratingNode == null){
            return null;
        }
        else{
            //iterate through list as long as its its not the last Node
            while(iteratingNode.next != null){
                //true if the two nodes = each other
                if (iteratingNode.equalsNode(temp)){
                    return iteratingNode;
                }
                else{
                    //move the Node over to the next node on the list
                    iteratingNode = iteratingNode.next;
                }
            }
            //checks the last Node in the list
            if (iteratingNode.equalsNode(temp)){
                return iteratingNode;
            }
            return null;
        }}

    public Set<Key> keySet(){
        Set<Key> keys = new HashSet<Key>(); 
        for (int i = 0; i<5;i++){
            if (hashTable[i] != null) {
                LinkedList<Key,Value> list = (LinkedList<Key,Value>) hashTable[i];
                Node<Key,Value> iteratingNode = list.head;
                if (iteratingNode != null){
                    while(iteratingNode.next != null){
                        keys.add(iteratingNode.key);
                        iteratingNode = iteratingNode.next;
                    }
                    keys.add(iteratingNode.key);
                }
            }
        }
        return Collections.unmodifiableSet(keys);
    }

    public Collection<Value> values(){
        List<Value> values = new ArrayList<Value>(); 
        for (int i = 0; i<5;i++){
            if (hashTable[i] != null) {
                LinkedList<Key,Value> list = (LinkedList<Key,Value>) hashTable[i];
                Node<Key,Value> iteratingNode = list.head;
                if (iteratingNode != null){
                    while(iteratingNode.next != null){
                        values.add(iteratingNode.value);
                        iteratingNode = iteratingNode.next;
                    }
                    values.add(iteratingNode.value);
                }
            }
        }
        return Collections.unmodifiableCollection(values);
    }

    public int size(){
        return numberOfKeys;
    }

    private int hashFunction(Key key) {
        if (key != null) {
            return (key.hashCode() & 0x7fffffff) % this.hashTable.length;
        }
        throw new IllegalArgumentException();
    }
}