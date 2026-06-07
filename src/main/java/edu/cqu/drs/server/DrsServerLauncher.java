package edu.cqu.drs.server;

import edu.cqu.drs.data.AuditDao;
import edu.cqu.drs.data.AuditDaoImpl;
import edu.cqu.drs.data.Database;
import edu.cqu.drs.data.IncidentDao;
import edu.cqu.drs.data.IncidentDaoImpl;
import edu.cqu.drs.data.ResponderDao;
import edu.cqu.drs.data.ResponderDaoImpl;
import edu.cqu.drs.presenter.AlertTemplateRecommender;
import edu.cqu.drs.server.service.IncidentService;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Entry point that starts the DRS-Enhanced server against the MySQL data tier.
 *
 * <p>Start sequence: ensure MySQL is running, then run this class (optionally with
 * a port as the first argument; the default is {@link DrsServer#DEFAULT_PORT}).
 * It applies the schema and seed scripts, wires the JDBC DAOs into the service
 * layer and the request dispatcher, and starts accepting clients. Clients then
 * connect with an {@link edu.cqu.drs.client.ServerStub}.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public final class DrsServerLauncher {

    private DrsServerLauncher() {
    }

    /**
     * Boots the server.
     *
     * @param args optional single argument: the listening port.
     * @throws IOException  if the database scripts cannot be read or the socket cannot bind.
     * @throws SQLException if the schema/seed cannot be applied.
     */
    public static void main(String[] args) throws IOException, SQLException {
        int port = (args.length > 0) ? Integer.parseInt(args[0]) : DrsServer.DEFAULT_PORT;

        Database database = new Database();
        database.initialise();

        IncidentDao incidentDao = new IncidentDaoImpl(database);
        ResponderDao responderDao = new ResponderDaoImpl(database);
        AuditDao auditDao = new AuditDaoImpl(database);
        IncidentService incidentService = new IncidentService(
                incidentDao, responderDao, auditDao, new AlertTemplateRecommender());
        RequestDispatcher dispatcher = new DrsRequestDispatcher(incidentService);

        DrsServer server = new DrsServer(port, dispatcher);
        server.start();
        System.out.println("DRS-Enhanced server listening on port " + server.getPort());
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "drs-shutdown"));
    }
}
