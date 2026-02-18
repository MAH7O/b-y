package de.thm.mni.pi2.adder.handler;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Handler class for User-related operations.
 * Handles user creation, retrieval, updates, deletions, and pagination.
 */
public class UserHandler {

    private final Connection conn;

    public UserHandler(Connection conn) {
        this.conn = conn;
    }

    /**
     * Retrieves all users.
     *
     * @param context The routing context.
     *                Returns 200 with a JSON array of users on success.
     *                Returns 500 if no users are found or on error.
     */
    public void getAllUsers(RoutingContext context) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT u.id,u.username,r.role FROM users u JOIN userrole ur ON u.id = ur.userid JOIN roles r ON ur.roleid = r.id ORDER BY u.id");
            ResultSet res = ps.executeQuery();
            JsonArray respond = new JsonArray();
            boolean first = true;

            while (res.next()) {
                JsonObject user = new JsonObject();
                user.put("id", res.getInt("id"));
                user.put("username", res.getString("username"));
                user.put("role", res.getString("role"));
                respond.add(user);
                first = false;
            }

            if (first) {
                context.response().setStatusCode(500).end("No Users");
            } else {
                context.response().setStatusCode(200).end(respond.encodePrettily());
            }

        } catch (Exception e) {
            context.response().setStatusCode(500).end(e.getMessage());
        }
    }

    /**
     * Retrieves the logged-in user's details.
     *
     * @param context The routing context.
     *                Returns 401 if the user is not logged in.
     *                Returns 200 with the user details on success.
     *                Returns 404 if the user is not found.
     *                Returns 500 on error.
     */
    public void getUser(RoutingContext context) {
        try {
            Integer id = context.session().get("id");
            if (id == null || id == 0) {
                context.response().setStatusCode(401)
                        .end(new JsonObject().put("message", "You must be logged in").encode());
                return;
            }

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT u.id,u.username,r.role FROM users u JOIN userrole ur ON u.id = ur.userid JOIN roles r ON ur.roleid = r.id WHERE u.id = ? ORDER BY u.id");
            ps.setInt(1, id);
            ResultSet res = ps.executeQuery();
            JsonArray respond = new JsonArray();

            boolean first = true;
            while (res.next()) {
                JsonObject user = new JsonObject();
                user.put("id", res.getInt("id"));
                user.put("username", res.getString("username"));
                user.put("role", res.getString("role"));
                respond.add(user);
                first = false;
            }

            if (first) {
                context.response().setStatusCode(404).end("User Not Found");
            } else {
                context.response().setStatusCode(200).end(respond.encodePrettily());
            }
        } catch (Exception e) {
            context.response().setStatusCode(500).end(e.getMessage());
        }
    }

    /**
     * Creates a new user.
     *
     * @param context The routing context containing the request body.
     *                Requires "username", "password", and "role" fields.
     *                Returns 400 on invalid input.
     *                Returns 409 if the username already exists.
     *                Returns 201 on successful creation.
     *                Returns 500 on SQL error.
     */
    public void createUser(RoutingContext context) {
        JsonObject requestBody = context.body().asJsonObject();

        String username = requestBody.getString("username");
        String password = requestBody.getString("password");
        String role = requestBody.getString("role");

        if (username == null || password == null || role == null) {
            context.response()
                    .setStatusCode(400)
                    .end(new JsonObject().put("message", "Invalid input").encode());
            return;
        }

        try {
            String checkUserSql = "SELECT id FROM users WHERE username = ?";
            try (PreparedStatement checkUser = conn.prepareStatement(checkUserSql)) {
                checkUser.setString(1, username);
                ResultSet resultSet = checkUser.executeQuery();
                if (resultSet.next()) {
                    context.response()
                            .setStatusCode(409)
                            .end(new JsonObject().put("message", "Username already exists").encode());
                    return;
                }
            }

            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
            String insertUserSql = "INSERT INTO users (username, password) VALUES (?, ?)";
            String insertRoleSql = "INSERT INTO userrole (userid, roleid) VALUES ((SELECT id FROM users WHERE username = ?), (SELECT id FROM roles WHERE role = ?))";

            try (PreparedStatement userCreation = conn.prepareStatement(insertUserSql);
                    PreparedStatement roleAssignment = conn.prepareStatement(insertRoleSql)) {

                userCreation.setString(1, username);
                userCreation.setString(2, hashedPassword);

                int rowsInsertedInUserCreation = userCreation.executeUpdate();

                roleAssignment.setString(1, username);
                roleAssignment.setString(2, role);
                int rowsInsertedInRoleAssignment = roleAssignment.executeUpdate();

                if (rowsInsertedInUserCreation > 0 && rowsInsertedInRoleAssignment > 0) {
                    context.response()
                            .setStatusCode(201)
                            .end(new JsonObject().put("message", "User created successfully").encode());
                } else {
                    context.response()
                            .setStatusCode(409)
                            .end(new JsonObject().put("message", "User creation failed").encode());
                }
            }
        } catch (SQLException e) {
            context.response()
                    .setStatusCode(500)
                    .end(new JsonObject().put("message", "Internal Server Error: " + e.getMessage()).encode());
        }
    }

    /**
     * Updates an existing user.
     *
     * @param context The routing context containing the request body and
     *                parameters.
     *                Requires "id" path parameter.
     *                Requires "username" and "password" in the body.
     *                Returns 200 on successful update.
     *                Returns 404 if the user is not found.
     *                Returns 500 on SQL error.
     */
    public void updateUser(RoutingContext context) {
        int userId = Integer.parseInt(context.request().getParam("id"));
        JsonObject updateBody = context.body().asJsonObject();
        String newUsername = updateBody.getString("username");
        String newPassword = updateBody.getString("password");

        String updateSql = "UPDATE users SET username = ?, password = ? WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
            pstmt.setString(1, newUsername);
            pstmt.setString(2, BCrypt.hashpw(newPassword, BCrypt.gensalt()));
            pstmt.setInt(3, userId);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                context.response().setStatusCode(200).end(new JsonObject().put("message", "User updated").encode());
            } else {
                context.response().setStatusCode(404).end(new JsonObject().put("message", "User not found").encode());
            }
        } catch (SQLException e) {
            context.response().setStatusCode(500)
                    .end(new JsonObject().put("message", "Internal Server Error: " + e.getMessage()).encode());
        }
    }

    /**
     * Deletes a user by their ID.
     *
     * @param context The routing context containing the request parameters.
     *                Requires "id" path parameter.
     *                Returns 200 on successful deletion.
     *                Returns 404 if the user is not found.
     *                Returns 500 on error.
     */
    public void deleteUser(RoutingContext context) {
        String userId = context.pathParam("id");
        try {
            PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE id = ?");
            ps.setString(1, userId);
            int deleted = ps.executeUpdate();

            if (deleted > 0) {
                context.response().setStatusCode(200).end("user deleted");
            } else {
                context.response().setStatusCode(404).end(new JsonObject().put("error", "User not found").encode());
            }
            ps.close();
        } catch (SQLException e) {
            System.err.println("SQL Error: " + e.getMessage());
            context.response().setStatusCode(500).end(new JsonObject().put("error", "Internal Server Error").encode());
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            context.response().setStatusCode(500).end(new JsonObject().put("error", "Internal Server Error").encode());
        }
    }

    /**
     * Retrieves users with pagination. Admin access required.
     *
     * @param context The routing context containing query parameters.
     *                Requires "page" and "limit" query parameters.
     *                Returns 401 if not logged in or not an Admin.
     *                Returns 400 on invalid page or limit.
     *                Returns 200 with users lists and total pages.
     *                Returns 500 on error.
     */
    public void getUsersWithPagination(RoutingContext context) {
        Integer userId = context.session().get("id");
        if (userId == null || userId == 0) {
            context.response()
                    .setStatusCode(401)
                    .end(new JsonObject().put("message", "You must be logged in").encode());
            return;
        }

        try (PreparedStatement roleps = conn.prepareStatement(
                "SELECT role FROM roles WHERE id = (SELECT roleid FROM userrole WHERE userid = ?)")) {
            roleps.setInt(1, userId);

            ResultSet roleres = roleps.executeQuery();
            if (roleres.next() && "Admin".equals(roleres.getString("role"))) {
                HttpServerRequest request = context.request();
                int page;
                int limit;

                try {
                    page = Integer.parseInt(request.getParam("page"));
                    limit = Integer.parseInt(request.getParam("limit"));
                    if (page < 1 || limit < 1) {
                        throw new NumberFormatException("Page and limit must be positive integers.");
                    }
                } catch (NumberFormatException e) {
                    context.response()
                            .setStatusCode(400)
                            .end(new JsonObject().put("message", "Invalid page or limit").encode());
                    return;
                }

                int offset = (page - 1) * limit;

                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT u.id, u.username, r.role FROM users u " +
                                "JOIN userrole ur ON u.id = ur.userid " +
                                "JOIN roles r ON ur.roleid = r.id " +
                                "ORDER BY u.id LIMIT ? OFFSET ?")) {
                    ps.setInt(1, limit);
                    ps.setInt(2, offset);

                    ResultSet res = ps.executeQuery();
                    JsonArray responseArray = new JsonArray();

                    while (res.next()) {
                        JsonObject user = new JsonObject()
                                .put("id", res.getInt("id"))
                                .put("username", res.getString("username"))
                                .put("role", res.getString("role"));
                        responseArray.add(user);
                    }

                    try (PreparedStatement countPs = conn.prepareStatement("SELECT COUNT(*) AS total FROM users")) {
                        ResultSet countRes = countPs.executeQuery();
                        if (countRes.next()) {
                            int totalUsers = countRes.getInt("total");
                            int totalPages = (int) Math.ceil((double) totalUsers / limit);

                            JsonObject responseJson = new JsonObject()
                                    .put("users", responseArray)
                                    .put("totalPages", totalPages);

                            context.response()
                                    .putHeader("content-type", "application/json")
                                    .end(responseJson.encodePrettily());
                        } else {
                            context.response()
                                    .setStatusCode(500)
                                    .end(new JsonObject().put("message", "Failed to count total users").encode());
                        }
                    }

                } catch (SQLException e) {
                    e.printStackTrace();
                    context.response()
                            .setStatusCode(500)
                            .end(new JsonObject().put("message", "Database query error").encode());
                }
            } else {
                context.response()
                        .setStatusCode(401)
                        .end(new JsonObject().put("message", "You are not an admin").encode());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            context.response()
                    .setStatusCode(500)
                    .end(new JsonObject().put("message", "Database connection error").encode());
        }
    }
}
