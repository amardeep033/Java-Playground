// Functional interface: an interface with exactly one abstract method.
// Anonymous class: the pre-Java 8 way to create an inline implementation of that interface.
// Lambda expression: the Java 8 compact way to implement the same functional interface.
//
// Lambdas are commonly used for:
// 1. Passing logic to methods, such as sorting or filtering rules.
// 2. Executing a task later, such as with Runnable or callbacks.
// 3. Building stream pipelines, such as filter(), map(), and forEach().

public class S01FunctionalInterfaceAnonymousLambda {
    public static void main(String[] args) {
        Order order = new Order("Laptop", 75000, true);

        // Example 1: anonymous class implementation
        // Anonymous class:
        // This is the old style. We create an unnamed implementation of PricingRule
        // and override its single abstract method.
        PricingRule preJava8FestivalRule = new PricingRule() {
            @Override
            public double finalPrice(Order order) {
                double festivalDiscount = order.price * 0.10;
                return order.price - festivalDiscount;
            }
        };
        double festivalPrice = preJava8FestivalRule.finalPrice(order);
        System.out.println("Original price: " + order.price);
        System.out.println("Anonymous class festival price: " + festivalPrice);

        // Example 2: lambda expression implementation
        // Lambda expression:
        // A lambda is an anonymous function: it has no method name and can be passed around as data.
        // Here orderDetails is the lambda parameter, and the lambda implements PricingRule.finalPrice().
        // A lambda can implement only the one abstract method of the target functional interface.
        // Its parameter list and return type must match that abstract method.
        PricingRule java8LoyalCustomerRule = orderDetails -> {
            double loyaltyDiscount = orderDetails.loyalCustomer ? orderDetails.price * 0.05 : 0;
            double deliveryCharge = orderDetails.price < 1000 ? 80 : 0;
            return orderDetails.price - loyaltyDiscount + deliveryCharge;
        };
        // Use a lambda when you want to pass behavior, not just data.
        double loyalCustomerPrice = java8LoyalCustomerRule.finalPrice(order);
        System.out.println("Lambda loyal customer price: " + loyalCustomerPrice);
    }

    // Functional interface:
    // @FunctionalInterface is optional, but it asks the compiler to protect the contract.
    // If someone adds a second abstract method later, compilation fails immediately.
    //
    // This interface says: "pricing logic will be supplied from outside."
    // That is useful when there can be many strategies, such as different sorting rules,
    // discount rules, validation rules, or filtering rules.
    //
    // Allowed inside a functional interface:
    // 1. Exactly one abstract method.
    // 2. Any number of default methods.
    // 3. Any number of static methods.
    @FunctionalInterface
    private interface PricingRule {
        double finalPrice(Order order);
    }

    private static class Order {
        private final String item;
        private final double price;
        private final boolean loyalCustomer;

        Order(String item, double price, boolean loyalCustomer) {
            this.item = item;
            this.price = price;
            this.loyalCustomer = loyalCustomer;
        }
    }
}
