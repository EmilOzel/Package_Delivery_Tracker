package app.daos;

import app.entities.DeliveryEvent;
import app.entities.Parcel;
import app.enums.PackageStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import java.util.List;
import java.util.Optional;

public class ParcelDAO {

    private final EntityManagerFactory emf;

    public ParcelDAO(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public Parcel createWithInitialEvent(Parcel parcel) {
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();
                em.persist(parcel);

                DeliveryEvent initialEvent = new DeliveryEvent(
                        parcel,
                        PackageStatus.CREATED,
                        null,
                        "Pakken blev oprettet"
                );
                em.persist(initialEvent);

                transaction.commit();
                return parcel;
            } catch (RuntimeException exception) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw exception;
            }
        }
    }

    public Optional<Parcel> findById(int id) {
        try (EntityManager em = emf.createEntityManager()) {
            return Optional.ofNullable(em.find(Parcel.class, id));
        }
    }

    public Optional<Parcel> findByTrackingNumber(String trackingNumber) {
        try (EntityManager em = emf.createEntityManager()) {
            List<Parcel> parcels = em.createQuery(
                            "SELECT p FROM Parcel p " +
                                    "JOIN FETCH p.owner " +
                                    "WHERE p.trackingNumber = :trackingNumber",
                            Parcel.class
                    )
                    .setParameter("trackingNumber", trackingNumber)
                    .setMaxResults(1)
                    .getResultList();

            return parcels.stream().findFirst();
        }
    }

    public List<Parcel> findAll() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery(
                            "SELECT p FROM Parcel p " +
                                    "JOIN FETCH p.owner " +
                                    "ORDER BY p.createdAt DESC",
                            Parcel.class
                    )
                    .getResultList();
        }
    }

    public List<Parcel> findAllByOwnerId(int ownerId) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery(
                            "SELECT p FROM Parcel p " +
                                    "JOIN FETCH p.owner " +
                                    "WHERE p.owner.id = :ownerId " +
                                    "ORDER BY p.createdAt DESC",
                            Parcel.class
                    )
                    .setParameter("ownerId", ownerId)
                    .getResultList();
        }
    }

    public List<DeliveryEvent> findEventsByParcelId(int parcelId) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery(
                            "SELECT e FROM DeliveryEvent e " +
                                    "JOIN FETCH e.parcel " +
                                    "WHERE e.parcel.id = :parcelId " +
                                    "ORDER BY e.timestamp ASC",
                            DeliveryEvent.class
                    )
                    .setParameter("parcelId", parcelId)
                    .getResultList();
        }
    }

    public DeliveryEvent addEventAndUpdateStatus(
            int parcelId,
            PackageStatus status,
            String location,
            String message
    ) {
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();

                Parcel parcel = em.find(Parcel.class, parcelId);
                if (parcel == null) {
                    transaction.rollback();
                    return null;
                }

                parcel.setCurrentStatus(status);
                DeliveryEvent event = new DeliveryEvent(parcel, status, location, message);
                em.persist(event);

                transaction.commit();
                return event;
            } catch (RuntimeException exception) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw exception;
            }
        }
    }

    public boolean deleteById(int parcelId) {
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();
                Parcel parcel = em.find(Parcel.class, parcelId);
                if (parcel == null) {
                    transaction.rollback();
                    return false;
                }

                em.createQuery("DELETE FROM DeliveryEvent e WHERE e.parcel.id = :parcelId")
                        .setParameter("parcelId", parcelId)
                        .executeUpdate();
                em.remove(parcel);
                transaction.commit();
                return true;
            } catch (RuntimeException exception) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw exception;
            }
        }
    }

    public boolean trackingNumberExists(String trackingNumber) {
        try (EntityManager em = emf.createEntityManager()) {
            Long count = em.createQuery(
                            "SELECT COUNT(p) FROM Parcel p WHERE p.trackingNumber = :trackingNumber",
                            Long.class
                    )
                    .setParameter("trackingNumber", trackingNumber)
                    .getSingleResult();
            return count > 0;
        }
    }
}
