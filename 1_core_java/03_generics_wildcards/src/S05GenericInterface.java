public class S05GenericInterface {
    public static void main(String[] args) {
        // Repository<T> is generic, but this variable fixes T as User.
        // Because of that, findById returns User directly. No cast is required.
        Repository<User> repository = new UserRepository();
        User user = repository.findById(101);

        System.out.println("Repository<User> returned: " + user);
    }

    // Generic interfaces are common in backend code.
    // For example, Spring repositories often use a type parameter for the entity type.
    interface Repository<T> {
        T findById(int id);
    }

    // This implementation chooses User as the concrete type for T.
    // After that choice, the method signature becomes User findById(int id).
    static class UserRepository implements Repository<User> {
        @Override
        public User findById(int id) {
            return new User(id, "Asha");
        }
    }

    // A simple domain object used by the repository example.
    static class User {
        private final int id;
        private final String name;

        User(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return "User{id=" + id + ", name='" + name + "'}";
        }
    }
}
