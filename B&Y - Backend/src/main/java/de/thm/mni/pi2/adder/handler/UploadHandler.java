package de.thm.mni.pi2.adder.handler;

import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.nio.file.Paths;
import java.util.UUID;

/**
 * Handler class for file uploads.
 * Handles uploading files to the server.
 */
public class UploadHandler {

    private final Vertx vertx;

    public UploadHandler(Vertx vertx) {
        this.vertx = vertx;
    }

    /**
     * Handles file uploads.
     * Moves uploaded files from temporary storage to the uploads directory.
     * Renames files with a GUID to avoid collisions.
     *
     * @param context The routing context containing the file uploads.
     *                Returns 401 if the user is not logged in.
     *                Returns 200 with the new filename on success.
     *                Returns 500 on upload failure.
     */
    public void upload(RoutingContext context) {
        Integer id = context.session().get("id");
        if (id == null || id == 0) {
            context.response().setStatusCode(401)
                    .end(new JsonObject().put("message", "You must be logged in").encode());
            return;
        }

        FileSystem fs = vertx.fileSystem();
        try {
            context.fileUploads().forEach(fileUpload -> {

                String originalName = fileUpload.fileName();
                String tempFileDest = fileUpload.uploadedFileName();
                String extension = "";
                int lastDotIndex = originalName.lastIndexOf('.');
                if (lastDotIndex != -1) {
                    extension = originalName.substring(lastDotIndex);
                }
                String randomName = UUID.randomUUID() + extension;
                String target = Paths.get("uploads", randomName).toString();

                fs.move(tempFileDest, target, moveRes -> {
                    if (moveRes.succeeded()) {
                        context.response().setStatusCode(200)
                                .end(new JsonObject().put("message", "File Uploaded").put("filename", randomName)
                                        .encode());
                    } else {
                        context.response().setStatusCode(500).end("upload Fail.");
                    }
                });
            });
        } catch (Exception e) {
            // Log error if needed, avoiding console prints as per previous task
        }
    }
}
