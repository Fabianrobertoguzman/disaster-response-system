/**
 * Shared application protocol for DRS-Enhanced: the serialisable {@link
 * edu.cqu.drs.protocol.Request} / {@link edu.cqu.drs.protocol.Response} message
 * pair, the {@link edu.cqu.drs.protocol.Action} and {@link
 * edu.cqu.drs.protocol.Status} enumerations, and the {@link
 * edu.cqu.drs.protocol.ProtocolKeys} parameter-name constants.
 *
 * <p>This single package is depended on by <em>both</em> the client and the
 * server, so the two tiers always serialise and deserialise identical message
 * classes (each with an explicit {@code serialVersionUID}). Keeping the protocol
 * in one shared module is what lets the JavaFX client and the multi-threaded
 * server exchange objects over an {@link java.io.ObjectOutputStream} without
 * version drift.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
package edu.cqu.drs.protocol;
