package de.thm.mni.pi2.adder.handler;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Handler class for managing Image-related operations.
 * Handles uploading, retrieving, updating, and deleting images, as well as
 * managing images within albums.
 */
public class ImageHandler {

    private final Connection conn;

    public ImageHandler(Connection conn) {
        this.conn = conn;
    }

    /**
     * Retrieves a specific image by its ID.
     *
     * @param context The routing context containing the request parameters.
     *                Requires "id" path parameter.
     *                Returns 401 if the user is not logged in.
     *                Returns 200 with image details on success.
     *                Returns 404 if the image is not found.
     *                Returns 500 on error.
     */
    public void getImage(RoutingContext context) {
        Integer id = context.session().get("id");
        if (id == null || id == 0) {
            context.response().setStatusCode(401)
                    .end(new JsonObject().put("message", "You must be logged in").encode());
            return;
        }

        HttpServerRequest requestBody = context.request();
        String userid = String.valueOf(id);
        String imageid = requestBody.getParam("id");

        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM users u JOIN images i ON u.id = i.userid JOIN imagetags it ON i.id = it.imageid WHERE u.id = ? AND i.id = ?");
            ps.setString(1, userid);
            ps.setString(2, imageid);

            ResultSet res = ps.executeQuery();

            JsonObject image = new JsonObject();
            JsonArray tags = new JsonArray();
            boolean imageFound = false;
            while (res.next()) {
                if (!imageFound) {
                    image.put("id", res.getString("id"));
                    image.put("title", res.getString("title"));
                    image.put("date", res.getString("date"));
                    image.put("path", res.getString("path"));
                    imageFound = true;
                }

                String tag = res.getString("tag");
                if (tag != null) {
                    tags.add(tag);
                }
            }

            if (imageFound) {
                image.put("tags", tags);
                context.response().setStatusCode(200).end(image.encodePrettily());
            } else {
                context.response().setStatusCode(404).end("No Image");
            }

        } catch (Exception e) {
            context.response().setStatusCode(500).end(e.getMessage());
        }
    }

    /**
     * Retrieves all images for the logged-in user.
     *
     * @param context The routing context.
     *                Returns 401 if the user is not logged in.
     *                Returns 200 with a JSON array of images on success.
     *                Returns 500 on error.
     */
    public void getImages(RoutingContext context) {
        Integer id = context.session().get("id");
        if (id == null || id == 0) {
            context.response().setStatusCode(401)
                    .end(new JsonObject().put("message", "You must be logged in").encode());
            return;
        }

        String userid = String.valueOf(id);

        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT i.id, i.title, i.date, i.path, it.tag " +
                            "FROM users u " +
                            "JOIN images i ON u.id = i.userid " +
                            "LEFT JOIN imagetags it ON i.id = it.imageid " +
                            "WHERE u.id = ?");
            ps.setString(1, userid);
            ResultSet res = ps.executeQuery();

            JsonArray respond = new JsonArray();
            Map<String, JsonObject> imageMap = new HashMap<>();

            while (res.next()) {
                String imageId = res.getString("id");

                JsonObject images;
                if (imageMap.containsKey(imageId)) {
                    images = imageMap.get(imageId);
                } else {
                    images = new JsonObject();
                    images.put("id", imageId);
                    images.put("title", res.getString("title"));
                    images.put("date", res.getString("date"));
                    images.put("path", res.getString("path"));
                    images.put("tags", new JsonArray());
                    imageMap.put(imageId, images);
                }

                String tag = res.getString("tag");
                if (tag != null) {
                    images.getJsonArray("tags").add(tag);
                }
            }

            for (JsonObject image : imageMap.values()) {
                respond.add(image);
            }

            context.response().setStatusCode(200).end(respond.encodePrettily());

        } catch (Exception e) {
            context.response().setStatusCode(500).end(e.getMessage());
        }
    }

    /**
     * Adds a new image to the database.
     *
     * @param context The routing context containing the request body.
     *                Requires JSON body with "title", "date", "path", and optional
     *                "tags".
     *                Returns 401 if the user is not logged in.
     *                Returns 200 on successful addition.
     *                Returns 409 if addition fails.
     *                Returns 500 on SQL error.
     */
    public void addImage(RoutingContext context) {
        Integer id = context.session().get("id");
        if (id == null || id == 0) {
            context.response().setStatusCode(401)
                    .end(new JsonObject().put("message", "You must be logged in").encode());
            return;
        }

        JsonObject requestBody = context.body().asJsonObject();

        String title = requestBody.getString("title");
        String date = requestBody.getString("date");
        String path = requestBody.getString("path");
        JsonArray tagsArray = requestBody.getJsonArray("tags");

        try {
            PreparedStatement imagesps = conn
                    .prepareStatement("INSERT INTO images (userid, title, date, path) VALUES (?, ?, ?, ?)");
            imagesps.setString(1, String.valueOf(id));
            imagesps.setString(2, title);
            imagesps.setString(3, date);
            imagesps.setString(4, path);

            if (tagsArray != null) {
                StringBuilder tagsInsertionQuery = new StringBuilder(
                        "INSERT INTO imagetags (imageid, tag) VALUES ((SELECT id FROM images WHERE path = ?), ?)");
                for (int i = 1; i < tagsArray.size(); i++) {
                    tagsInsertionQuery.append(", ((SELECT id FROM images WHERE path = ?), ?)");
                }
                PreparedStatement tagsps = conn.prepareStatement(tagsInsertionQuery.toString());
                int parameterIndex = 1;

                for (int i = 0; i < tagsArray.size(); i++) {
                    tagsps.setString(parameterIndex++, path);
                    tagsps.setString(parameterIndex++, tagsArray.getString(i));
                }

                int rowsInsertedForImages = imagesps.executeUpdate();
                int rowsInsertedForTags = tagsps.executeUpdate();
                if (rowsInsertedForImages > 0 && rowsInsertedForTags > 0) {
                    context.response().setStatusCode(200).end("Image Add Success");
                } else {
                    context.response().setStatusCode(409).end("Image Add Fail");
                }

            } else {
                int rowsInsertedForImages = imagesps.executeUpdate();
                if (rowsInsertedForImages > 0) {
                    context.response().setStatusCode(200).end("Image creation Success");
                } else {
                    context.response().setStatusCode(409).end("Image creation Fail");
                }
            }

        } catch (SQLException e) {
            context.response().setStatusCode(500).end(e.getMessage());
        }
    }

    /**
     * Updates an existing image details and tags.
     *
     * @param context The routing context containing the request body.
     *                Requires JSON body with "id", "title", "date", and "tags".
     *                Returns 401 if the user is not logged in.
     *                Returns 200 on successful update.
     *                Returns 409 if update fails.
     *                Returns 500 on SQL error.
     */
    public void updateImage(RoutingContext context) {
        Integer id = context.session().get("id");
        if (id == null || id == 0) {
            context.response().setStatusCode(401)
                    .end(new JsonObject().put("message", "You must be logged in").encode());
            return;
        }

        JsonObject jsonBody = context.body().asJsonObject();

        String imageid = jsonBody.getString("id");
        String title = jsonBody.getString("title");
        String date = jsonBody.getString("date");
        JsonArray newTags = jsonBody.getJsonArray("tags");

        try {
            PreparedStatement ps = conn.prepareStatement("UPDATE images SET title = ?, date = ? WHERE id = ?");
            ps.setString(1, title);
            ps.setString(2, date);
            ps.setString(3, imageid);
            int rowsUpdated = ps.executeUpdate();

            if (rowsUpdated > 0) {
                ps = conn.prepareStatement("DELETE FROM imagetags WHERE imageid = ?");
                ps.setString(1, imageid);
                ps.executeUpdate();

                ps = conn.prepareStatement("INSERT INTO imagetags (imageid, tag) VALUES (?, ?)");
                for (int i = 0; i < newTags.size(); i++) {
                    ps.setString(1, imageid);
                    ps.setString(2, newTags.getString(i));
                    ps.addBatch();
                }
                ps.executeBatch();

                context.response().setStatusCode(200).end("Image and tags update Success");
            } else {
                context.response().setStatusCode(409).end("Image update Fail");
            }
        } catch (SQLException e) {
            context.response().setStatusCode(500).end(e.getMessage());
        }
    }

    /**
     * Deletes an image by its ID.
     *
     * @param context The routing context containing the request parameters.
     *                Requires "id" path parameter.
     *                Returns 401 if the user is not logged in.
     *                Returns 200 on successful deletion.
     *                Returns 404 if the image is not found.
     *                Returns 500 on error.
     */
    public void deleteImage(RoutingContext context) {
        Integer id = context.session().get("id");
        if (id == null || id == 0) {
            context.response().setStatusCode(401)
                    .end(new JsonObject().put("message", "You must be logged in").encode());
            return;
        }

        HttpServerRequest requestBody = context.request();
        String imageid = requestBody.getParam("id");

        try {
            PreparedStatement ps = conn.prepareStatement("DELETE FROM images WHERE id = ?");
            ps.setString(1, imageid);

            int deleted = ps.executeUpdate();
            if (deleted > 0) {
                context.response().setStatusCode(200).end("image deleted");
            } else {
                context.response().setStatusCode(404).end(new JsonObject().put("error", "image not found").encode());
            }
        } catch (Exception e) {
            context.response().setStatusCode(500).end(e.getMessage());
        }
    }

    /**
     * Retrieves all images associated with a specific album.
     *
     * @param context The routing context containing the request parameters.
     *                Requires "id" (album id) path parameter.
     *                Returns 401 if the user is not logged in.
     *                Returns 200 with a JSON array of images on success.
     *                Returns 500 on error.
     */
    public void getImagesfromAlbum(RoutingContext context) {
        Integer id = context.session().get("id");
        if (id == null || id == 0) {
            context.response().setStatusCode(401)
                    .end(new JsonObject().put("message", "You must be logged in").encode());
            return;
        }

        HttpServerRequest requestBody = context.request();
        String albumid = requestBody.getParam("id");

        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT i.id, i.title, i.date, i.path FROM images i JOIN albumimages ai ON i.id = ai.imageid JOIN albums a ON ai.albumid = a.id WHERE a.id = ?");
            ps.setInt(1, Integer.parseInt(albumid));
            ResultSet res = ps.executeQuery();

            JsonArray respond = new JsonArray();
            while (res.next()) {
                JsonObject images = new JsonObject();
                images.put("id", res.getString("id"));
                images.put("title", res.getString("title"));
                images.put("date", res.getString("date"));
                images.put("path", res.getString("path"));
                respond.add(images);
            }

            context.response().setStatusCode(200).end(respond.encodePrettily());

        } catch (Exception e) {
            context.response().setStatusCode(500).end(e.getMessage());
        }
    }

    /**
     * Updates image details within an album context.
     *
     * @param context The routing context containing the request parameters.
     *                Requires parameters: "albumid", "imageid", "title", "date",
     *                "path".
     *                Returns 401 if the user is not logged in.
     *                Returns 200 on successful update.
     *                Returns 409 if update fails.
     *                Returns 500 on SQL error.
     */
    public void updateImageinAlbum(RoutingContext context) {
        Integer id = context.session().get("id");
        if (id == null || id == 0) {
            context.response().setStatusCode(401)
                    .end(new JsonObject().put("message", "You must be logged in").encode());
            return;
        }

        HttpServerRequest requestBody = context.request();

        String albumid = requestBody.getParam("albumid");
        String imageid = requestBody.getParam("imageid");
        String title = requestBody.getParam("title");
        String date = requestBody.getParam("date");
        String path = requestBody.getParam("path");

        try {
            PreparedStatement ps = conn
                    .prepareStatement(
                            "UPDATE albumimages SET title = ?, date = ?, path = ? WHERE albumid = ? AND imageid = ?");

            ps.setString(1, title);
            ps.setString(2, date);
            ps.setString(3, path);
            ps.setString(4, albumid);
            ps.setString(5, imageid);

            int rowsUpdated = ps.executeUpdate();

            if (rowsUpdated > 0) {
                context.response().setStatusCode(200).end("Image update in Album Success");
            } else {
                context.response().setStatusCode(409).end("Image update in Album Fail");
            }
        } catch (SQLException e) {
            context.response().setStatusCode(500).end(e.getMessage());
        }
    }

    /**
     * Associates an existing image with an album.
     *
     * @param context The routing context containing the request body.
     *                Requires JSON body with "imageid" and "albumid".
     *                Returns 200 on success.
     *                Returns 409 on failure.
     *                Returns 500 on SQL error.
     */
    public void addImagetoAlbum(RoutingContext context) {
        JsonObject requestBody = context.body().asJsonObject();

        String imageid = requestBody.getString("imageid");
        String albumid = requestBody.getString("albumid");

        try {
            PreparedStatement ps = conn.prepareStatement("INSERT INTO albumimages (albumid, imageid) VALUES (?,?)");

            ps.setString(1, albumid);
            ps.setString(2, imageid);

            int rowsInserted = ps.executeUpdate();

            if (rowsInserted > 0) {
                context.response().setStatusCode(200).end("Image creation to album Success");
            } else {
                context.response().setStatusCode(409).end("Image creation to album Fail");
            }
        } catch (SQLException e) {
            context.response().setStatusCode(500).end(e.getMessage());
        }
    }

    /**
     * Removes an image from an album.
     *
     * @param context The routing context containing the path parameters.
     *                Requires "albumid" and "imageid" path parameters.
     *                Returns 401 if the user is not logged in.
     *                Returns 400 if IDs are missing.
     *                Returns 200 on successful deletion.
     *                Returns 404 if image not found in album.
     *                Returns 500 on error.
     */
    public void deleteImagefromAlbum(RoutingContext context) {
        int userId = context.session().get("id");

        if (userId == 0) {
            context.response().setStatusCode(401)
                    .end(new JsonObject().put("message", "You must be logged in").encode());
            return;
        }

        String albumId = context.pathParam("albumid");
        String imageId = context.pathParam("imageid");

        if (albumId == null || imageId == null) {
            context.response().setStatusCode(400)
                    .end(new JsonObject().put("error", "Album ID and Image ID are required").encode());
            return;
        }

        try {
            PreparedStatement ps = conn.prepareStatement("DELETE FROM albumimages WHERE albumid = ? AND imageid = ?");
            ps.setString(1, albumId);
            ps.setString(2, imageId);

            int deleted = ps.executeUpdate();
            if (deleted > 0) {
                context.response().setStatusCode(200)
                        .end(new JsonObject().put("message", "Image deleted from album").encode());
            } else {
                context.response().setStatusCode(404)
                        .end(new JsonObject().put("error", "Image not found in album").encode());
            }
        } catch (Exception e) {
            context.response().setStatusCode(500).end(new JsonObject().put("error", "Internal Server Error").encode());
        }
    }
}
