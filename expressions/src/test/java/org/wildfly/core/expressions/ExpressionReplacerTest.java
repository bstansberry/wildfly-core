package org.wildfly.core.expressions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author John Bailey
 */
public class ExpressionReplacerTest {

    final Properties properties = new Properties();
    final ExpressionReplacer replacer = ExpressionReplacer.Factory.resolvingReplacer(new PropertiesExpressionResolver(properties));

    @Before
    public void setupProperties() {
        properties.setProperty("test1", "testValue1");
        properties.setProperty("test2", "testValue2");
        properties.setProperty("test3", "testValue3");
    }

    @After

    @Test
    public void testNoProperties() throws Exception {
        final String initial = "Some string of stuff";
        final String after = replacer.replaceExpressions(initial);
        assertEquals(initial, after);
    }

    @Test
    public void testSingle() throws Exception {
        final String initial = "Some ${test1} stuff";
        final String after = replacer.replaceExpressions(initial);
        assertEquals("Some testValue1 stuff", after);
    }

    @Test
    public void testMultiple() throws Exception {
        final String initial = "${test1} ${test2} ${test3}";
        final String after = replacer.replaceExpressions(initial);
        assertEquals("testValue1 testValue2 testValue3", after);
    }

    @Test
    public void testPropertyNotFound() throws Exception {
        final String initial = "Some ${test4} stuff";
        try {
            replacer.replaceExpressions(initial);
            fail("Should have failed");
        } catch (IllegalStateException expected) {
            // ok
        }
    }

    @Test
    public void testBadProperty() throws Exception {
        final String initial = "Some ${test2 stuff";
        try {
            replacer.replaceExpressions(initial);
            fail("Should have failed");
        } catch (IllegalStateException expected) {
            // ok
        }
    }

    @Test
    public void testCustomReplacer() {
        final ExpressionReplacer replacer = new ExpressionReplacer() {
            public String replaceExpressions(String text) {
                return text.toUpperCase();
            }
        };
        final String initial = "Some string of stuff";
        final String after = replacer.replaceExpressions(initial);
        assertEquals("SOME STRING OF STUFF", after);
    }

    /** Test for JBMETA-371 */
    @Test
    public void testFullExpressionReplacementWithCompositeResolver() {
        final String vaultExpression =  "VAULT::1:2";
        final ExpressionReplacer replacer = ExpressionReplacer.Factory.resolvingReplacer(
                new SimpleExpressionResolver() {
                    @Override
                    public ResolutionResult resolveExpressionContent(String expressionContent) {
                        return vaultExpression.equals(expressionContent) ? new ResolutionResult("true", false) : null;
                    }
                },
                new PropertiesExpressionResolver(properties)
        );
        String after = replacer.replaceExpressions("${" + vaultExpression + "}");
        assertEquals("true", after);

        final String nonVaultExpression = "RANDOM::1:2";
        after = replacer.replaceExpressions("${" + nonVaultExpression + "}");
        assertEquals(":1:2", after);
    }

    @Test
    public void testFullExpressionReplacementWithCompositeLegacyResolver() {
        final String vaultExpression =  "VAULT::1:2";
        final ExpressionReplacer replacer = ExpressionReplacer.Factory.resolvingReplacer(
                new SimpleExpressionResolver() {
                    @Override
                    public ResolutionResult resolveExpressionContent(String expressionContent) {
                        return vaultExpression.equals(expressionContent) ? new ResolutionResult("true", false) : null;
                    }
                },
                new PropertiesExpressionResolver(properties)
        );
        String after = replacer.replaceExpressions("${" + vaultExpression + "}");
        assertEquals("true", after);

        final String nonVaultExpression = "RANDOM::1:2";
        after = replacer.replaceExpressions("${" + nonVaultExpression + "}");
        assertEquals(":1:2", after);
    }

    /** Test for JBMETA-371 */
    @Test
    public void testFullExpressionReplacementWithoutDefaultResolver() {
        final String vaultExpression =  "VAULT::1:2";
        final ExpressionReplacer replacer = ExpressionReplacer.Factory.resolvingReplacer(new SimpleExpressionResolver() {
            @Override
            public ResolutionResult resolveExpressionContent(String expressionContent) {
                return vaultExpression.equals(expressionContent) ? new ResolutionResult("true", false) : null;
            }
        });
        String after = replacer.replaceExpressions("${" + vaultExpression + "}");
        assertEquals("true", after);

        final String nonVaultExpression = "RANDOM::1:2";
        after = replacer.replaceExpressions("${" + nonVaultExpression + "}");
        assertEquals(":1:2", after);
    }

