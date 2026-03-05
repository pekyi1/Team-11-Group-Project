package com.amalitech.qa;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.testng.annotations.*;
import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

public class ServiceRequestApiTest {
    private String authToken;

    @BeforeClass
    public void setup() {
        RestAssured.baseURI = "http://localhost:8080";
        authToken = given()
            .contentType(ContentType.JSON)
            .body("{\"email\":\"admin@amalitech.com\",\"password\":\"password123\"}")
        .when().post("/api/auth/login").then().extract().path("token");
    }

    @Test
    public void testGetAllRequests() {
        given().header("Authorization", "Bearer " + authToken)
        .when().get("/api/requests")
        .then().statusCode(200);
    }

    @Test
    public void testCreateServiceRequest() {
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + authToken)
            .body("{\"title\":\"Laptop not working\",\"description\":\"Screen is blank\",\"category\":\"IT_SUPPORT\",\"priority\":\"HIGH\",\"departmentId\":1}")
        .when().post("/api/requests")
        .then().statusCode(200).body("title", equalTo("Laptop not working"));
    }

    // TODO: testUpdateRequestStatus
    // TODO: testGetDepartments
    // TODO: testUnauthorizedAccess
}
