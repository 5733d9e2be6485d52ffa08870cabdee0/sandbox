package com.redhat.service.smartevents.integration.tests;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

public class PostgreSqlFiller {

    private static final String url = "jdbc:postgresql://localhost:5432/event-bridge";
    private static final String user = "event-bridge";
    private static final String password = "event-bridge";

    /**
     * Connect to the PostgreSQL database
     *
     * @return a Connection object
     */
    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * insert multiple actors
     */
    public static void main(String[] args) {
        String SQL = "INSERT INTO bridge(id, customer_id, name, submitted_at, published_at, status, endpoint, shard_id, \"version\", dependency_status, modified_at, organisation_id, owner) "
                + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)";

        try (
                Connection conn = connect();
                PreparedStatement statement = conn.prepareStatement(SQL);) {
            //            int count = 0;

            for (int i = 0; i < 10000; i++) {
                statement.setString(1, UUID.randomUUID().toString());
                statement.setString(2, "some-customer-id");
                statement.setString(3, "bridge-name" + i);
                statement.setTimestamp(4, new Timestamp(Instant.now().toEpochMilli()));
                statement.setTimestamp(5, new Timestamp(Instant.now().toEpochMilli()));
                statement.setString(6, "READY");
                statement.setString(7, "http://192.168.39.187/ob-87d8e063-2c60-4847-9f4f-5d3b59319f8c/events");
                statement.setString(8, "150b23e9-0b34-45b7-a91a-5ec388d03a1d");
                statement.setLong(9, 1L);
                statement.setString(10, "READY");
                statement.setTimestamp(11, new Timestamp(Instant.now().toEpochMilli()));
                statement.setString(12, "some-organization-id");
                statement.setString(13, "some-owner");

                statement.addBatch();
                //                count++;
                // execute every 100 rows or less
                //                if (count % 100 == 0 || count == list.size()) {
                //                    statement.executeBatch();
                //                }
            }
            statement.executeBatch();
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }
}
