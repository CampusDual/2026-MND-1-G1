package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Añade perro, hora y servicios solicitados para consultar solicitudes de peluquería.
 */
public class V15__GroomingRequestDetails extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws SQLException {
        Connection connection = context.getConnection();
        addDogColumn(connection);
        addRequestedTimeColumn(connection);
        addDogForeignKey(connection);
        createRequestServiceTable(connection);
        createRequestIndexes(connection);
    }

    private void addDogColumn(Connection connection) throws SQLException {
        if (hasColumn(connection, "SOLICITUD_PELUQUERIA", "PERRO_ID")) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE solicitud_peluqueria ADD COLUMN perro_id BIGINT");
        }
    }

    private void addRequestedTimeColumn(Connection connection) throws SQLException {
        if (hasColumn(connection, "SOLICITUD_PELUQUERIA", "HORA_SOLICITADA")) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE solicitud_peluqueria ADD COLUMN hora_solicitada TIME");
        }
    }

    private void addDogForeignKey(Connection connection) throws SQLException {
        if (hasConstraint(connection, "SOLICITUD_PELUQUERIA", "FK_SOLICITUD_PELUQUERIA_PERRO")) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    ALTER TABLE solicitud_peluqueria ADD CONSTRAINT fk_solicitud_peluqueria_perro
                    FOREIGN KEY (perro_id) REFERENCES perro(id)
                    """);
        }
    }

    private void createRequestServiceTable(Connection connection) throws SQLException {
        if (hasTable(connection, "SOLICITUD_PELUQUERIA_SERVICIO")) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE solicitud_peluqueria_servicio (
                        solicitud_id BIGINT NOT NULL,
                        servicio_id BIGINT NOT NULL,
                        CONSTRAINT pk_solicitud_peluqueria_servicio
                            PRIMARY KEY (solicitud_id, servicio_id),
                        CONSTRAINT fk_sol_pelu_serv_solicitud
                            FOREIGN KEY (solicitud_id) REFERENCES solicitud_peluqueria(id),
                        CONSTRAINT fk_sol_pelu_serv_servicio
                            FOREIGN KEY (servicio_id) REFERENCES servicio_peluqueria(id)
                    )
                    """);
        }
    }

    private void createRequestIndexes(Connection connection) throws SQLException {
        if (!hasIndex(connection, "SOLICITUD_PELUQUERIA", "IX_SOL_PELU_PERRO")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                        CREATE INDEX ix_sol_pelu_perro ON solicitud_peluqueria(perro_id)
                        """);
            }
        }
        if (!hasIndex(connection, "SOLICITUD_PELUQUERIA_SERVICIO", "IX_SOL_PELU_SERVICIO")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                        CREATE INDEX ix_sol_pelu_servicio
                        ON solicitud_peluqueria_servicio(servicio_id, solicitud_id)
                        """);
            }
        }
    }

    private boolean hasColumn(Connection connection, String tableName, String columnName)
            throws SQLException {
        try (ResultSet result = connection.getMetaData().getColumns(
                null,
                null,
                tableName,
                columnName
        )) {
            return result.next();
        }
    }

    private boolean hasConstraint(Connection connection, String tableName, String constraintName)
            throws SQLException {
        try (var statement = connection.prepareStatement("""
                SELECT COUNT(*) FROM information_schema.table_constraints
                WHERE UPPER(table_name) = UPPER(?)
                  AND UPPER(constraint_name) = UPPER(?)
                """)) {
            statement.setString(1, tableName);
            statement.setString(2, constraintName);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1) > 0;
            }
        }
    }

    private boolean hasTable(Connection connection, String tableName) throws SQLException {
        try (ResultSet result = connection.getMetaData().getTables(
                null,
                null,
                tableName,
                new String[]{"TABLE"}
        )) {
            return result.next();
        }
    }

    private boolean hasIndex(Connection connection, String tableName, String indexName)
            throws SQLException {
        try (ResultSet result = connection.getMetaData().getIndexInfo(
                null,
                null,
                tableName,
                false,
                false
        )) {
            while (result.next()) {
                String foundName = result.getString("INDEX_NAME");
                if (foundName != null && foundName.equalsIgnoreCase(indexName)) {
                    return true;
                }
            }
            return false;
        }
    }
}
