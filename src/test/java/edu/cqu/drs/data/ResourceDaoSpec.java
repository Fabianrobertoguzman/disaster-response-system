package edu.cqu.drs.data;

import edu.cqu.drs.model.Resource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests for {@link ResourceDaoImpl} against the selected JDBC backend (MySQL, or in-memory H2 under -Ptest-h2).
 *
 * <p>Skipped (not failed) when no database is reachable - see
 * {@link DatabaseTestSupport}. Each test starts from a freshly initialised schema
 * and seed.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
@DisplayName("ResourceDao - resource persistence (MySQL/H2)")
class ResourceDaoSpec {

    /** Number of resources created by the reference seed. */
    private static final int SEEDED_RESOURCES = 3;

    private Database database;
    private ResourceDao dao;

    @BeforeAll
    static void requireDatabase() {
        assumeTrue(DatabaseTestSupport.available(),
                "MySQL not reachable - ResourceDao integration tests skipped");
    }

    @BeforeEach
    void setUp() throws Exception {
        this.database = DatabaseTestSupport.freshDatabase();
        this.dao = new ResourceDaoImpl(this.database);
    }

    @Test
    @DisplayName("insert then findByUuid round-trips the id, type and availability")
    void shouldRoundTripResource() {
        Resource resource = new Resource("Ambulance");
        this.dao.insert(resource);

        Optional<Resource> loaded = this.dao.findByUuid(resource.getId());
        assertTrue(loaded.isPresent());
        assertEquals(resource.getId(), loaded.get().getId());
        assertEquals("Ambulance", loaded.get().getResourceType());
        assertTrue(loaded.get().isAvailable());
    }

    @Test
    @DisplayName("update persists a changed type and availability")
    void shouldPersistUpdate() {
        Resource resource = new Resource("Fire Appliance");
        this.dao.insert(resource);

        resource.setResourceType("Heavy Pump");
        resource.markAllocated();
        this.dao.update(resource);

        Resource loaded = this.dao.findByUuid(resource.getId()).orElseThrow();
        assertEquals("Heavy Pump", loaded.getResourceType());
        assertFalse(loaded.isAvailable());
    }

    @Test
    @DisplayName("findAll returns the seeded resources plus any newly inserted one")
    void shouldListSeededAndInserted() {
        assertEquals(SEEDED_RESOURCES, this.dao.findAll().size());

        this.dao.insert(new Resource("Rescue Tender"));
        assertEquals(SEEDED_RESOURCES + 1, this.dao.findAll().size());
    }

    @Test
    @DisplayName("findByUuid returns empty for an unknown id")
    void shouldReturnEmptyForUnknownId() {
        assertTrue(this.dao.findByUuid(UUID.randomUUID()).isEmpty());
    }
}
