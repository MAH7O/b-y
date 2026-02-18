package de.thm.mni.pi2.adder.handler;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Handler class for user authentication.
 * Handles login, logout, and checking user roles.
 */
public class AuthHandler {

    private final Connection conn;

    public AuthHandler(Connection conn) {
        this.conn = conn;
    }

    /**
     * Authenticates a user.
     *
     * @param routingContext The routing context containing the request body.
     *                       Requires a JSON body with "username" and "password".
     *                       Returns 200 with user ID on successful login.
     *                       Returns 401 on invalid credentials.
     *                       Returns 500 on internal server error.
     */
    public void login(RoutingContext routingContext) {
        try {
            JsonObject requestBody = routingContext.body().asJsonObject();
            String username = requestBody.getString("username");
            String password = requestBody.getString("password");

            String sql = "SELECT id, password FROM users WHERE username = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ResultSet res = ps.executeQuery();

            if (res.next()) {
                String storedHashedPassword = res.getString("password");
                int id = res.getInt("id");

                if (BCrypt.checkpw(password, storedHashedPassword)) {
                    routingContext.session().put("id", id);
                    JsonObject responseJson = new JsonObject().put("message", "Login successful").put("id", id);
                    routingContext.response()
                            .putHeader("content-type", "application/json")
                            .setStatusCode(200)
                            .end(responseJson.encode());
                } else {
                    routingContext.response()
                            .setStatusCode(401)
                            .putHeader("content-type", "application/json")
                            .end(new JsonObject().put("error", "Wrong username or password").encode());
                }
            } else {
                routingContext.response()
                        .setStatusCode(401)
                        .putHeader("content-type", "application/json")
                        .end(new JsonObject().put("error", "Wrong username or password").encode());
            }
        } catch (Exception e) {
            routingContext.response()
                    .setStatusCode(500)
                    .putHeader("content-type", "application/json")
                    .end(new JsonObject().put("error", "Internal Server Error: " + e.getMessage()).encode());
        }
    }

    /**
     * Logs out the current user by invalidating the session.
     *
     * @param context The routing context.
     *                Returns 200 on successful logout.
     *                Returns 500 if the user was not logged in or on error.
     */
    public void logout(RoutingContext context) {
        try {
            Session session = context.session();
            if (session.get("id") != null) {
                session.destroy();
                context.response().setStatusCode(200).end("Logout Success");
            } else {
                context.response().setStatusCode(500).end("you were not logged in to logout");
            }
        } catch (Exception e) {
            context.response().setStatusCode(500).end(e.getMessage());
        }
    }

    /**
     * Retrieves the roles associated with the logged-in user.
     *
     * @param routingContext The routing context.
     *                       Returns 401 if the user is not logged in.
     *                       Returns 200 with a JSON array of roles on success.
     *                       Returns 500 on internal server error.
     */
    public void getUserRoles(RoutingContext routingContext) {
        try {
            Integer userId = routingContext.session().get("id");
            if (userId == null) {
                routingContext.response()
                        .setStatusCode(401)
                        .putHeader("content-type", "application/json")
                        .end(new JsonObject().put("error", "Unauthorized").encode());
                return;
            }

            String roleSql = "SELECT r.role FROM roles r INNER JOIN userrole ur ON r.id = ur.roleid WHERE ur.userid = ?";
            PreparedStatement rolePs = conn.prepareStatement(roleSql);
            rolePs.setInt(1, userId);
            ResultSet roleRes = rolePs.executeQuery();

            List<String> roles = new ArrayList<>();
            while (roleRes.next()) {
                roles.add(roleRes.getString("role"));
            }

            JsonObject responseJson = new JsonObject().put("roles", new JsonArray(roles));
            routingContext.response()
                    .putHeader("content-type", "application/json")
                    .setStatusCode(200)
                    .end(responseJson.encode());
        } catch (Exception e) {
            routingContext.response()
                    .setStatusCode(500)
                    .putHeader("content-type", "application/json")
                    .end(new JsonObject().put("error", "Internal Server Error: " + e.getMessage()).encode());
        }
    }
}
