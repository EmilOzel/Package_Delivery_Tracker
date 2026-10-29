package app.config;

import app.daos.UserDAO;
import app.entities.User;
import app.enums.Role;
import app.security.PasswordService;
import jakarta.persistence.EntityManagerFactory;

public class Populate {

    public static void main(String[] args) {
        EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();
        try {
            UserDAO userDAO = new UserDAO(emf);
            PasswordService passwordService = new PasswordService();
            createIfMissing(
                    userDAO,
                    passwordService,
                    "Emil",
                    "emil@example.com",
                    "Demo-password-2026",
                    Role.USER
            );
            createIfMissing(
                    userDAO,
                    passwordService,
                    "Demo Employee",
                    "employee@example.com",
                    "Employee-password-2026",
                    Role.EMPLOYEE
            );
        } finally {
            if (emf.isOpen()) {
                emf.close();
            }
        }
    }

    private static void createIfMissing(
            UserDAO userDAO,
            PasswordService passwordService,
            String name,
            String email,
            String password,
            Role role
    ) {
        if (userDAO.findByEmail(email).isPresent()) {
            System.out.println(email + " already exists");
            return;
        }

        User user = new User(name, email, passwordService.hash(password), role);
        userDAO.create(user);
        System.out.println("Created " + role + " with id " + user.getId());
    }
}
