package edu.cqu.drs.data;

import edu.cqu.drs.model.Incident;
import edu.cqu.drs.model.Resource;
import edu.cqu.drs.model.Responder;
import edu.cqu.drs.model.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A thread-safe, in-memory backend for the whole data tier. It exposes one
 * {@link IncidentDao}, {@link ResponderDao}, {@link ResourceDao},
 * {@link AuditDao} and {@link UserDao} view ({@link #incidentDao()} etc.), all
 * sharing the same maps and one {@link ReentrantReadWriteLock}.
 *
 * <p>It serves two purposes. First, the server's service layer and the
 * end-to-end socket tests run with no MySQL present (the same services run
 * unchanged over the JDBC DAOs in production). Second, it demonstrates the
 * concurrency posture required of the multi-threaded server.</p>
 *
 * <p><strong>Locking invariant.</strong> Every mutating operation (insert,
 * update, the compound responder allocation) takes the <em>write</em> lock and
 * stores a defensive copy; every read takes the <em>read</em> lock and returns a
 * detached copy ({@code copyOf}). Consequently no object that another worker
 * thread might be mutating is ever handed out for serialisation, and the
 * read/write locks make reads and writes mutually exclusive. The append-only
 * audit trail uses a {@link CopyOnWriteArrayList}. Two honest limits remain, both
 * acceptable for this prototype: the responder-allocation step is the only
 * <em>compound</em> operation made atomic; a service-level read-modify-write
 * (triage/resolve/recommend = find, mutate the copy, update) is last-writer-wins
 * across concurrent updates to the <em>same</em> incident.</p>
 *
 * <p>The five DAO interfaces could not be implemented by one class directly,
 * because {@code findByUuid(UUID)} and {@code findAll()} appear in several of
 * them with different return types; the inner-class views resolve that cleanly
 * while still sharing one coherent set of state.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public final class InMemoryDataStore {

    private final Map<UUID, Incident> incidents = new ConcurrentHashMap<>();
    private final Map<UUID, Responder> responders = new ConcurrentHashMap<>();
    private final Map<UUID, Resource> resources = new ConcurrentHashMap<>();
    private final Map<UUID, User> users = new ConcurrentHashMap<>();
    private final Map<String, StoredUser> credentials = new ConcurrentHashMap<>();
    private final List<AuditEntry> auditEntries = new CopyOnWriteArrayList<>();

    /** Guards every mutation and snapshot read of the shared maps. */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /** @return an {@link IncidentDao} view over this store. */
    public IncidentDao incidentDao() {
        return new IncidentView();
    }

    /** @return a {@link ResponderDao} view over this store. */
    public ResponderDao responderDao() {
        return new ResponderView();
    }

    /** @return a {@link ResourceDao} view over this store. */
    public ResourceDao resourceDao() {
        return new ResourceView();
    }

    /** @return an {@link AuditDao} view over this store. */
    public AuditDao auditDao() {
        return new AuditView();
    }

    /** @return a {@link UserDao} view over this store. */
    public UserDao userDao() {
        return new UserView();
    }

    /**
     * @param condition the precondition that must hold.
     * @param message   the message for the exception if it does not.
     */
    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    // --- defensive copies (mirror the MySQL DAOs, which reconstruct fresh objects) ---

    /**
     * @param incident the stored incident (read under a lock by the caller).
     * @return a fully detached copy, including copied responders.
     */
    private static Incident copyOf(Incident incident) {
        List<Responder> responderCopies = new ArrayList<>();
        for (Responder responder : incident.getResponders()) {
            responderCopies.add(copyOf(responder));
        }
        return new Incident(incident.getId(), incident.getHazardType(), incident.getSeverity(),
                incident.getGpsLocation(), incident.getDescription(), incident.getVictimCount(),
                incident.getReportedAt(), incident.getStatus(),
                incident.getRecommendedTemplate(), responderCopies);
    }

    /**
     * @param responder the stored responder.
     * @return a detached copy.
     */
    private static Responder copyOf(Responder responder) {
        return new Responder(responder.getId(), responder.getName(),
                responder.getCurrentTaskingId());
    }

    /**
     * @param resource the stored resource.
     * @return a detached copy.
     */
    private static Resource copyOf(Resource resource) {
        return new Resource(resource.getId(), resource.getResourceType(), resource.isAvailable());
    }

    /**
     * @param user the stored user.
     * @return a detached copy.
     */
    private static User copyOf(User user) {
        return new User(user.getId(), user.getUsername(), user.getRole());
    }

    /** In-memory {@link IncidentDao} over the shared store. */
    private final class IncidentView implements IncidentDao {

        @Override
        public void insert(Incident incident) {
            require(incident != null, "incident must not be null");
            store().lock.writeLock().lock();
            try {
                store().incidents.put(incident.getId(), copyOf(incident));
            } finally {
                store().lock.writeLock().unlock();
            }
        }

        @Override
        public void update(Incident incident) {
            require(incident != null, "incident must not be null");
            store().lock.writeLock().lock();
            try {
                store().incidents.put(incident.getId(), copyOf(incident));
            } finally {
                store().lock.writeLock().unlock();
            }
        }

        @Override
        public Optional<Incident> findByUuid(UUID id) {
            require(id != null, "id must not be null");
            store().lock.readLock().lock();
            try {
                Incident incident = store().incidents.get(id);
                return (incident == null) ? Optional.empty() : Optional.of(copyOf(incident));
            } finally {
                store().lock.readLock().unlock();
            }
        }

        @Override
        public List<Incident> findAll() {
            store().lock.readLock().lock();
            try {
                List<Incident> copies = new ArrayList<>();
                for (Incident incident : store().incidents.values()) {
                    copies.add(copyOf(incident));
                }
                return copies;
            } finally {
                store().lock.readLock().unlock();
            }
        }

        @Override
        public void assignResponder(UUID incidentId, UUID responderId) {
            require(incidentId != null && responderId != null,
                    "incidentId and responderId must not be null");
            store().lock.writeLock().lock();
            try {
                Incident incident = store().incidents.get(incidentId);
                if (incident == null) {
                    throw new DataAccessException("No incident with id " + incidentId, null);
                }
                Responder responder = store().responders.get(responderId);
                if (responder == null) {
                    throw new DataAccessException("No responder with id " + responderId, null);
                }
                // Reuse the domain rule (cap of MAX_RESPONDERS, no duplicate),
                // surfacing its domain exceptions as the DAO's DataAccessException.
                incident.assignResponder(responder);
            } catch (IllegalArgumentException | IllegalStateException ex) {
                throw new DataAccessException("Could not allocate responder " + responderId
                        + " to incident " + incidentId + ": " + ex.getMessage(), ex);
            } finally {
                store().lock.writeLock().unlock();
            }
        }

        @Override
        public List<Responder> findAssignedResponders(UUID incidentId) {
            require(incidentId != null, "incidentId must not be null");
            store().lock.readLock().lock();
            try {
                Incident incident = store().incidents.get(incidentId);
                if (incident == null) {
                    return new ArrayList<>();
                }
                List<Responder> copies = new ArrayList<>();
                for (Responder responder : incident.getResponders()) {
                    copies.add(copyOf(responder));
                }
                return copies;
            } finally {
                store().lock.readLock().unlock();
            }
        }
    }

    /** In-memory {@link ResponderDao} over the shared store. */
    private final class ResponderView implements ResponderDao {

        @Override
        public void insert(Responder responder) {
            require(responder != null, "responder must not be null");
            store().lock.writeLock().lock();
            try {
                store().responders.put(responder.getId(), copyOf(responder));
            } finally {
                store().lock.writeLock().unlock();
            }
        }

        @Override
        public void update(Responder responder) {
            require(responder != null, "responder must not be null");
            store().lock.writeLock().lock();
            try {
                store().responders.put(responder.getId(), copyOf(responder));
            } finally {
                store().lock.writeLock().unlock();
            }
        }

        @Override
        public Optional<Responder> findByUuid(UUID id) {
            require(id != null, "id must not be null");
            store().lock.readLock().lock();
            try {
                Responder responder = store().responders.get(id);
                return (responder == null) ? Optional.empty() : Optional.of(copyOf(responder));
            } finally {
                store().lock.readLock().unlock();
            }
        }

        @Override
        public List<Responder> findAll() {
            store().lock.readLock().lock();
            try {
                List<Responder> copies = new ArrayList<>();
                for (Responder responder : store().responders.values()) {
                    copies.add(copyOf(responder));
                }
                return copies;
            } finally {
                store().lock.readLock().unlock();
            }
        }
    }

    /** In-memory {@link ResourceDao} over the shared store. */
    private final class ResourceView implements ResourceDao {

        @Override
        public void insert(Resource resource) {
            require(resource != null, "resource must not be null");
            store().lock.writeLock().lock();
            try {
                store().resources.put(resource.getId(), copyOf(resource));
            } finally {
                store().lock.writeLock().unlock();
            }
        }

        @Override
        public void update(Resource resource) {
            require(resource != null, "resource must not be null");
            store().lock.writeLock().lock();
            try {
                store().resources.put(resource.getId(), copyOf(resource));
            } finally {
                store().lock.writeLock().unlock();
            }
        }

        @Override
        public Optional<Resource> findByUuid(UUID id) {
            require(id != null, "id must not be null");
            store().lock.readLock().lock();
            try {
                Resource resource = store().resources.get(id);
                return (resource == null) ? Optional.empty() : Optional.of(copyOf(resource));
            } finally {
                store().lock.readLock().unlock();
            }
        }

        @Override
        public List<Resource> findAll() {
            store().lock.readLock().lock();
            try {
                List<Resource> copies = new ArrayList<>();
                for (Resource resource : store().resources.values()) {
                    copies.add(copyOf(resource));
                }
                return copies;
            } finally {
                store().lock.readLock().unlock();
            }
        }
    }

    /** In-memory {@link AuditDao} over the shared store. */
    private final class AuditView implements AuditDao {

        @Override
        public void record(AuditEntry entry) {
            require(entry != null, "entry must not be null");
            if (entry.getIncidentUuid() != null
                    && !store().incidents.containsKey(entry.getIncidentUuid())) {
                throw new DataAccessException(
                        "Unknown incident for audit entry: " + entry.getIncidentUuid(), null);
            }
            if (entry.getActorUuid() != null
                    && !store().users.containsKey(entry.getActorUuid())) {
                throw new DataAccessException(
                        "Unknown actor for audit entry: " + entry.getActorUuid(), null);
            }
            AuditEntry stamped = (entry.getTimestamp() != null) ? entry
                    : new AuditEntry(entry.getActorUuid(), entry.getIncidentUuid(),
                            entry.getAction(), entry.getEntity(), entry.getEntityUuid(),
                            LocalDateTime.now());
            store().auditEntries.add(stamped);
        }

        @Override
        public List<AuditEntry> findAll() {
            return new ArrayList<>(store().auditEntries);
        }

        @Override
        public List<AuditEntry> findByIncident(UUID incidentId) {
            require(incidentId != null, "incidentId must not be null");
            List<AuditEntry> result = new ArrayList<>();
            for (AuditEntry entry : store().auditEntries) {
                if (incidentId.equals(entry.getIncidentUuid())) {
                    result.add(entry);
                }
            }
            return result;
        }
    }

    /** In-memory {@link UserDao} over the shared store. */
    private final class UserView implements UserDao {

        @Override
        public void insert(User user, String passwordHash, String salt) {
            require(user != null && passwordHash != null && salt != null,
                    "user, passwordHash and salt are required");
            store().lock.writeLock().lock();
            try {
                if (store().credentials.containsKey(user.getUsername())) {
                    throw new DataAccessException("Duplicate username: " + user.getUsername(), null);
                }
                User stored = copyOf(user);
                store().users.put(stored.getId(), stored);
                store().credentials.put(stored.getUsername(),
                        new StoredUser(stored, passwordHash, salt));
            } finally {
                store().lock.writeLock().unlock();
            }
        }

        @Override
        public Optional<StoredUser> findByUsername(String username) {
            require(username != null, "username must not be null");
            store().lock.readLock().lock();
            try {
                StoredUser stored = store().credentials.get(username);
                return (stored == null) ? Optional.empty()
                        : Optional.of(new StoredUser(copyOf(stored.getUser()),
                                stored.getPasswordHash(), stored.getSalt()));
            } finally {
                store().lock.readLock().unlock();
            }
        }

        @Override
        public Optional<User> findByUuid(UUID id) {
            require(id != null, "id must not be null");
            store().lock.readLock().lock();
            try {
                User user = store().users.get(id);
                return (user == null) ? Optional.empty() : Optional.of(copyOf(user));
            } finally {
                store().lock.readLock().unlock();
            }
        }

        @Override
        public List<User> findAll() {
            store().lock.readLock().lock();
            try {
                List<User> copies = new ArrayList<>();
                for (User user : store().users.values()) {
                    copies.add(copyOf(user));
                }
                return copies;
            } finally {
                store().lock.readLock().unlock();
            }
        }
    }

    /** @return the enclosing store (a concise handle for the inner views). */
    private InMemoryDataStore store() {
        return InMemoryDataStore.this;
    }
}
