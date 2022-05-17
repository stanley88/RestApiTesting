package com.stan.demo.spock

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.restassured.http.ContentType
import io.restassured.specification.RequestSpecification
import spock.lang.Specification
import io.restassured.RestAssured

class createUserSpecification extends Specification{
    RequestSpecification request
    Object createUserPayload

    def setup() {
        request = RestAssured.given()
        request.baseUri("https://gorest.co.in/public/v2/users")
        request.queryParam("access-token", "7a54eb3570edeca8e389af2183c1b5002937f18a30ce4cabe621d20afbcf802e")
        request.contentType(ContentType.JSON)

        def jsonSlurper = new JsonSlurper()
        createUserPayload = jsonSlurper.parse(getClass().getClassLoader().getResource("payloads/createUser.json"))
    }

    def "User has been successfully created"() {
        given: "proper request body"
            def requestBody = JsonOutput.toJson(createUserPayload)

        when: "POST call to createUser is made"
            def response = request.body(requestBody.toString()).post()

        then: "response status call is 201"
            response.statusCode() == 201

        expect: "user has been created"
            int userId = response.path("id")
            def createdUser = request.get("/{userId}", userId)

            createdUser.statusCode() == 200
            createdUser.path("name") == "testUser"
            createdUser.path("email") == "test.email@gmail.com"
            createdUser.path("status") == "active"
            createdUser.path("gender") == "male"

        cleanup: "user deleted"
            request.delete("/${userId}")
    }

    def "User email already taken"() {
        given: "a user with the test email has already been created"
            def requestBody = JsonOutput.toJson(createUserPayload)

            int userId = request.body(requestBody.toString()).post().path("id")

        when: "an attempt to create a user with the same email address has been made"
            def response = request.body(requestBody.toString()).post()

        then: "the call fails gracefully"
            response.statusCode == 422
            response.path("") == [[field:"email", message:"has already been taken"]]

        cleanup: "user deleted"
            request.delete("/${userId}")
    }

    def "Data validation for endpoint failed"() {
        given: "an improper request body"
            def requestBody = JsonOutput.toJson(["name": "", "email": "", "gender": "", "status": ""])

        when: "an attempt to create a user with the same email address has been made"
            def response = request.body(requestBody.toString()).post()

        then: "the call fails gracefully with proper message"
            response.statusCode == 422
            response.path("") == [
                    [field:"email", message:"can't be blank"],
                    [field:"name", message:"can't be blank"],
                    [field:"gender", message:"can't be blank"],
                    [field:"status", message:"can't be blank"]
            ]
    }
}
