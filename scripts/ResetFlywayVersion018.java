import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ResetFlywayVersion018 {
    private static final String URL = "jdbc:mysql://localhost:13306/cost_platform?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=GMT%2B8";
    private static final String USER = "root";
    private static final String PASSWORD = "root";
    private static final String VERSION = "20260405.018";

    public static void main(String[] args) throws Exception {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
            long before = queryCount(connection);
            try (PreparedStatement statement = connection.prepareStatement(
                    "delete from flyway_schema_history where version = ?")) {
                statement.setString(1, VERSION);
                int deleted = statement.executeUpdate();
                System.out.println("deleted_rows=" + deleted);
            }
            long after = queryCount(connection);
            System.out.println("before_count=" + before);
            System.out.println("after_count=" + after);
        }
    }

    private static long queryCount(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "select count(1) from flyway_schema_history where version = ?")) {
            statement.setString(1, VERSION);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0L;
            }
        }
    }
}
