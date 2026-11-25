package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Añade la asignación opcional de un cuidador responsable a un grupo.
 */
public class V13__CaregiverGroupAssignment extends BaseJavaMigration {

    private static final String COLUMN_NAME = "CUIDADOR_ID";

    @Override
    public void migrate(Context context) throws SQLException {
        Connection connection = context.getConnection();
        addCaregiverColumn(connection);
        addCaregiverConstraint(connection);
        addCaregiverIndex(connection);
        ensureCaregiverRole(connection);
    }

    private void addCaregiverColumn(Connection connection) throws SQLException {
        if (hasColumn(connection, "GRUPO", COLUMN_NAME)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE grupo ADD COLUMN cuidador_id BIGINT");
        }
    }

    private void addCaregiverConstraint(Connection connection) throws SQLException {
        if (hasConstraint(connection, "GRUPO", "FK_GRUPO_CUIDADOR")) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    ALTER TABLE grupo ADD CONSTRAINT fk_grupo_cuidador
                    FOREIGN KEY (cuidador_id) REFERENCES USUARIOS(id)
                    """);
        }
    }

    private void addCaregiverIndex(Connection connection) throws SQLException {
        if (hasIndex(connection, "GRUPO", "IX_GRUPO_CUIDADOR_CENTRO")) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE INDEX ix_grupo_cuidador_centro
                    ON grupo(cuidador_id, centro_id)
                    """);
        }
    }

    private void ensureCaregiverRole(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM rol WHERE nombre = ?"
        )) {
            statement.setString(1, "CUIDADOR");
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                if (result.getInt(1) > 0) {
                    return;
                }
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO rol (nombre, descripcion) VALUES (?, ?)"
        )) {
            statement.setString(1, "CUIDADOR");
            statement.setString(2, "Personal cuidador responsable de grupos");
            statement.executeUpdate();
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
        try (PreparedStatement statement = connection.prepareStatement("""
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
