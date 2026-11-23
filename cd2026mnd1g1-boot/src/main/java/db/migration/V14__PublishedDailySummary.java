package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Añade auditoría mínima y estado a los resúmenes diarios publicados.
 */
public class V14__PublishedDailySummary extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws SQLException {
        Connection connection = context.getConnection();
        addColumn(connection, "cuidador_id", "BIGINT");
        addColumn(connection, "centro_id", "BIGINT");
        addColumn(connection, "estado", "VARCHAR(20)");
        addColumn(connection, "publicado_en", "TIMESTAMP");

        addForeignKey(connection);
        addCenterForeignKey(connection);
        addUniqueConstraint(connection);

        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    UPDATE resumen_diario
                    SET estado = 'PUBLICADO'
                    WHERE estado IS NULL
                    """);
            statement.execute("""
                    UPDATE resumen_diario
                    SET publicado_en = generado_en
                    WHERE publicado_en IS NULL
                    """);
        }
    }

    private void addColumn(Connection connection, String name, String type) throws SQLException {
        if (hasColumn(connection, "RESUMEN_DIARIO", name)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE resumen_diario ADD COLUMN " + name + " " + type);
        }
    }

    private void addForeignKey(Connection connection) throws SQLException {
        if (hasConstraint(connection, "FK_RESUMEN_CUIDADOR")) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    ALTER TABLE resumen_diario ADD CONSTRAINT fk_resumen_cuidador
                    FOREIGN KEY (cuidador_id) REFERENCES USUARIOS(id)
                    """);
        }
    }

    private void addCenterForeignKey(Connection connection) throws SQLException {
        if (hasConstraint(connection, "FK_RESUMEN_CENTRO")) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    ALTER TABLE resumen_diario ADD CONSTRAINT fk_resumen_centro
                    FOREIGN KEY (centro_id) REFERENCES centro(id)
                    """);
        }
    }

    private void addUniqueConstraint(Connection connection) throws SQLException {
        if (hasConstraint(connection, "UQ_RESUMEN_RESERVA_DESTINATARIO")) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            ResultSet result = statement.executeQuery("""
                    SELECT COUNT(*) FROM (
                        SELECT reserva_id, destinatario
                        FROM resumen_diario
                        GROUP BY reserva_id, destinatario
                        HAVING COUNT(*) > 1
                    ) duplicate_summaries
                    """);
            result.next();
            if (result.getInt(1) == 0) {
                result.close();
                statement.execute("""
                        ALTER TABLE resumen_diario
                        ADD CONSTRAINT uq_resumen_reserva_destinatario
                        UNIQUE (reserva_id, destinatario)
                        """);
            } else {
                result.close();
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

    private boolean hasConstraint(Connection connection, String constraintName) throws SQLException {
        try (ResultSet result = connection.getMetaData().getTables(
                null,
                null,
                "%",
                new String[]{"TABLE"}
        )) {
            while (result.next()) {
                // La comprobación específica se realiza por metadatos de restricciones si está disponible.
            }
        }
        try (var statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE UPPER(constraint_name) = UPPER(?)
                """)) {
            statement.setString(1, constraintName);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1) > 0;
            }
        }
    }
}
