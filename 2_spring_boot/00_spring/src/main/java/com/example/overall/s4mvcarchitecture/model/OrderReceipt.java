package com.example.overall.s4mvcarchitecture.model;

// This is a shared application model because the example is intentionally small. The repository creates it, the service returns it, and the controller reads it.
public record OrderReceipt(int orderId, String itemName) {
}

// This is a class, specifically a special kind of class called a record, introduced as a preview in Java 14 and made permanent in Java 16.
// Conceptually, this expands to:

// public final class OrderReceipt {
//     private final int orderId;
//     private final String itemName;

//     public OrderReceipt(int orderId, String itemName) {   // canonical constructor
//         this.orderId = orderId;
//         this.itemName = itemName;
//     }

//     public int orderId() { return orderId; }              // accessor, not getOrderId()
//     public String itemName() { return itemName; }          // accessor

//     @Override
//     public boolean equals(Object o) { ... }               // generated; compares all record components
//     @Override
//     public int hashCode() { ... }                         // generated from all record components
//     @Override
//     public String toString() { ... }                      // generated: "OrderReceipt[orderId=1, itemName=Pen]"
// }
