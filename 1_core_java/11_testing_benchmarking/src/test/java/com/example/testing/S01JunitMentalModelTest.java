package com.example.testing;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit") // Folder mental model: run all unit-tagged tests with: mvn test -Dgroups=unit
class S01JunitMentalModelTest {

    // A1. @BeforeAll: runs once before all tests in this class.
    // Must be static by default because JUnit creates a fresh test instance per test method.
    @BeforeAll
    static void beforeAll() {
        System.out.println("A1 BEFORE_ALL");
    }

    // A2. @BeforeEach: runs before every single @Test / @ParameterizedTest invocation.
    // Use it to create fresh objects so tests do not share mutable state.
    @BeforeEach
    void beforeEach() {
        System.out.println("A2 BEFORE_EACH");
    }

    // A3. @Test: marks a normal test method.
    // Interview mental model: Arrange -> Act -> Assert.
    @Test
    void normalTestUsesArrangeActAssert() {
        System.out.println("A3 TEST AAAA");

        String actual = "AAAA".toLowerCase();

        assertEquals("aaaa", actual);
    }

    // A4. @AfterEach: runs after every single test invocation.
    // Use it for cleanup like deleting temp files or clearing state.
    @AfterEach
    void afterEach() {
        System.out.println("A4 AFTER_EACH");
    }

    // A5. @AfterAll: runs once after all tests in this class.
    @AfterAll
    static void afterAll() {
        System.out.println("A5 AFTER_ALL");
    }

    // A6. assertThrows: test expected exception behavior.
    // Interview point: assertThrows returns the exception, so message/details can be checked.
    @Test
    void assertThrowsReturnsExceptionForMoreChecks() {
        System.out.println("A6 ASSERT_THROWS");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> failForDemo("AAAA")
        );

        assertEquals("bad input: AAAA", exception.getMessage());
    }

    // A7. @ParameterizedTest + @ValueSource: one argument, many values.
    // @ValueSource: one argument, many values.
    @ParameterizedTest
    @ValueSource(strings = {"AAAA", "BBBB", "CCCC"})
    void valueSourceRunsSameTestForManySingleValues(String input) {
        System.out.println("A7 VALUE_SOURCE " + input);

        assertTrue(input.length() == 4);
    }

    // A8. @ParameterizedTest + @CsvSource: multiple columns per case.
    // @CsvSource: multiple columns per case.
    @ParameterizedTest
    @CsvSource({
            "AAAA, aaaa",
            "BBBB, bbbb"
    })
    void csvSourceGivesMultipleColumns(String input, String expected) {
        System.out.println("A8 CSV_SOURCE " + input + " -> " + expected);

        assertEquals(expected, input.toLowerCase());
    }

    // A9. @ParameterizedTest + @MethodSource: use when values are richer or need Java code to build.
    // @MethodSource: use when values are richer or need Java code to build.
    @ParameterizedTest
    @MethodSource("methodSourceValues")
    void methodSourceGivesValuesFromMethod(String input) {
        System.out.println("A9 METHOD_SOURCE " + input);

        assertTrue(input.startsWith("A"));
    }

    static Stream<String> methodSourceValues() {
        return Stream.of("AAAA", "A101");
    }

    // B1. assertEquals: checks value equality using equals().
    @Test
    void b1AssertEqualsChecksValueEquality() {
        System.out.println("B1 ASSERT_EQUALS");

        assertEquals("AAAA", "AA" + "AA");
    }

    // B2. assertNotEquals: checks that two values are not equal.
    @Test
    void b2AssertNotEqualsChecksDifferentValues() {
        System.out.println("B2 ASSERT_NOT_EQUALS");

        assertNotEquals("AAAA", "BBBB");
    }

    // B3. assertTrue: use when a boolean condition should be true.
    @Test
    void b3AssertTrueChecksTrueCondition() {
        System.out.println("B3 ASSERT_TRUE");

        assertTrue("AAAA".startsWith("A"));
    }

    // B4. assertFalse: use when a boolean condition should be false.
    @Test
    void b4AssertFalseChecksFalseCondition() {
        System.out.println("B4 ASSERT_FALSE");

        assertFalse("AAAA".isBlank());
    }

    // B5. assertNull: use when no value is expected.
    @Test
    void b5AssertNullChecksNoValue() {
        System.out.println("B5 ASSERT_NULL");

        String value = null;

        assertNull(value);
    }

    // B6. assertSame vs assertEquals:
    // assertEquals checks logical equality. assertSame checks both references point to the exact same object.
    // Interview point: most business tests need assertEquals; assertSame is for identity/singleton/cache style checks.
    @Test
    void b6AssertSameVsAssertEquals() {
        System.out.println("B6 ASSERT_SAME_VS_ASSERT_EQUALS");

        String first = new String("AAAA");
        String second = new String("AAAA");
        String sameReference = first;

        assertEquals(first, second);
        assertSame(first, sameReference);
    }

    // B7. Assertions.assertThrows vs static assertThrows:
    // Both call the same JUnit API. Static import is shorter; class-qualified call can be clearer while learning.
    @Test
    void b7AssertionsAssertThrowsVsStaticAssertThrows() {
        System.out.println("B7 ASSERTIONS_ASSERT_THROWS_VS_STATIC_ASSERT_THROWS");

        RuntimeException first = Assertions.assertThrows(RuntimeException.class, () -> failRuntime("BBBB"));
        RuntimeException second = assertThrows(RuntimeException.class, () -> failRuntime("CCCC"));

        assertAll(
                () -> assertEquals("BBBB failed", first.getMessage()),
                () -> assertEquals("CCCC failed", second.getMessage())
        );
    }

    private static void failForDemo(String input) {
        throw new IllegalArgumentException("bad input: " + input);
    }

    private static void failRuntime(String input) {
        throw new RuntimeException(input + " failed");
    }
}