    @Test
    public void testNestedExpressions() {
        properties.setProperty("foo", "FOO");
        properties.setProperty("bar", "BAR");
        properties.setProperty("baz", "BAZ");
        properties.setProperty("FOO", "oof");
        properties.setProperty("BAR", "rab");
        properties.setProperty("BAZ", "zab");
        properties.setProperty("foo.BAZ.BAR", "FOO.baz.bar");
        properties.setProperty("foo.BAZBAR", "FOO.bazbar");
        properties.setProperty("bazBAR", "BAZbar");
        properties.setProperty("fooBAZbar", "FOObazBAR");
        try {
            assertEquals("FOO", replacer.replaceExpressions("${foo:${bar}}"));

            assertEquals("rab", replacer.replaceExpressions("${${bar}}"));

            assertEquals("FOO.baz.bar", replacer.replaceExpressions("${foo.${baz}.${bar}}"));

            assertEquals("FOO.bazbar", replacer.replaceExpressions("${foo.${baz}${bar}}"));

            assertEquals("aFOObazBARb", replacer.replaceExpressions("a${foo${baz${bar}}}b"));

            assertEquals("aFOObazBAR", replacer.replaceExpressions("a${foo${baz${bar}}}"));

            assertEquals("FOObazBARb", replacer.replaceExpressions("${foo${baz${bar}}}b"));

            assertEquals("aFOO.b.BARc", replacer.replaceExpressions("a${foo}.b.${bar}c"));

            properties.remove("foo");

            assertEquals("BAR", replacer.replaceExpressions("${foo:${bar}}"));

            assertEquals("BAR.{}.$$", replacer.replaceExpressions("${foo:${bar}.{}.$$}"));

            assertEquals("$BAR", replacer.replaceExpressions("$$${bar}"));

        } finally {
            properties.remove("foo");
            properties.remove("bar");
            properties.remove("baz");
            properties.remove("FOO");
            properties.remove("BAR");
            properties.remove("BAZ");
            properties.remove("foo.BAZ.BAR");
            properties.remove("foo.BAZBAR");
            properties.remove("bazBAR");
            properties.remove("fooBAZbar");
        }
    }

    @Test
    public void testDollarEscaping() {
        properties.setProperty("$$", "FOO");
        try {
            assertEquals("$", replacer.replaceExpressions("$$"));

            assertEquals("$$", replacer.replaceExpressions("$$$"));

            assertEquals("$$", replacer.replaceExpressions("$$$$"));

            assertEquals("$$", replacer.replaceExpressions("${$$$$:$$}"));

            assertEquals("FOO", replacer.replaceExpressions("${$$:$$}"));

            assertEquals("${bar}", replacer.replaceExpressions("${foo:$${bar}}"));

            assertEquals("${bar}", replacer.replaceExpressions("$${bar}"));
        } finally {
            properties.remove("$$");
        }
    }

    @Test
    public void testFileSeparator() {
        assertEquals(File.separator, replacer.replaceExpressions("${/}"));
        assertEquals(File.separator + "a", replacer.replaceExpressions("${/}a"));
        assertEquals("a" + File.separator, replacer.replaceExpressions("a${/}"));
    }

    @Test
    public void testPathSeparator() {
        assertEquals(File.pathSeparator, replacer.replaceExpressions("${:}"));
        assertEquals(File.pathSeparator + "a", replacer.replaceExpressions("${:}a"));
        assertEquals("a" + File.pathSeparator, replacer.replaceExpressions("a${:}"));
    }

    @Test
    public void testBlankExpression() {
        final String initial = "";
        final String after = replacer.replaceExpressions(initial);
        assertEquals("", after);
    }

    /**
     * Test that a incomplete expression to a system property reference throws an ISE
     */
    @Test(expected = IllegalStateException.class)
    public void testIncompleteReference() {
        String resolved = replacer.replaceExpressions("${test1");
        fail("Did not fail with ISE: " + resolved);
    }

    /**
     * Test that an incomplete expression is ignored if escaped
     */
    @Test
    public void testEscapedIncompleteReference() {
        assertEquals("${test1", replacer.replaceExpressions("$${test1"));
    }

    /**
     * Test that a incomplete expression to a system property reference throws an ISE
     */
    @Test(expected = IllegalStateException.class)
    public void testIncompleteReferenceFollowingSuccessfulResolve() {
        String resolved = replacer.replaceExpressions("${test1} ${test1");
        fail("Did not fail with OFE: " + resolved);
    }

    @Test
    public void testDefaultExpressionResolverWithRecursiveSystemPropertyResolutions() {
        // recursive example
        properties.setProperty("test.prop.prop", "${test.prop.prop.intermediate}");
        properties.setProperty("test.prop.prop.intermediate", "PROP");

        // recursive example with a property expression as the default
        properties.setProperty("test.prop.expr", "${NOTHERE:${ISHERE}}");
        properties.setProperty("ISHERE", "EXPR");

        //PROP
        try {
            assertEquals("PROP", replacer.replaceExpressions("${test.prop.prop}"));
            assertEquals("EXPR", replacer.replaceExpressions("${test.prop.expr}"));
        } finally {
            System.clearProperty("test.prop.prop");
            System.clearProperty("test.prop.prop.intermediate");
            System.clearProperty("test.prop.expr");
            System.clearProperty("ISHERE");
        }
    }

    @Test
    public void testCustomExpressionResolverRecursive() {
        final ExpressionReplacer replacer = ExpressionReplacer.Factory.resolvingReplacer(
                new SimpleExpressionResolver() {
                    @Override
                    public ResolutionResult resolveExpressionContent(String expressionContent) {

                        if (expressionContent.equals("test.prop.expr")) {
                            return new ResolutionResult("${test.prop.expr.inner}", false);
                        } else if (expressionContent.equals("test.prop.expr.inner")) {
                            return new ResolutionResult("${test1}", false);
                        }
                        return null;
                    }
                }, new PropertiesExpressionResolver(properties));
        assertEquals("testValue1", replacer.replaceExpressions("${test.prop.expr}"));
    }
}
