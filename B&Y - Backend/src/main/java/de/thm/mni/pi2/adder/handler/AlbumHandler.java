package de.thm.mni.pi2.adder.handler;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Handler class for managing Album-related operations.
 * Handles creating, retrieving, updating, and deleting albums.
 */
public class AlbumHandler {

    private final Connection conn;

    public AlbumHandler(Connection conn) {
        this.conn = conn;
    }

    /**
     * Retrieves all albums for the logged-in user.
     *
     * @param context The routing context containing the request and response
     *                objects.
     *                Finds the user ID from the session.
     *                Returns 401 if the user is not logged in.
     *                Returns 200 with a JSON array of albums on success.
     *                Returns 500 on internal server error.
     */
    public void getAlbums(RoutingContext context) {
        Integer sessionId = context.session().get("id");

        if (sessionId == null || sessionId == 0) {
            context.response().setStatusCode(401)
                    .end(new JsonObject().put("message", "You must be logged in").encode());
            return;
        }

        String userid = sessionId.toString();

        try {
            String query = "SELECT a.id, a.title, GROUP_CONCAT(at.tag SEPARATOR ',') as tags " +
                    "FROM albums a " +
                    "LEFT JOIN albumtags at ON a.id = at.albumid " +
                    "WHERE a.userid = ? " +
                    "GROUP BY a.id";

            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, userid);
            ResultSet res = ps.executeQuery();

            JsonArray respond = new JsonArray();

            while (res.next()) {
                JsonObject album = new JsonObject();
                album.put("id", res.getString("id"));
                album.put("title", res.getString("title"));

                String tagsString = res.getString("tags");
                JsonArray tagsArray = new JsonArray();
                if (tagsString != null && !tagsString.isEmpty()) {
                    for (String tag : tagsString.split(",")) {
                        tagsArray.add(tag.trim());
                    }
                }
                album.put("tags", tagsArray);

                respond.add(album);
            }

            context.response().setStatusCode(200).end(respond.encodePrettily());

        } catch (Exception e) {
            System.err.println("Exception occurred: " + e.getMessage());
            context.response().setStatusCode(500).end(e.getMessage());
        }
    }

    /**
     * Retrieves a specific album by its ID.
     *
     * @param context The routing context containing the request parameters.
     *                Requires "id" as a path parameter.
     *                Returns 401 if the user is not logged in.
     *                Returns 400 if the album ID is missing.
     *                Returns 200 with the album details on success.
     *                Returns 404 if the album is not found.
     *                Returns 500 on internal server error.
     */
    public void getAlbumById(RoutingContext context) {
        Integer sessionId = context.session().get("id");

        if (sessionId == null || sessionId == 0) {
            context.response().setStatusCode(401)
                    .end(new JsonObject().put("message", "You must be logged in").encode());
            return;
        }

        String albumId = context.request().getParam("id");

        if (albumId == null) {
            context.response().setStatusCode(400).end(new JsonObject().put("message", "Album ID is required").encode());
            return;
        }

        try {
            String query = "SELECT albums.id, albums.title, GROUP_CONCAT(tags.tag SEPARATOR ', ') as tags " +
                    "FROM albums " +
                    "LEFT JOIN albumtags tags ON albums.id = tags.albumid " +
                    "WHERE albums.id = ? AND albums.userid = ? " +
                    "GROUP BY albums.id";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, albumId);
            ps.setString(2, sessionId.toString());
            ResultSet res = ps.executeQuery();

            if (res.next()) {
                JsonObject album = new JsonObject();
                album.put("id", res.getString("id"));
                album.put("title", res.getString("title"));
                album.put("tags", res.getString("tags"));

                context.response().setStatusCode(200).end(album.encodePrettily());
            } else {
                context.response().setStatusCode(404).end(new JsonObject().put("message", "Album not found").encode());
            }
        } catch (SQLException e) {
            System.err.println("SQL Exception occurred: " + e.getMessage());
            context.response().setStatusCode(500)
                    .end(new JsonObject().put("message", "Internal Server Error").put("details", e.getMessage())
                            .encode());
        } catch (Exception e) {
            System.err.println("Exception occurred: " + e.getMessage());
            context.response().setStatusCode(500)
                    .end(new JsonObject().put("message", "Internal Server Error").put("details", e.getMessage())
                            .encode());
        }
    }

    /**
     * Creates a new album for the logged-in user.
     *
     * @param context The routing context containing the request body.
     *                Requires a JSON body with a "title" field and optional "tags"
     *                field.
     *                Returns 401 if the user is not logged in.
     *                Returns 200 on successful creation.
     *                Returns 409 if creation fails.
     *                Returns 500 on SQL error.
     */
    public void createAlbum(RoutingContext context) {
        Integer userId = context.session().get("id");

        if (userId == null || userId == 0) {
            context.response()
                    .setStatusCode(401)
                    .end(new JsonObject().put("message", "You must be logged in").encode());
            return;
        }

        JsonObject body = context.body().asJsonObject();
        String title = body.getString("title");
        String tags = body.getString("tags");

        try {
            // Insert the album and get its generated ID
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO albums (userid, title) VALUES (?, ?)",
                    PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setInt(1, userId);
            ps.setString(2, title);

            int rowsInserted = ps.executeUpdate();

            if (rowsInserted > 0) {
                // Get the generated album ID
                ResultSet generatedKeys = ps.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int albumId = generatedKeys.getInt(1);

                    // Insert tags if provided
                    if (tags != null && !tags.isEmpty()) {
                        String[] tagArray = tags.split(",\\s*");
                        ps = conn.prepareStatement("INSERT INTO albumtags (albumid, tag) VALUES (?, ?)");
                        for (String tag : tagArray) {
                            if (!tag.trim().isEmpty()) {
                                ps.setInt(1, albumId);
                                ps.setString(2, tag.trim());
                                ps.addBatch();
                            }
                        }
                        ps.executeBatch();
                    }
                }
                context.response().setStatusCode(200).end("Album creation Success");
            } else {
                context.response().setStatusCode(409).end("Album creation Fail");
            }
        } catch (SQLException e) {
            context.response().setStatusCode(500).end(e.getMessage());
        }
    }

    /**
     * Updates an existing album.
     *
     * @param context The routing context containing the request body and
     *                parameters.
     *                Requires "id" path parameter.
     *                Requires a JSON body with "title" and "tags" fields.
     *                Returns 401 if the user is not logged in.
     *                Returns 200 on successful update.
     *                Returns 409 if update fails.
     *                Returns 500 on SQL error.
     */
    public void updateAlbum(RoutingContext context) {
        Integer userId = context.session().get("id");
        if (userId == null || userId == 0) {
            context.response().setStatusCode(401)
                    .end(new JsonObject().put("message", "You must be logged in").encode());
            return;
        }

        JsonObject jsonBody = context.body().asJsonObject();
        String albumId = context.request().getParam("id");
        String title = jsonBody.getString("title");
        String tags = jsonBody.getString("tags");

        try {
            PreparedStatement ps = conn.prepareStatement("UPDATE albums SET title = ? WHERE id = ?");
            ps.setString(1, title);
            ps.setString(2, albumId);
            int rowsUpdated = ps.executeUpdate();

            if (rowsUpdated > 0) {
                ps = conn.prepareStatement("DELETE FROM albumtags WHERE albumid = ?");
                ps.setString(1, albumId);
                ps.executeUpdate();

                String[] tagArray = tags.split(",\\s*");
                ps = conn.prepareStatement("INSERT INTO albumtags (albumid, tag) VALUES (?, ?)");
                for (String tag : tagArray) {
                    ps.setString(1, albumId);
                    ps.setString(2, tag.trim());
                    ps.addBatch();
                }
                ps.executeBatch();

                context.response().setStatusCode(200).end("Album update Success");
            } else {
                context.response().setStatusCode(409).end("Album update Fail");
            }
        } catch (SQLException e) {
            context.response().setStatusCode(500).end(e.getMessage());
        }
    }

    /**
     * Deletes an album by its ID.
     *
     * @param context The routing context containing the request parameters.
     *                Requires "id" path parameter.
     *                Returns 401 if the user is not logged in.
     *                Returns 400 if the album ID is missing.
     *                Returns 200 on successful deletion.
     *                Returns 404 if the album is not found.
     *                Returns 500 on error.
     */
    public void deleteAlbum(RoutingContext context) {
        Integer id = context.session().get("id");
        if (id == null || id == 0) {
            context.response().setStatusCode(401)
                    .end(new JsonObject().put("message", "You must be logged in").encode());
            return;
        }

        HttpServerRequest requestBody = context.request();
        String albumId = requestBody.getParam("id");
        if (albumId == null) {
            context.response().setStatusCode(400).end(new JsonObject().put("error", "Album ID is required").encode());
            return;
        }

        try {
            PreparedStatement ps = conn.prepareStatement("DELETE FROM albums WHERE id = ?");
            ps.setString(1, albumId);
            int deleted = ps.executeUpdate();
            if (deleted > 0) {
                context.response().setStatusCode(200).end(new JsonObject().put("message", "Album deleted").encode());
            } else {
                context.response().setStatusCode(404).end(new JsonObject().put("error", "Album not found").encode());
            }
        } catch (SQLException e) {
            System.err.println("SQL error: " + e.getMessage());
            context.response().setStatusCode(500).end(new JsonObject().put("error", e.getMessage()).encode());
        } catch (Exception e) {
            System.err.println("General error: " + e.getMessage());
            context.response().setStatusCode(500).end(new JsonObject().put("error", e.getMessage()).encode());
        }
    }
}
