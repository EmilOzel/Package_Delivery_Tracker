package app.config;

import app.entities.DeliveryEvent;
import app.entities.Parcel;
import app.entities.User;
import org.hibernate.cfg.Configuration;

final class EntityRegistry {

    private EntityRegistry() {}

    static void registerEntities(Configuration configuration) {

        configuration.addAnnotatedClass(User.class);
        configuration.addAnnotatedClass(Parcel.class);
        configuration.addAnnotatedClass(DeliveryEvent.class);
    }
}