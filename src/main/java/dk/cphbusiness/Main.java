package dk.cphbusiness;

import dk.cphbusiness.rest.ApplicationConfig;
import dk.cphbusiness.rest.RestRoutes;
import dk.cphbusiness.security.routes.SecurityRoute;

import static io.javalin.apibuilder.ApiBuilder.get;

public class Main {
    public static void main(String[] args) {
        ApplicationConfig
                .getInstance()
                .initiateServer()
                .setRoutes(new RestRoutes().getPoemRoutes()) // A different way to get the EndpointGroup.
                .setRoutes(SecurityRoute.getSecurityRoutes())
                .checkSecurityRoles()
                .startServer(7070)
//                .setCORS()
                .setGeneralExceptionHandling();
//            .setErrorHandling()
//                .setApiExceptionHandling();
    }
}