package edu.cqu.drs.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * A priority-ordered queue of reported incidents, used by the Dispatcher console to surface the
 * most urgent incident first (FR-04 "prioritized responses accordingly").
 *
 * <p>Ordering: highest {@link Severity} priority first; ties broken by highest {@link Incident#getVictimCount()}
 *  -  so a CRITICAL incident affecting many people is dequeued before a CRITICAL incident affecting
 * few, which is dequeued before a HIGH incident, and so on. This makes the prioritisation rule a data
 * comparison ({@code Severity.getPriority()} + {@code victimCount}) rather than a {@code switch} on a
 * discriminator. The queue is in-memory only  -  it is the prototype's stand-in for the durable
 * incident store that is a Stage-3 item.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class IncidentQueue {

    /** Most-urgent-first comparator: severity priority descending, then victim count descending. */
    private static final Comparator<Incident> MOST_URGENT_FIRST =
            Comparator.comparingInt((Incident incident) -> incident.getSeverity().getPriority())
                      .thenComparingInt(Incident::getVictimCount)
                      .reversed();

    /** The backing priority queue, ordered by {@link #MOST_URGENT_FIRST}. */
    private final PriorityQueue<Incident> queue;

    /**
     * Creates an empty incident queue.
     */
    public IncidentQueue() {
        this.queue = new PriorityQueue<>(MOST_URGENT_FIRST);
    }

    /**
     * Adds an incident to the queue.
     *
     * @param incident the incident to enqueue (must not be null).
     * @throws IllegalArgumentException if {@code incident} is null.
     */
    public void enqueue(Incident incident) {
        if (incident == null) {
            throw new IllegalArgumentException("incident must not be null");
        }
        this.queue.add(incident);
    }

    /**
     * Removes and returns the most urgent incident.
     *
     * @return the most urgent incident, or {@code null} if the queue is empty.
     */
    public Incident dequeue() {
        return this.queue.poll();
    }

    /**
     * Returns  -  without removing  -  the most urgent incident.
     *
     * @return the most urgent incident, or {@code null} if the queue is empty.
     */
    public Incident peek() {
        return this.queue.peek();
    }

    /**
     * @return the number of incidents currently in the queue.
     */
    public int size() {
        return this.queue.size();
    }

    /**
     * @return true if the queue holds no incidents.
     */
    public boolean isEmpty() {
        return this.queue.isEmpty();
    }

    /**
     * Returns a snapshot of the queued incidents in most-urgent-first order, without removing them.
     * (A {@link PriorityQueue}'s own iterator is not in priority order, so the snapshot is sorted here.)
     * Used by the Dispatcher console to populate the incident table.
     *
     * @return a new list of the queued incidents, most urgent first (empty if the queue is empty).
     */
    public List<Incident> asSortedList() {
        List<Incident> snapshot = new ArrayList<>(this.queue);
        snapshot.sort(MOST_URGENT_FIRST);
        return snapshot;
    }

    /**
     * Removes the given incident from the queue, if present. Used when an
     * incident is re-triaged to a different severity: a {@link PriorityQueue}
     * does not re-order an element whose comparator key changed in place, so
     * the caller removes the incident and then re-{@link #enqueue}s it. Resolved
     * incidents are kept in the queue by design so the dispatcher can still
     * see them.
     *
     * @param incident the incident to remove.
     * @return true if the incident was present and removed; false otherwise.
     */
    public boolean remove(Incident incident) {
        return this.queue.remove(incident);
    }

    /**
     * @param incident the incident to look for.
     * @return true if the queue currently holds this incident.
     */
    public boolean contains(Incident incident) {
        return this.queue.contains(incident);
    }

    /**
     * Hand-written display string showing the queue size and head; the default {@code Object.toString()} returns Class@hash.
     *
     * @return e.g. {@code "IncidentQueue{size=3, head=Incident{...}}"}.
     */
    @Override
    public String toString() {
        return "IncidentQueue{size=" + this.queue.size() + ", head=" + this.queue.peek() + "}";
    }
}
