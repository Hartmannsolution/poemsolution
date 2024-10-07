package rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.cphbusiness.persistence.HibernateConfig;
import dk.cphbusiness.persistence.entities.IJPAEntity;
import dk.cphbusiness.persistence.entities.Poem;
import dk.cphbusiness.rest.ApplicationConfig;
import dk.cphbusiness.rest.RestRoutes;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RestassuredTest {

    private static ApplicationConfig appConfig;
    private static EntityManagerFactory emfTest;
    private static ObjectMapper jsonMapper = new ObjectMapper();
    Map<String, IJPAEntity> populated;

    @BeforeAll
    static void setUpAll() {
        RestAssured.baseURI = "http://localhost:7777/api";

        HibernateConfig.setTestMode(true); // IMPORTANT leave this at the very top of this method in order to use the test database
        RestRoutes restRoutes = new RestRoutes();

        // Setup test database using docker testcontainers
        emfTest = HibernateConfig.getEntityManagerFactoryForTest();

        // Start server
        appConfig = ApplicationConfig.
                getInstance()
                .initiateServer()
//                .checkSecurityRoles()
//                .setErrorHandling()
//                .setGeneralExceptionHandling()
//                .setRoutes(SecurityRoutes.getSecurityRoutes())
//                .setRoutes(SecurityRoutes.getSecuredRoutes())
                .setRoutes(restRoutes.getPoemRoutes()) // A different way to get the EndpointGroup. Getting data from DB
                .setApi404ExceptionHandling()
//                .setCORS()
//                .setApiExceptionHandling()
                .startServer(7777);
    }

    @AfterAll
    static void afterAll() {
        HibernateConfig.setTestMode(false);
        appConfig.stopServer();
    }

    @BeforeEach
    void setUpEach() {
        // Setup test database for each test
        new TestUtils().createUsersAndRoles(emfTest);
        // Setup DB Persons and Addresses
        populated = new TestUtils().createPoemsInDB(emfTest);
        populated.values().forEach(System.out::println);
    }

    @Test
    void testGetAllPoems() {
        given()
                .when()
                .get("/poem")
                .then()
                .statusCode(200)
                .body("size()", equalTo(2));
    }

    @Test
    @DisplayName("Test poem by ID")
    void testGetPoemByID() {
        given()
//                .log().all()
                .when()
                .get("/poem/" + populated.get("poem1").getId())
                .then()
//                .log().all()
                .statusCode(200)
                .body("title", equalTo("Roses"));
    }

    @Test
    @DisplayName("Test non-existing poem by ID")
    void testGetNonExistingPoemByID() {
        given()
                .when()
                .get("/poem/100")
                .then()
                .statusCode(404)
                .body("msg", equalTo("Poem with id: " + 100 + " not found"));
    }

    @Test
    @DisplayName("Test create poem")
    void testCreatePoem() {
        ObjectNode node = jsonMapper.createObjectNode();
        node.put("title", "Test title");
        node.put("author", "Test author");
        node.put("text", "poem text");
        String json = node.toString();
        given()
                .header("Content-Type", "application/json")
                .accept("application/json")
                .body(json)
                .when()
                .post("/poem")
                .then()
                .statusCode(201)
                .body("title", equalTo("Test title"));
    }

    @Test
    @DisplayName("Test update poem")
    void testUpdate() {
        ObjectMapper mapper = new ObjectMapper();

        Poem poem = new Poem("Dandelions", "Benjamin Lollipop", "Dandelions are white, violets are blue, sugar is sweet, and so are you");
        String jsonPoem = null;
        try {
            jsonPoem = mapper.writeValueAsString(poem);
        } catch (JsonProcessingException ex) {
            ex.printStackTrace();
        }
        given()
                .header("Content-Type", "application/json")
                .accept("application/json")
                .body(jsonPoem)
                .when()
                .put("/poem/"+populated.get("poem2").getId())
                .then()
                .statusCode(200)
                .body("title", equalTo("Dandelions"));
    }

    @Test
    @DisplayName("Test delete poem")
    void testDelete() {
        given()
                .when()
                .delete("/poem/"+populated.get("poem2").getId())
                .then()
                .statusCode(200);
//        given().when().get("/poem").then().body("size()", equalTo(1));
        given().when()
                .get("/poem/"+populated.get("poem2").getId())
                .then().statusCode(404);
    }

    @Test
    @DisplayName("Test JsonPath")
    void testJsonPath() {
        Response response = given().get("/poem");
        List<String> titles = response.jsonPath().get("title");
        System.out.println("Title: " + titles);
        assertTrue(titles.contains("Roses"));
    }
}
