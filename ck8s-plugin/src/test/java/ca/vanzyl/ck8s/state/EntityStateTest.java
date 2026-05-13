package ca.vanzyl.ck8s.state;

import com.walmartlabs.concord.runtime.v2.runner.PersistenceService;
import org.junit.Before;
import org.junit.Test;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class EntityStateTest {

    private EntityState entityState;
    private PersistenceService mockPersistence;

    private record TestKey(String id) implements EntityKey<TestEntity> {

        @Override
        public Class<TestEntity> entityType() {
            return TestEntity.class;
        }
    }

    private record TestEntity(String value) implements Entity {

        @Override
        public String entityName() {
            return "test-entity";
        }

        @Override
        public void dump(OutputStream out) {
        }
    }

    @Before
    public void setUp() {
        mockPersistence = mock(PersistenceService.class);
        entityState = new EntityState(mockPersistence);
    }

    @Test
    public void testPutAndGet() {
        var key = new TestKey("k1");
        var entity = new TestEntity("v1");

        entityState.put(key, entity);
        var result = entityState.get(key);

        assertNotNull(result);
        assertEquals("v1", result.value());
    }

    @Test
    public void testGetInitial() {
        var key = new TestKey("k1");
        var entity = new TestEntity("v1");

        entityState.put(key, entity);
        var result = entityState.getInitial(key);

        assertNotNull(result);
        assertEquals("v1", result.value());
    }

    @Test
    public void testGetOrLoad() {
        var key = new TestKey("k2");
        var entity = new TestEntity("loaded");

        EntityLoader<TestKey, TestEntity> loader = k -> entity;
        var result = entityState.getOrLoad(key, loader);

        assertNotNull(result);
        assertEquals("loaded", result.value());
        assertEquals(result, entityState.get(key));
    }

    @Test
    public void testDelete() {
        var key = new TestKey("k3");
        var entity = new TestEntity("toDelete");

        entityState.put(key, entity);
        entityState.delete(key);

        assertNull(entityState.get(key));
    }

    @Test
    public void testListByType() {
        TestKey key1 = new TestKey("a");
        TestEntity entity1 = new TestEntity("A");

        TestKey key2 = new TestKey("b");
        TestEntity entity2 = new TestEntity("B");

        entityState.put(key1, entity1);
        entityState.put(key2, entity2);

        List<Map.Entry<TestKey, TestEntity>> list = entityState.list(TestEntity.class);
        assertEquals(2, list.size());
    }
}
