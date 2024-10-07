package dk.cphbusiness.rest;

import dk.cphbusiness.persistence.HibernateConfig;
import dk.cphbusiness.rest.controllers.PoemController;
import dk.cphbusiness.security.controllers.ISecurityController;
import dk.cphbusiness.security.controllers.SecurityController;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.security.RouteRole;

import static io.javalin.apibuilder.ApiBuilder.*;

/**
 * Purpose: To handle the routes for the rest api
 *
 * Author: Thomas Hartmann
 */
public class RestRoutes {
    PoemController poemController = PoemController.getInstance(HibernateConfig.getEntityManagerFactory()); // IN memory person collection
    ISecurityController securityController = SecurityController.getInstance(HibernateConfig.getEntityManagerFactory());

    public EndpointGroup getPoemRoutes() {
        return () -> {
            path("/", () -> {
                path("/poem", () -> {
                    before(securityController.authenticate());
                    get("/", poemController.getAll());
                    // Populate is a GET request for convenience, but it should be a POST request.
                    get("/populate", poemController.resetData(), Role.ANYONE);
                    get("/{id}", poemController.getById());
                    post("/", poemController.create());
                    put("/{id}", poemController.update());
                    delete("/{id}", poemController.delete(), Role.ADMIN, Role.USER);
                });
            });
        };
    }
    public enum Role implements RouteRole { ANYONE, USER, ADMIN }
}