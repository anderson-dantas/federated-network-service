package org.fogbow.federatednetwork.datastore.order_storage;

import org.apache.log4j.Logger;
import org.fogbow.federatednetwork.model.FederatedOrder;
import org.fogbowcloud.manager.core.datastore.commands.TimestampSQLCommands;
import org.fogbowcloud.manager.core.datastore.orderstorage.OrderStorage;

import java.sql.*;
import java.util.*;
import java.util.Date;

public class OrderTimestampStorage extends OrderStorage {

    private static final Logger LOGGER = Logger.getLogger(OrderTimestampStorage.class);

    public OrderTimestampStorage() throws SQLException {
        Statement statement = null;
        Connection connection = null;

        try {
            connection = getConnection();

            statement = connection.createStatement();

            statement.execute(TimestampSQLCommands.CREATE_TIMESTAMP_TABLE_SQL);

            statement.close();
        } catch (SQLException e) {
            LOGGER.error("Error creating timestamp table", e);
            throw new SQLException(e);
        } finally {
            closeConnection(statement, connection);
        }
    }

    public void addOrder(FederatedOrder order) throws SQLException {
        Connection connection = null;
        PreparedStatement orderStatement = null;

        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            orderStatement = connection.prepareStatement(TimestampSQLCommands.INSERT_TIMESTAMP_SQL);

            orderStatement.setString(1, order.getId());
            orderStatement.setString(2, order.getOrderState().name());
            orderStatement.setString(3, order.getUser().getFederatedUserId());
            orderStatement.setString(4, order.getUser().getFederatedUserName());
            orderStatement.setString(5, order.getRequestingMember());
            orderStatement.setString(6, order.getProvidingMember());
            orderStatement.setTimestamp(7, new Timestamp(new Date().getTime()));

            orderStatement.executeUpdate();

            connection.commit();
        } catch (SQLException e) {
            LOGGER.error("Couldn't add timestamp.", e);
            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException e1) {
                LOGGER.error("Couldn't rollback transaction.", e1);
                throw e1;
            }

            throw e;
        } finally {
            closeConnection(orderStatement, connection);
        }
    }

    // Used for tests. Returns a map of found order and the list of states
    protected Map<String, List<String>> selectOrderById(String orderId) throws SQLException {
        PreparedStatement selectMemberStatement = null;

        Connection connection = null;

        Map<String, List<String>> listOfOrders = new HashMap<>();
        listOfOrders.put(orderId, new ArrayList<>());
        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            selectMemberStatement = connection
                    .prepareStatement(TimestampSQLCommands.SELECT_TIMESTAMP_BY_ORDER_ID_SQL);

            selectMemberStatement.setString(1, orderId);

            ResultSet rs = selectMemberStatement.executeQuery();
            while (rs.next()) {
                String state = rs.getString("order_state");
                listOfOrders.get(orderId).add(state);
            }

            connection.commit();

        } catch (SQLException e) {
            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException e1) {
                e1.printStackTrace();
                System.out.println("Couldn't rollback transaction.");
            }

        } finally {
            closeConnection(selectMemberStatement, connection);
        }

        return listOfOrders;
    }
}
