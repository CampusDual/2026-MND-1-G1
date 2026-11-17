package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Crea datos de demostración para el gerente con permisos administrativos del centro.
 */
public class V4__SeedDaycareManager extends BaseJavaMigration {

    private static final String CENTER_NAME = "Guardería Demo";
    private static final String CENTER_CIF = "B00000000";
    private static final String MANAGER_EMAIL = "admin@guarderiademo.test";
    private static final String MANAGER_PASSWORD = "admin1234";

    /**
     * Inserta el centro, el rol, el permiso y las cuentas relacionadas con el gerente.
     *
     * @param context contexto JDBC de Flyway
     * @throws SQLException si falla una operación de base de datos
     */
    @Override
    public void migrate(Context context) throws SQLException {
        Connection connection = context.getConnection();
        String passwordHash = new BCryptPasswordEncoder().encode(MANAGER_PASSWORD);

        Long centerId = findCenterId(connection);
        if (centerId == null) {
            centerId = insertCenter(connection);
        }

        Long managerRoleId = findId(connection, "SELECT id FROM rol WHERE nombre = ?", "GERENTE");
        if (managerRoleId == null) {
            managerRoleId = insertManagerRole(connection);
        }

        Long adminPermissionId = findId(connection, "SELECT id FROM permiso WHERE codigo = ?", "ADMIN_ALL");
        if (adminPermissionId == null) {
            adminPermissionId = insertAdminPermission(connection);
        }
        insertRolePermissionIfMissing(connection, managerRoleId, adminPermissionId);
        insertDaycareManagerIfMissing(connection, centerId, managerRoleId, passwordHash);

        Long legacyRoleId = findId(connection, "SELECT id FROM roles WHERE name = ?", "ROLE_ADMIN");
        if (legacyRoleId == null) {
            throw new SQLException("No existe el rol ROLE_ADMIN creado por la migración V2");
        }
        Long legacyUserId = findId(connection, "SELECT id FROM users WHERE username = ?", MANAGER_EMAIL);
        if (legacyUserId == null) {
            legacyUserId = insertLegacyUser(connection, passwordHash);
        }
        insertLegacyUserRoleIfMissing(connection, legacyUserId, legacyRoleId);
    }

    private Long findCenterId(Connection connection) throws SQLException {
        return findId(connection, "SELECT id FROM centro WHERE cif = ?", CENTER_CIF);
    }

    private Long insertCenter(Connection connection) throws SQLException {
        String sql = """
                INSERT INTO centro
                    (nombre, tipo, cif, direccion, codigo_postal, localidad, telefono, email, activo)
                VALUES (?, 'PROPIA', ?, ?, ?, ?, ?, ?, TRUE)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, CENTER_NAME);
            statement.setString(2, CENTER_CIF);
            statement.setString(3, "Calle de Ejemplo 1");
            statement.setString(4, "15001");
            statement.setString(5, "A Coruña");
            statement.setString(6, "981000000");
            statement.setString(7, "contacto@guarderiademo.test");
            statement.executeUpdate();
            return generatedId(statement, "centro");
        }
    }

    private Long insertManagerRole(Connection connection) throws SQLException {
        String sql = "INSERT INTO rol (nombre, descripcion) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, "GERENTE");
            statement.setString(2, "Gerencia del centro con permisos administrativos");
            statement.executeUpdate();
            return generatedId(statement, "rol");
        }
    }

    private Long insertAdminPermission(Connection connection) throws SQLException {
        String sql = "INSERT INTO permiso (codigo, descripcion) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, "ADMIN_ALL");
            statement.setString(2, "Acceso administrativo a las operaciones del centro");
            statement.executeUpdate();
            return generatedId(statement, "permiso");
        }
    }

    private void insertRolePermissionIfMissing(Connection connection, Long roleId, Long permissionId)
            throws SQLException {
        String sql = "SELECT COUNT(1) FROM rol_permiso WHERE rol_id = ? AND permiso_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, roleId);
            statement.setLong(2, permissionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next() && resultSet.getLong(1) > 0) {
                    return;
                }
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO rol_permiso (rol_id, permiso_id) VALUES (?, ?)")) {
            statement.setLong(1, roleId);
            statement.setLong(2, permissionId);
            statement.executeUpdate();
        }
    }

    private void insertDaycareManagerIfMissing(
            Connection connection,
            Long centerId,
            Long roleId,
            String passwordHash
    ) throws SQLException {
        if (findId(connection, "SELECT id FROM usuario WHERE email = ?", MANAGER_EMAIL) != null) {
            return;
        }
        String sql = """
                INSERT INTO usuario
                    (centro_id, rol_id, nombre, apellidos, dni, email, password_hash, activo)
                VALUES (?, ?, ?, ?, ?, ?, ?, TRUE)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, centerId);
            statement.setLong(2, roleId);
            statement.setString(3, "Admin");
            statement.setString(4, "Guardería Demo");
            statement.setString(5, "00000000T");
            statement.setString(6, MANAGER_EMAIL);
            statement.setString(7, passwordHash);
            statement.executeUpdate();
        }
    }

    private Long insertLegacyUser(Connection connection, String passwordHash) throws SQLException {
        String sql = "INSERT INTO users (username, password, enabled) VALUES (?, ?, TRUE)";
        try (PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, MANAGER_EMAIL);
            statement.setString(2, passwordHash);
            statement.executeUpdate();
            return generatedId(statement, "users");
        }
    }

    private void insertLegacyUserRoleIfMissing(Connection connection, Long userId, Long roleId)
            throws SQLException {
        String sql = "SELECT COUNT(1) FROM user_roles WHERE user_id = ? AND role_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, roleId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next() && resultSet.getLong(1) > 0) {
                    return;
                }
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)")) {
            statement.setLong(1, userId);
            statement.setLong(2, roleId);
            statement.executeUpdate();
        }
    }

    private Long findId(Connection connection, String sql, String value) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : null;
            }
        }
    }

    private Long generatedId(PreparedStatement statement, String tableName) throws SQLException {
        try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
            if (!generatedKeys.next()) {
                throw new SQLException("No se pudo recuperar el identificador generado para " + tableName);
            }
            return generatedKeys.getLong(1);
        }
    }
}
