package app.daos;

import app.entities.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import java.util.List;
import java.util.Optional;

public class UserDAO {

    private final EntityManagerFactory emf;

    public UserDAO(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public Optional<User> findById(int id) {
        try (EntityManager em = emf.createEntityManager()) {
            return Optional.ofNullable(em.find(User.class, id));
        }
    }

    public Optional<User> findByEmail(String email) {
        try (EntityManager em = emf.createEntityManager()) {
            List<User> users = em.createQuery(
                            "SELECT u FROM User u WHERE lower(u.email) = lower(:email)",
                            User.class
                    )
                    .setParameter("email", email)
                    .setMaxResults(1)
                    .getResultList();

            if (users.isEmpty()) {
                return Optional.empty();
            }

            User user = users.getFirst();
            return Optional.of(user);
        }
    }

    public User create(User user) {
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();
                em.persist(user);
                transaction.commit();
                return user;
            } catch (RuntimeException exception) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw exception;
            }
        }
    }

}
