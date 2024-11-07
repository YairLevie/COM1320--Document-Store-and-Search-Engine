# Document Store and Search Engine

This project, a core component of my Data Structures course, is a **fully functional document store and search engine** built in Java. It leverages advanced data structures and memory management techniques to store, retrieve, and manage documents. Each development stage introduced a new level of functionality and efficiency, culminating in a system that supports in-memory and disk-based storage.

## 🚀 Features

1. **Efficient Document Storage**: Implemented as a hierarchical data store that uses hash tables, Tries, heaps, and B-trees for highly efficient document management.
2. **Search Functionality**: Supports keyword and prefix-based search across documents using a Trie data structure, with results ranked by keyword frequency.
3. **Undo/Redo Support**: Enables undoing of the last operation or a specific operation on a document, using a custom command stack with lambdas for functional programming.
4. **Memory Management**: Uses a Least Recently Used (LRU) eviction policy based on a min-heap and supports both document and byte-size limits.
5. **Two-Tier Storage**: Manages data between RAM and disk storage, automatically persisting documents to disk when memory limits are reached and retrieving them when needed.

## 🛠️ Technical Overview

### Stages of Development

1. **Stage 1** - **In-Memory Document Store**  
   Implemented a `DocumentStore` class to manage plain text and binary documents. Documents are stored in a `HashMap`, identified by a unique URI. Custom metadata for each document is stored in a `HashMap` within each document.

2. **Stage 2** - **HashTable Implementation**  
   Built a custom hash table with separate chaining for collision resolution and fixed-size internal storage. This replaced Java’s `HashMap` in the `DocumentStore`, enhancing modular design.

3. **Stage 3** - **Undo Functionality**  
   Implemented a stack-based command pattern to support undo and redo operations. Lambda functions for undo actions enabled functional programming for efficiency and clarity.

4. **Stage 4** - **Search with Trie Data Structure**  
   Integrated a `Trie` to support keyword and prefix-based search with case-sensitive functionality. Documents with the highest occurrence of a search term are prioritized in search results.

5. **Stage 5** - **Min-Heap for Memory Management**  
   Introduced a min-heap to track document usage time, enforcing document and byte-size limits in memory. When limits are exceeded, the least recently used document is evicted, freeing memory for new documents.

6. **Stage 6** - **Two-Tier Storage with B-Tree and Disk Persistence**  
   Replaced the `HashTable` with a `BTree`, enabling two-tier storage across RAM and disk. Document serialization/deserialization is handled by `DocumentPersistenceManager`, with documents moved to disk once memory limits are reached.

### Key Data Structures, Algorithms, and Optimizations

- **HashTable**: Custom-built with separate chaining for collision handling.
- **Trie**: Used for keyword and prefix search across documents, providing O(m) search complexity (where m is the length of the search keyword).
- **Min-Heap**: Tracks document access times for enforcing memory limits, ensuring O(log n) complexity for insertion and re-heapify operations.
- **B-Tree**: Organizes document storage between RAM and disk, optimizing retrieval based on usage frequency.
- **Serialization**: Documents are serialized to JSON format for disk storage, using Google’s GSON library for efficiency.
- **Efficient Data Retrieval:** Trie-based keyword searches ensure quick lookups, even across large datasets.
- **Memory-Efficient Design:** Dynamic min-heap and disk storage balance memory constraints with rapid data access.
- **Functional Programming:** Leveraged Java lambdas for undo commands, providing concise, effective state management.

## 📂 Repository Structure and Usage

```plaintext
DataStructures/
├── project/
│   ├── stage1/
│   ├── stage2/
│   ├── stage3/
│   ├── stage4/
│   ├── stage5/
│   └── stage6/

Each folder corresponds to a stage with its own Maven project, ensuring modularity and ease of testing.

🚀 Usage
Clone the repository and use Maven to build each project stage independently:

# Clone the repository
git clone https://github.com/yairlevie/COM1320-Document-Store-and-Search-Engine.git
cd COM1320-Document-Store-and-Search-Engine/DataStructures/project/stageX

# Build and run tests
mvn clean install
mvn test
