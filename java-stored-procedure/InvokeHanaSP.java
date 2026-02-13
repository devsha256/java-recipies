package HanaDBConfig;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.StringWriter;
import java.sql.*;

/**
 * <h2>InvokeHanaSP</h2>
 * <p>
 * A high-performance utility for invoking SAP HANA Stored Procedures within a Mule 4 environment.
 * This class is engineered to eliminate common JDBC performance bottlenecks including:
 * <ul>
 * <li><b>Connection Overhead:</b> Utilizes HikariCP for high-speed connection pooling.</li>
 * <li><b>Serialization Latency:</b> Uses Jackson Streaming API to convert ResultSets directly to JSON strings.</li>
 * <li><b>Metadata Overhead:</b> Uses fixed-index positional fetching to bypass expensive database metadata lookups.</li>
 * </ul>
 * </p>
 * * @author Saddam
 * @version 1.0
 */
public class InvokeHanaSP {

    private static volatile HikariDataSource dataSource;
    private static final JsonFactory jsonFactory = new JsonFactory();

    /**
     * Initializes the HikariCP connection pool if it hasn't been created.
     * Implements thread-safe double-checked locking.
     */
    private static void initPool(String url, String user, String password) {
        if (dataSource == null) {
            synchronized (InvokeHanaSP.class) {
                if (dataSource == null) {
                    HikariConfig config = new HikariConfig();
                    config.setJdbcUrl(url);
                    config.setUsername(user);
                    config.setPassword(password);
                    config.setDriverClassName("com.sap.db.jdbc.Driver");
                    
                    // Optimization for low-latency transaction processing
                    config.setMaximumPoolSize(20); 
                    config.setMinimumIdle(10); 
                    config.addDataSourceProperty("cachePrepStmts", "true");
                    config.addDataSourceProperty("prepStmtCacheSize", "250");
                    config.addDataSourceProperty("useServerPrepStmts", "true");
                    
                    dataSource = new HikariDataSource(config);
                }
            }
        }
    }

    /**
     * Invokes a HANA Stored Procedure and streams the result directly to a JSON String.
     * * @param proc The SQL call string (e.g., "{call MY_PROC()}").
     * @param url The JDBC connection URL.
     * @param user The database username.
     * @param password The database password.
     * @param columns An ordered array of column names matching the SP's output table structure.
     * @return A JSON formatted String representing the result set.
     * @throws RuntimeException if database access or JSON serialization fails.
     */
    public static String callspAsJson(String proc, String url, String user, String password, String[] columns) {
        initPool(url, user, password);
        
        // Pre-allocated for small 0-2 row payloads
        StringWriter writer = new StringWriter(1024);

        try (Connection conn = dataSource.getConnection();
             CallableStatement cstmt = conn.prepareCall(proc);
             ResultSet rs = cstmt.executeQuery();
             JsonGenerator jg = jsonFactory.createGenerator(writer)) {

            jg.writeStartArray();
            int colCount = columns.length;

            while (rs.next()) {
                jg.writeStartObject();
                for (int i = 0; i < colCount; i++) {
                    // Positional fetching for O(1) column access speed
                    jg.writeFieldName(columns[i]);
                    jg.writeObject(rs.getObject(i + 1));
                }
                jg.writeEndObject();
            }
            jg.writeEndArray();
            jg.flush();

        } catch (Exception e) {
            throw new RuntimeException("HANA_INVOCATION_FAILURE: " + e.getMessage(), e);
        }

        return writer.toString();
    }
}
