package ca.vanzyl.ck8s.utils;

import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VerifyUtilsListTest {

    @Test
    public void testExactListMatch() {
        var existing = List.of("one", "two", "three");
        var expected = List.of("one", "two", "three");

        assertTrue(VerifyUtils.verifyPartialListMatch("strings.exact", existing, expected));
    }

    @Test
    public void testPartialListMatch() {
        var existing = List.of("one", "two", "three", "four");
        var expected = List.of("two", "three");

        assertTrue(VerifyUtils.verifyPartialListMatch("strings.partial", existing, expected));
    }

    @Test
    public void testListMismatch() {
        var existing = List.of("a", "b", "c");
        var expected = List.of("a", "x");

        assertFalse(VerifyUtils.verifyPartialListMatch("strings.mismatch", existing, expected));
    }

    @Test
    public void testNullLists() {
        assertFalse(VerifyUtils.verifyPartialListMatch("nulls", null, List.of("a")));
        assertFalse(VerifyUtils.verifyPartialListMatch("nulls", List.of("a"), null));
    }

    @Test
    public void testListOfMapsExactMatch() {
        var existing = List.of(
                Map.of("name", "foo", "type", "admin"),
                Map.of("name", "bar", "type", "user")
        );
        var expected = List.of(
                Map.of("name", "foo", "type", "admin")
        );

        assertTrue(VerifyUtils.verifyPartialListMatch("maps.exact", existing, expected));
    }

    @Test
    public void testListOfMapsPartialMatch() {
        var existing = List.of(
                Map.of("name", "foo", "type", "admin", "env", "prod"),
                Map.of("name", "bar", "type", "user")
        );
        var expected = List.of(
                Map.of("name", "foo")
        );

        assertTrue(VerifyUtils.verifyPartialListMatch("maps.partial", existing, expected));
    }

    @Test
    public void testListOfMaps() {
        var existing = List.of(
                Map.of("name", "foo", "type", "admin", "env", "prod"),
                Map.of("name", "bar", "type", "user")
        );
        var expected = List.of(
                Map.of("name", "foo"),
                Map.of("env", "prod")
        );

        assertFalse(VerifyUtils.verifyPartialListMatch("maps.partial", existing, expected));
    }

    @Test
    public void testListOfMapsMismatch() {
        var existing = List.of(
                Map.of("name", "foo", "type", "admin"),
                Map.of("name", "bar", "type", "user")
        );
        var expected = List.of(
                Map.of("name", "baz", "type", "admin")
        );

        assertFalse(VerifyUtils.verifyPartialListMatch("maps.mismatch", existing, expected));
    }

    @Test
    public void testListOfNestedMapsMismatch() {
        var existing = List.of(
                Map.of("id", "1", "meta", Map.of("env", "prod")),
                Map.of("id", "2", "meta", Map.of("env", "test"))
        );
        var expected = List.<Map<String, Object>>of(
                Map.of("meta", Map.of("env", "dev"))
        );

        assertFalse(VerifyUtils.verifyPartialListMatch("maps.nested.mismatch", existing, expected));
    }

    @Test
    public void testListOfNestedMapsMatch() {
        var existing = List.of(
                Map.of("id", "1", "meta", Map.of("env", "prod")),
                Map.of("id", "2", "meta", Map.of("env", "test"))
        );
        var expected = List.<Map<String, Object>>of(
                Map.of("meta", Map.of("env", "test"))
        );

        assertTrue(VerifyUtils.verifyPartialListMatch("maps.nested.partial", existing, expected));
    }
}
