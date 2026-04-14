import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

public class InspectCostDemoData {
    private static final String URL = "jdbc:mysql://localhost:13306/cost_platform?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=GMT%2B8";
    private static final String USER = "root";
    private static final String PASSWORD = "root";

    private static final String[] SCENE_CODES = {
        "PORT-OPS-001",
        "SALARY-OUTSOURCE-001",
        "STORAGE-LEASE-001",
        "SHOUGANG-ORE-HR-001"
    };

    public static void main(String[] args) throws Exception {
        Map<String, String> tables = new LinkedHashMap<>();
        tables.put("cost_scene", "select count(1) from cost_scene where scene_code = ?");
        tables.put("cost_fee_item", "select count(1) from cost_fee_item where scene_id = ?");
        tables.put("cost_variable_group", "select count(1) from cost_variable_group where scene_id = ?");
        tables.put("cost_variable", "select count(1) from cost_variable where scene_id = ?");
        tables.put("cost_fee_variable_rel", "select count(1) from cost_fee_variable_rel where scene_id = ?");
        tables.put("cost_rule", "select count(1) from cost_rule where scene_id = ?");
        tables.put("cost_rule_condition", "select count(1) from cost_rule_condition where scene_id = ?");
        tables.put("cost_rule_tier", "select count(1) from cost_rule_tier where scene_id = ?");
        tables.put("cost_formula", "select count(1) from cost_formula where scene_id = ?");
        tables.put("cost_formula_version", "select count(1) from cost_formula_version where scene_id = ?");
        tables.put("cost_publish_version", "select count(1) from cost_publish_version where scene_id = ?");
        tables.put("cost_publish_snapshot", "select count(1) from cost_publish_snapshot where version_id in (select version_id from cost_publish_version where scene_id = ?)");
        tables.put("cost_bill_period", "select count(1) from cost_bill_period where scene_id = ?");
        tables.put("cost_calc_task", "select count(1) from cost_calc_task where scene_id = ?");
        tables.put("cost_result_ledger", "select count(1) from cost_result_ledger where scene_id = ?");
        tables.put("cost_result_trace", "select count(1) from cost_result_trace where scene_id = ?");

        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
            for (String sceneCode : SCENE_CODES) {
                Long sceneId = querySceneId(connection, sceneCode);
                System.out.println("sceneCode=" + sceneCode + ", sceneId=" + sceneId);
                for (Map.Entry<String, String> entry : tables.entrySet()) {
                    long count;
                    if ("cost_scene".equals(entry.getKey())) {
                        count = queryCountBySceneCode(connection, entry.getValue(), sceneCode);
                    } else if (sceneId == null) {
                        count = 0L;
                    } else {
                        count = queryCountBySceneId(connection, entry.getValue(), sceneId);
                    }
                    System.out.println("  " + entry.getKey() + "=" + count);
                }
            }
        }
    }

    private static Long querySceneId(Connection connection, String sceneCode) throws Exception {
        String sql = "select scene_id from cost_scene where scene_code = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, sceneCode);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : null;
            }
        }
    }

    private static long queryCountBySceneCode(Connection connection, String sql, String sceneCode) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, sceneCode);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0L;
            }
        }
    }

    private static long queryCountBySceneId(Connection connection, String sql, long sceneId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, sceneId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0L;
            }
        }
    }
}
