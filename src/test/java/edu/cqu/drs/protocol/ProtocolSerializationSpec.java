package edu.cqu.drs.protocol;

import edu.cqu.drs.model.GpsCoordinate;
import edu.cqu.drs.model.HazardType;
import edu.cqu.drs.model.Incident;
import edu.cqu.drs.model.Responder;
import edu.cqu.drs.model.Severity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that the protocol messages and the domain objects they carry survive a
 * full {@link ObjectOutputStream} / {@link ObjectInputStream} round-trip - the
 * exact serialisation the client and server perform over a socket. Pure
 * in-memory; no database or network required, so these run unconditionally.
 *
 * @author Fabian Roberto Guzman (12287570)
 */
@DisplayName("Protocol - Request/Response serialisation round-trip")
class ProtocolSerializationSpec {

    /**
     * Serialises an object to bytes and reads it back.
     *
     * @param original the object to round-trip.
     * @return the deserialised copy.
     * @throws Exception if (de)serialisation fails.
     */
    private static Serializable roundTrip(Serializable original) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(original);
            out.flush();
        }
        try (ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()))) {
            return (Serializable) in.readObject();
        }
    }

    @Test
    @DisplayName("a Request round-trips its action, token and typed parameters")
    void shouldRoundTripRequest() throws Exception {
        UUID incidentId = UUID.randomUUID();
        Request original = new Request(Action.TRIAGE_INCIDENT)
                .withToken("session-123")
                .with(ProtocolKeys.INCIDENT_ID, incidentId)
                .with(ProtocolKeys.SEVERITY, Severity.CRITICAL);

        Request copy = (Request) roundTrip(original);
        assertEquals(Action.TRIAGE_INCIDENT, copy.getAction());
        assertEquals("session-123", copy.getToken());
        assertEquals(incidentId, copy.getUuid(ProtocolKeys.INCIDENT_ID));
        assertEquals(Severity.CRITICAL, copy.get(ProtocolKeys.SEVERITY));
    }

    @Test
    @DisplayName("a Response carrying a full Incident graph round-trips intact")
    void shouldRoundTripResponseWithIncident() throws Exception {
        Incident incident = new Incident(HazardType.FIRE,
                new GpsCoordinate(-23.3781, 150.5136), "Warehouse blaze", 3);
        incident.triage(Severity.HIGH);
        incident.assignResponder(new Responder("Alpha"));

        Response copy = (Response) roundTrip(Response.ok(incident));
        assertTrue(copy.isOk());
        assertTrue(copy.getPayload() instanceof Incident);
        Incident restored = (Incident) copy.getPayload();
        assertEquals(incident.getId(), restored.getId());
        assertEquals(HazardType.FIRE, restored.getHazardType());
        assertEquals(Severity.HIGH, restored.getSeverity());
        assertEquals(1, restored.getResponders().size());
        assertEquals("Alpha", restored.getResponders().get(0).getName());
    }

    @Test
    @DisplayName("an error Response round-trips its status and message with a null payload")
    void shouldRoundTripErrorResponse() throws Exception {
        Response copy = (Response) roundTrip(Response.unauthorized("no token"));
        assertEquals(Status.UNAUTHORIZED, copy.getStatus());
        assertEquals("no token", copy.getMessage());
        assertNull(copy.getPayload());
    }
}
