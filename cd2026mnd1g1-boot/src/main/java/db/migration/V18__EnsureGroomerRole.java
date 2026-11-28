package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Garantiza el rol necesario para identificar peluqueros en las agendas de recepción.
 */
public class V18__EnsureGroomerRole extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws SQLException {
        Connection connection = context.getConnection();
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT COUNT(*) FROM ROL WHERE nombre = ?"
        )) {
            query.setString(1, "PELUQUERO");
            try (ResultSet result = query.executeQuery()) {
                result.next();
                if (result.getInt(1) > 0) {
                    return;
                }
            }
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO ROL (nombre, descripcion) VALUES (?, ?)"
        )) {
            insert.setString(1, "PELUQUERO");
            insert.setString(2, "Personal de peluquería y gestión de agenda");
            insert.executeUpdate();
        }
    }
}
