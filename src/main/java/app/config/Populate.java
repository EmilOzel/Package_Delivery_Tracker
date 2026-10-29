package app.config;

import app.daos.UserDAO;
import app.entities.User;
import app.enums.Role;
import jakarta.persistence.EntityManagerFactory;

public class Populate {

    private static final String DEMO_EMAIL = "emil@example.com";

    public static void main(String[] args) {
        EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();
        try {
            UserDAO userDAO = new UserDAO(emf);
            if (userDAO.emailExists(DEMO_EMAIL)) {
                System.out.println("Demo user already exists");
                return;
            }

            User user = new User(
                    "Emil",
                    DEMO_EMAIL,
                    "not-used-before-security-is-added",
                    Role.USER
            );
            userDAO.create(user);
            System.out.println("Created demo user with id " + user.getId());
        } finally {
            if (emf.isOpen()) {
                emf.close();
            }
        }
    }
}
