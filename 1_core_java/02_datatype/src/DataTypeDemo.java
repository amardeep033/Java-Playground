import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DataTypeDemo {

    //primitive datatype are not objects - stored directly
    //byte, short, int, long, float, double, char, boolean

    //non-primitive datatype are objects - String, Array, Wrapper, Collection, Map

    //Java collection always store objects - not primitives
    //List<Integer> possible, List<int> not possible
    //Wrapper - Integer, Long, Float, Double, Character, Boolean, etc.

    public static void main(String[] args) {
        primitiveAndWrapperDemo();
        referenceTypeDemo();
        stringAndArrayDemo();
        iterableDemo();
        collectionDemo();
        listDemo();
        queueDemo();
        setDemo();
        mapDemo();
    }

    private static void primitiveAndWrapperDemo() {
        System.out.println("---------------- PRIMITIVE AND WRAPPER ----------------");

        int primitiveCount = 10;
        Integer boxedCount = primitiveCount; //autoboxing - primitive int converted into Integer object
        int unboxedCount = boxedCount; //unboxing - Integer object converted back into primitive int

        Integer cachedOne = Integer.valueOf(100); //❓valueOf can reuse cached wrapper object
        Integer cachedTwo = Integer.valueOf(100); //❓wrapper cache commonly covers -128 to 127

        System.out.println("Primitive int: " + primitiveCount);
        System.out.println("Wrapper Integer: " + boxedCount);
        System.out.println("Unboxed int: " + unboxedCount);
        System.out.println("Integer.valueOf(100) == Integer.valueOf(100): " + (cachedOne == cachedTwo));
    }

    private static void referenceTypeDemo() {
        System.out.println("\n---------------- REFERENCE TYPE AND OBJECT ----------------");

        Object stringObject = "hello"; //💧0. String object can be assigned to Object reference -- Uses string pool //Every class automatically extends Object(not object) unless you specify another superclass.
        Object arrayObject = new int[5]; //❓array is also object in Java

        System.out.println("Object holding String: " + stringObject);
        System.out.println("Object holding int[] class: " + arrayObject.getClass().getSimpleName());
    }

    private static void stringAndArrayDemo() {
        System.out.println("\n---------------- STRING AND ARRAY ----------------");

        String pooled = "java"; //💧1. immutable, shared, stored once in string pool, == checks same reference
        String heapString = new String("java"); //💧2. immutable, separate object on heap, .equals checks value --- creates NEW object on change instead of updating same -- even though on heap
        CharSequence sequence = pooled; //ℹ️CharSequence interface can refer to String, StringBuilder, StringBuffer
        StringBuilder builder = new StringBuilder(); //💧3. mutable, not synchronized, good for changing string in single thread -- mutable because buffer on heap -- even full address changes and not object
        builder.append("Java").append(" Collections");
        StringBuffer buffer = new StringBuffer(); //💧4. mutable, synchronized, use when 🧵thread-safe mutable string is required -- acquire lock -- slow
        buffer.append("Thread-safe mutable string");
        StringTokenizer tokenizer = new StringTokenizer("java,string,tokenizer", ","); //⚠️legacy tokenizer - prefer split() or Scanner in new code

        int[] scores = {90, 80, 70}; //⭐array: fixed size, can store primitives directly
        Object[] names = new String[2]; //❓array covariance: Object[] reference can point to String[] object

        System.out.println("pooled == heapString: " + (pooled == heapString));
        System.out.println("pooled.equals(heapString): " + pooled.equals(heapString));
        System.out.println("CharSequence value: " + sequence);
        System.out.println("StringBuilder value: " + builder);
        System.out.println("StringBuffer value: " + buffer);
        System.out.println("StringTokenizer first token: " + tokenizer.nextToken());
        System.out.println("First array element: " + scores[0]);

        try {
            names[0] = 10; //compiles, but runtime checks actual array type and throws ArrayStoreException
        } catch (ArrayStoreException exception) {
            System.out.println("Array covariance runtime check: " + exception.getClass().getSimpleName());
        }
    }

    private static void iterableDemo() {
        System.out.println("\n---------------- ITERABLE ----------------");

        Iterable<String> iterable = List.of("List", "Queue", "Set"); //ℹ️Iterable is root interface of Collection hierarchy
        Iterator<String> iterator = iterable.iterator(); //Iterator used to read elements one by one

        while (iterator.hasNext()) {
            System.out.println(iterator.next());
        }
    }

    private static void collectionDemo() {
        System.out.println("\n---------------- COLLECTION ----------------");

        Collection<String> collection = new ArrayList<>(); //ℹ️Collection is interface, ArrayList is implementation class
        collection.add("List");
        collection.add("Queue");
        collection.add("Set");

        List<String> sortable = new ArrayList<>(); //Collections utility class has static helper methods
        Collections.addAll(sortable, "Vector", "ArrayList", "LinkedList");
        Collections.sort(sortable);

        System.out.println("Collection size: " + collection.size());
        System.out.println("Collection values: " + collection);
        System.out.println("Collections.sort result: " + sortable);
    }

    private static void listDemo() {
        System.out.println("\n---------------- LIST ----------------");

        //ℹ️List - interface, ordered, allows duplicate values, index based

        List<String> arrayList = new ArrayList<>(); //⭐Vector:: NOT synchronized, growable array, fast search by index, modern, preferred
        arrayList.add("ArrayList: fast indexed access");

        LinkedList<String> linkedList = new LinkedList<>(); //🌟doubly linked list, can work as List and Deque, faster add/remove in middle
        linkedList.add("LinkedList: list + deque operations");

        Vector<String> vector = new Vector<>(); //⚠️synchronized(🧵thread-safe), growable array, legacy
        vector.add("Vector: synchronized legacy list");

        Stack<String> stack = new Stack<>(); //⚠️child of Vector, LIFO(last in first out), legacy - ArrayDeque preferred
        stack.push("Stack: legacy LIFO class");

        System.out.println(arrayList);
        System.out.println(linkedList);
        System.out.println(vector);
        System.out.println(stack.pop());
    }

    // | C++ STL                            | Java (DSA)                             |
    // | ---------------------------------- | -------------------------------------- |
    // | `vector`                           | `ArrayList`                            |
    // | `stack`                            | `ArrayDeque`                           |
    // | `queue`                            | `ArrayDeque`                           |
    // | `deque`                            | `ArrayDeque`                           |
    // | `priority_queue`                   | `PriorityQueue`                        |
    // | `unordered_set`                    | `HashSet`                              |
    // | `set`                              | `TreeSet`                              |
    // | `unordered_map`                    | `HashMap`                              |
    // | `map`                              | `TreeMap`                              |
    // | `string`                           | `String`                               |
    // | **mutable string** (`std::string`) | `StringBuilder`                        |



    private static void queueDemo() {
        System.out.println("\n---------------- QUEUE ----------------");

        //ℹ️Queue - interface, generally FIFO(first in first out)

        Queue<String> priorityQueue = new PriorityQueue<>(); //⭐⭐⭐special_queue:: orders elements by priority/natural sorting, not normal insertion order
        priorityQueue.add("B");
        priorityQueue.add("A");
        priorityQueue.add("C");

        Deque<String> arrayDeque = new ArrayDeque<>(); //⭐Stack,Queue,Dequeu: Deque means double ended queue, add/remove from both sides
        arrayDeque.addFirst("front");
        arrayDeque.addLast("back");

        LinkedList<String> linkedListAsDeque = new LinkedList<>(); //🌟LinkedList implements List and Deque both
        linkedListAsDeque.addFirst("LinkedList can work as Deque");

        ConcurrentLinkedQueue<String> concurrentQueue = new ConcurrentLinkedQueue<>(); //⭐⭐⭐⭐🧵thread-safe non-blocking queue
        concurrentQueue.add("message");

        System.out.println("PriorityQueue removes first: " + priorityQueue.poll());
        System.out.println("ArrayDeque first: " + arrayDeque.removeFirst());
        System.out.println("ArrayDeque last: " + arrayDeque.removeLast());
        System.out.println(linkedListAsDeque.removeFirst());
        System.out.println("ConcurrentLinkedQueue values: " + concurrentQueue);
    }

    private static void setDemo() {
        System.out.println("\n---------------- SET ----------------");

        //Set - interface, does not allow duplicate values

        Set<String> hashSet = new HashSet<>(); //⭐unordered_set:: no duplicate, no insertion order guarantee, based on hash table
        hashSet.add("Java");
        hashSet.add("Java");
        hashSet.add("Python");

        Set<String> linkedHashSet = new LinkedHashSet<>(); //⭐⭐set:: no duplicate, maintains insertion order
        linkedHashSet.add("first");
        linkedHashSet.add("second");

        NavigableSet<Integer> treeSet = new TreeSet<>(); //⭐⭐⭐special_set:: NavigableSet extends SortedSet, TreeSet stores unique values in sorted order
        treeSet.add(30);
        treeSet.add(10);
        treeSet.add(20);

        System.out.println("HashSet removes duplicates: " + hashSet);
        System.out.println("LinkedHashSet keeps insertion order: " + linkedHashSet);
        System.out.println("TreeSet keeps sorted order: " + treeSet);
    }

    private static void mapDemo() {
        System.out.println("\n---------------- MAP ----------------");

        //ℹ️Map - interface, key-value pair, key must be unique, not child of Collection

        Map<String, Integer> hashMap = new HashMap<>(); //⭐unordered_map:: no duplicate key, no insertion order guarantee, one null key allowed
        hashMap.put("login", 3);
        hashMap.put("logout", 1);

        Map<String, Integer> linkedHashMap = new LinkedHashMap<>(); //⭐⭐map:: keeps insertion order
        linkedHashMap.put("first", 1);
        linkedHashMap.put("second", 2);

        Map<String, Integer> hashtable = new Hashtable<>(); //⚠️ legacy synchronized map, generally replaced by ConcurrentHashMap
        hashtable.put("legacy", 1);

        Map<String, Integer> concurrentHashMap = new ConcurrentHashMap<>(); //⭐⭐⭐⭐🧵thread-safe map, null keys/values not allowed
        concurrentHashMap.put("safe", 1);

        NavigableMap<String, Integer> treeMap = new TreeMap<>(); //⭐⭐⭐special_map:: NavigableMap extends SortedMap, TreeMap sorts entries by key
        treeMap.put("B", 20);
        treeMap.put("A", 10);
        treeMap.put("C", 30);

        System.out.println("HashMap values: " + hashMap);
        System.out.println("LinkedHashMap insertion order: " + linkedHashMap);
        System.out.println("Hashtable values: " + hashtable);
        System.out.println("ConcurrentHashMap values: " + concurrentHashMap);
        System.out.println("TreeMap sorted by key: " + treeMap);
        System.out.println("TreeMap lower key than C: " + treeMap.lowerKey("C"));
    }
}
