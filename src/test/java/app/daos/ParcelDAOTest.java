package app.daos;

import app.config.HibernateConfig;
import app.entities.DeliveryEvent;
import app.entities.Parcel;
import app.entities.User;
import app.enums.PackageStatus;
import app.enums.Role;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParcelDAOTest {

    private static EntityManagerFactory emf;
    private static ParcelDAO parcelDAO;
    private static UserDAO userDAO;

    @BeforeAll
    static void setUp() {
        emf = HibernateConfig.createTestEntityManagerFactory();
        parcelDAO = new ParcelDAO(emf);
        userDAO = new UserDAO(emf);
    }

    @AfterAll
    static void tearDown() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    @Test
    void createsAndFindsUserByEmail() {
        User user = userDAO.create(new User(
                "DAO User",
                "dao-user@example.com",
                "password-hash",
                Role.USER
        ));

        User found = userDAO.findByEmail("DAO-USER@example.com").orElseThrow();
        assertEquals(user.getId(), found.getId());
        assertEquals("DAO User", found.getName());
    }

    @Test
    void createsParcelWithEventAndUpdatesStatus() {
        User owner = userDAO.create(new User(
                "Parcel Owner",
                "parcel-owner@example.com",
                "password-hash",
                Role.USER
        ));
        Parcel parcel = new Parcel(
                "PKG-DAO123",
                "Emil",
                "Bo Jensen",
                "Amperestien 10, 2605 Brøndby",
                55.69,
                12.55,
                owner
        );

        parcelDAO.createWithInitialEvent(parcel);

        assertTrue(parcelDAO.findByTrackingNumber("PKG-DAO123").isPresent());
        List<DeliveryEvent> initialEvents = parcelDAO.findEventsByParcelId(parcel.getId());
        assertEquals(1, initialEvents.size());
        assertEquals(PackageStatus.CREATED, initialEvents.getFirst().getStatus());

        parcelDAO.addEventAndUpdateStatus(
                parcel.getId(),
                PackageStatus.PICKED_UP,
                "København",
                "Pakken er afhentet"
        );

        assertEquals(
                PackageStatus.PICKED_UP,
                parcelDAO.findById(parcel.getId()).orElseThrow().getCurrentStatus()
        );
        assertEquals(2, parcelDAO.findEventsByParcelId(parcel.getId()).size());
    }
}
