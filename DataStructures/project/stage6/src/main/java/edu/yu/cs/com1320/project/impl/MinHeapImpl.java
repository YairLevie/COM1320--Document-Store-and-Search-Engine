package edu.yu.cs.com1320.project.impl;

import java.util.NoSuchElementException;

import edu.yu.cs.com1320.project.MinHeap;

public class MinHeapImpl <E extends Comparable<E>> extends MinHeap<E> {
    
    public MinHeapImpl(){
        elements = (E[]) new  Comparable [100];
    }
    public void reHeapify (E element){
        try
        {
            int elementIndex = this.getArrayIndex(element);
            this.upHeap(elementIndex);
            this.downHeap(elementIndex);
        }
        catch (NoSuchElementException e)
        {
           return;
        }
    }

    public int getArrayIndex(E element){
        if (element == null){
            throw new IllegalArgumentException();
        }
        for (int i =1; i<elements.length; i++){
            if (elements[i] == null){
                throw new NoSuchElementException();
            }
            if (elements[i].equals(element)){
                return i;
            }
        }
        throw new NoSuchElementException();
    }

    public void doubleArraySize(){
        E[] doubledArray = (E[]) new  Comparable [elements.length *2];
        for (int i = 1; i<elements.length;i++){
            doubledArray[i] = elements[i];
        }
        this.elements = doubledArray;
    }
}
