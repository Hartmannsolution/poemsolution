package dk.cphbusiness.security.controllers;

import ch.qos.logback.core.encoder.EchoEncoder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.bugelhartmann.ITokenSecurity;
import dk.bugelhartmann.TokenSecurity;
import dk.bugelhartmann.UserDTO;
import dk.cphbusiness.persistence.HibernateConfig;
import dk.cphbusiness.rest.controllers.IController;
import dk.cphbusiness.security.daos.ISecurityDAO;
import dk.cphbusiness.security.daos.SecurityDAO;
import dk.cphbusiness.security.entities.User;
import dk.cphbusiness.security.exceptions.ApiException;
import dk.cphbusiness.security.exceptions.NotAuthorizedException;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityManagerFactory;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
* Purpose: 
* @author: Thomas Hartmann
*/
public class SecurityController implements ISecurityController{
    private final String SECRET_KEY = "841D8A6C80CBA4FCAD32D5367C18C5LKJLKDFSDFSD";
    ObjectMapper objectMapper = new ObjectMapper();
    static ISecurityDAO securityDAO = null;
    ITokenSecurity tokenSecurity = new TokenSecurity();
    private static SecurityController instance;
    private SecurityController(){}

    public static SecurityController getInstance(EntityManagerFactory emf){
        if(instance == null){
            instance = new SecurityController();
            securityDAO = new SecurityDAO(emf);
        }
        return instance;
    }

    @Override
    public Handler login() {
        return (ctx)->{
            UserDTO userInput = ctx.bodyAsClass(UserDTO.class);
            UserDTO verifiedUser = securityDAO.getVerifiedUser(userInput.getUsername(), userInput.getPassword());
            String token = createToken(verifiedUser);
        };
    }

    @Override
    public Handler register() {
        return (ctx) -> {
            ObjectNode returnObject = objectMapper.createObjectNode();
            try {
                UserDTO userInput = ctx.bodyAsClass(UserDTO.class);
                User created = securityDAO.createUser(userInput.getUsername(), userInput.getPassword());

                String token = createToken(new UserDTO(created.getUsername(), Set.of("user")));
                ctx.status(HttpStatus.CREATED).json(returnObject
                        .put("token", token)
                        .put("username", created.getUsername()));
            } catch (EntityExistsException e) {
                ctx.status(HttpStatus.UNPROCESSABLE_CONTENT);
                ctx.json(returnObject.put("msg", "User already exists"));
            }
        };
    }

    @Override
    public Handler authenticate() {

        ObjectNode returnObject = objectMapper.createObjectNode();
        return (ctx) -> {
            // This is a preflight request => OK
            if (ctx.method().toString().equals("OPTIONS")) {
                ctx.status(200);
                return;
            }
            String header = ctx.header("Authorization");
            // If there is no token we do not allow entry
            if (header == null) {
                ctx.status(HttpStatus.FORBIDDEN).json(returnObject.put("msg", "Authorization header missing"));
                return;
            }
            String token = header.split(" ")[1];
            // If the Authorization Header was malformed = no entry
            if (token == null) {
                ctx.status(HttpStatus.FORBIDDEN).json(returnObject.put("msg", "Authorization header malformed"));
                return;
            }
            UserDTO verifiedTokenUser = verifyToken(token);
            if (verifiedTokenUser == null) {
                ctx.status(HttpStatus.FORBIDDEN).json(returnObject.put("msg", "Invalid User or Token"));
            }
            System.out.println("USER IN AUTHENTICATE: " + verifiedTokenUser);
            ctx.attribute("user", verifiedTokenUser); // -> ctx.attribute("user") in ApplicationConfig beforeMatched filter
        };

    }

    @Override
    public boolean authorize(UserDTO userDTO, Set<String> allowedRoles) {
        AtomicBoolean hasAccess = new AtomicBoolean(false); // Since we update this in a lambda expression, we need to use an AtomicBoolean
        if (userDTO != null) {
            userDTO.getRoles().stream().forEach(role -> {
                if (allowedRoles.contains(role.toUpperCase())) {
                    hasAccess.set(true);
                }
            });
        }
        return hasAccess.get();
    }

    @Override
    public String createToken(UserDTO user) throws Exception {
        try {
            String ISSUER = "Thomas Hartmann";
            String TOKEN_EXPIRE_TIME = "1800000"; // 30 min
            return tokenSecurity.createToken(user, ISSUER, TOKEN_EXPIRE_TIME, SECRET_KEY);
        } catch(Exception e){
            throw new ApiException(500, "Could not create token");
        }
    }

    @Override
    public UserDTO verifyToken(String token) throws Exception {
        if(tokenSecurity.tokenIsValid(token, SECRET_KEY)){
            return tokenSecurity.getUserWithRolesFromToken(token);
        }
        else {
            throw new NotAuthorizedException(403, "Token is not valid");
        }
    }
}
