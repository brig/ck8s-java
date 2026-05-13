package ca.vanzyl.ck8s.cloudformation;

import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

public class StatementMergeStrategyTest {

    private StatementMergeStrategy strategy;

    @Before
    public void setUp() {
        strategy = new StatementMergeStrategy();
    }

    @Test
    public void mergesWhenEffectAndResourcesMatch() {
        Statement existing = new Statement(
                Statement.ALLOW,
                Set.of("s3:ListBucket"),
                Set.of(Resource.plain("arn:aws:s3:::bucket1"))
        );

        var incoming = new Statement(
                Statement.ALLOW,
                Set.of("s3:PutObject"),
                Set.of(Resource.plain("arn:aws:s3:::bucket1"))
        );

        var merged = strategy.tryMerge(existing, incoming);

        assertTrue(merged.isPresent());
        assertEquals(Set.of("s3:ListBucket", "s3:PutObject"), merged.get().actions());
        assertEquals(existing.resources(), merged.get().resources());
        assertEquals(Statement.ALLOW, merged.get().effect());
    }

    @Test
    public void doesNotMergeWhenEffectsDiffer() {
        Statement existing = new Statement(
                Statement.ALLOW,
                Set.of("s3:ListBucket"),
                Set.of(Resource.plain("arn:aws:s3:::bucket1"))
        );

        Statement incoming = new Statement(
                "Deny",
                Set.of("s3:PutObject"),
                Set.of(Resource.plain("arn:aws:s3:::bucket1"))
        );

        var merged = strategy.tryMerge(existing, incoming);

        assertFalse(merged.isPresent());
    }

    @Test
    public void doesNotMergeWhenResourcesDiffer() {
        Statement existing = new Statement(
                Statement.ALLOW,
                Set.of("s3:ListBucket"),
                Set.of(Resource.plain("arn:aws:s3:::bucket1"))
        );

        Statement incoming = new Statement(
                Statement.ALLOW,
                Set.of("s3:PutObject"),
                Set.of(Resource.plain("arn:aws:s3:::bucket2"))
        );

        var merged = strategy.tryMerge(existing, incoming);

        assertFalse(merged.isPresent());
    }

    @Test
    public void mergesWhenIncomingResourcesAreSubset() {
        Statement existing = new Statement(
                Statement.ALLOW,
                Set.of("s3:ListBucket"),
                Set.of(
                        Resource.plain("arn:aws:s3:::bucket1"),
                        Resource.plain("arn:aws:s3:::bucket2")
                )
        );

        Statement incoming = new Statement(
                Statement.ALLOW,
                Set.of("s3:GetObject"),
                Set.of(Resource.plain("arn:aws:s3:::bucket2"))
        );

        var merged = strategy.tryMerge(existing, incoming);

        assertTrue(merged.isPresent());
        assertEquals(Set.of("s3:ListBucket", "s3:GetObject"), merged.get().actions());
        assertEquals(existing.resources(), merged.get().resources());
    }
}
