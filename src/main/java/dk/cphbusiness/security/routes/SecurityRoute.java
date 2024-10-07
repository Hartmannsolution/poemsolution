package dk.cphbusiness.security.routes;

import dk.cphbusiness.persistence.HibernateConfig;
import dk.cphbusiness.security.controllers.ISecurityController;
import dk.cphbusiness.security.controllers.SecurityController;
import dk.cphbusiness.security.daos.ISecurityDAO;
import io.javalin.apibuilder.EndpointGroup;

import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;

/**
 * Purpose:
 *
 * @author: Thomas Hartmann
 */
public class SecurityRoute {
    private static ISecurityController securityController = SecurityController.getInstance(HibernateConfig.getEntityManagerFactory());

    public static EndpointGroup getSecurityRoutes(){
        return ()->{
            path("/auth", ()->{
                post("/login", securityController.login());
                post("/register", securityController.register());
            });
        };
    }

}
