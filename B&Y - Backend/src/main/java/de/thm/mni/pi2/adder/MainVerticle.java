package de.thm.mni.pi2.adder;

import de.thm.mni.pi2.adder.handler.AlbumHandler;
import de.thm.mni.pi2.adder.handler.AuthHandler;
import de.thm.mni.pi2.adder.handler.ImageHandler;
import de.thm.mni.pi2.adder.handler.UploadHandler;
import de.thm.mni.pi2.adder.handler.UserHandler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Main entry point for the Adder application.
 * This Verticle initializes the HTTP server, sets up the router, and configures
 * handlers for various endpoints including user management, authentication,
 * albums, images, and uploads.
 */
public class MainVerticle extends AbstractVerticle {

  private Connection conn;

  /**
   * Starts the Verticle.
   * Establishes a database connection using environment variables (DB_HOST,
   * DB_PORT, DB_NAME, DB_USER, DB_PASS)
   * or default values (localhost:3306/fotolab, root, empty password).
   * Configures the router with session, body, and CORS handlers, defines routes
   * for all API endpoints,
   * and starts the HTTP server on port 8888.
   */
  @Override
  public void start() {

    try {
      String dbHost = System.getenv().getOrDefault("DB_HOST", "localhost");
      String dbPort = System.getenv().getOrDefault("DB_PORT", "3306");
      String dbName = System.getenv().getOrDefault("DB_NAME", "fotolab");
      String dbUser = System.getenv().getOrDefault("DB_USER", "root");
      String dbPass = System.getenv().getOrDefault("DB_PASS", "");

      Connection connection = DriverManager.getConnection(
          String.format("jdbc:mariadb://%s:%s/%s", dbHost, dbPort, dbName),
          dbUser,
          dbPass);
      conn = connection;
      Router router = Router.router(vertx);

      // Initialize Handlers
      AuthHandler authHandler = new AuthHandler(conn);
      UserHandler userHandler = new UserHandler(conn);
      AlbumHandler albumHandler = new AlbumHandler(conn);
      ImageHandler imageHandler = new ImageHandler(conn);
      UploadHandler uploadHandler = new UploadHandler(vertx);

      // Session-Handler hinzufügen
      router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));
      // Body-Handler mit Upload-Verzeichnis-Konfiguration
      router.route().handler(BodyHandler.create().setUploadsDirectory("uploads"));
      // CORS-Handler hinzufügen
      router.route().handler(CorsHandler.create()
          .addOrigin("http://localhost:3000")
          .addOrigin("http://localhost:8080")
          .allowedMethod(HttpMethod.GET)
          .allowedMethod(HttpMethod.POST)
          .allowedMethod(HttpMethod.DELETE)
          .allowedMethod(HttpMethod.PUT)
          .allowedHeader("Access-Control-Allow-Origin")
          .allowedHeader("Access-Control-Allow-Headers")
          .allowedHeader("Access-Control-Allow-Methods")
          .allowedHeader("Content-Type")
          .allowedHeader("Accept")
          .allowCredentials(true));

      // Users Management
      router.get("/users").handler(userHandler::getAllUsers);
      router.get("/users/p").handler(userHandler::getUsersWithPagination);
      router.get("/user").handler(userHandler::getUser);
      router.post("/users").handler(userHandler::createUser);
      router.put("/users/:id").handler(userHandler::updateUser);
      router.delete("/users/:id").handler(userHandler::deleteUser);

      router.post("/upload").handler(uploadHandler::upload);

      // Authentication
      router.post("/login").handler(authHandler::login);
      router.post("/logout").handler(authHandler::logout);
      router.get("/userroles").handler(authHandler::getUserRoles);

      // Albums Management
      router.get("/user/albums").handler(albumHandler::getAlbums);
      router.get("/user/albums/:id").handler(albumHandler::getAlbumById);
      router.post("/albums").handler(albumHandler::createAlbum);
      router.put("/albums/:id").handler(albumHandler::updateAlbum);
      router.delete("/albums/:id").handler(albumHandler::deleteAlbum);

      // Images Management
      router.get("/user/images/:id").handler(imageHandler::getImage);
      router.get("/user/images").handler(imageHandler::getImages);
      router.post("/images").handler(imageHandler::addImage);
      router.put("/images").handler(imageHandler::updateImage);
      router.delete("/images/:id").handler(imageHandler::deleteImage);

      router.get("/albums/:id/albumimages").handler(imageHandler::getImagesfromAlbum);
      router.put("/albums/:albumid/albumimages/:imageid").handler(imageHandler::updateImageinAlbum);
      router.post("/albums/images").handler(imageHandler::addImagetoAlbum);
      router.delete("/albums/:albumid/images/:imageid").handler(imageHandler::deleteImagefromAlbum);

      // Uploads
      router.route("/uploads/*").handler(StaticHandler.create("uploads"));

      // Server starten
      vertx.createHttpServer()
          .requestHandler(router)
          .listen(8888)
          .onSuccess(server -> {
          });
    } catch (SQLException sqlex) {
      sqlex.printStackTrace();
    }

  }
}
