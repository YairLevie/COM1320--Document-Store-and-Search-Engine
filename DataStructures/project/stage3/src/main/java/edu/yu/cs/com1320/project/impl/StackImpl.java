package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.Stack;

public class StackImpl<T> implements Stack<T>{
    
    private class Node<T> {
        private T data;
        private Node<T> next;
    
        private Node(T data) {
            this.data = data;
            this.next=null;
        }
    }

    //instance variables:
    Node<T> head;
    int size;
    public StackImpl(){
        this.head = null;
        this.size = 0;
    }
    
    @Override
    public void push(T element){
        Node<T> newNode = new Node<T>(element);
        if (this.head == null){
            this.head = newNode;
        }
        else{
            newNode.next = this.head; // Fix THIS!
            this.head = newNode;
        }
        this.size++;
    }

    @Override
    public T pop(){
        if (head == null){
            return null;
        }
        else{
            T data = head.data;
            this.head = this.head.next;
            this.size--;
            return data;
        }

    }

    public T peek(){
        if (this.head ==null){
            return null;
        }
        return this.head.data;
    }

    @Override
    public int size(){
        return this.size;
    }
}
