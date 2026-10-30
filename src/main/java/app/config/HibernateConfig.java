package app.config;

import app.entities.DeliveryEvent;
import app.entities.Parcel;
import app.entities.User;
import app.utils.Utils;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

import java.util.Properties;

public final class HibernateConfig {

    private static EntityManagerFactory emf;

    private HibernateConfig() {}

    public static EntityManagerFactory getEntityManagerFactory() {
        if (emf == null) {
            Properties properties = buildProperties();
            emf = buildEntityManagerFactory(properties);
        }
        return emf;
    }

    private static Properties buildProperties() {
        Properties props = baseProperties();

        String ddlMode = System.getenv("HIBERNATE_DDL_AUTO");
        if (ddlMode == null) {
            ddlMode = "update";
        }
        props.put("hibernate.hbm2ddl.auto", ddlMode);

        if (System.getenv("DEPLOYED") != null) {
            setDeployedProperties(props);
        } else {
            setDevProperties(props);
        }
        return props;
    }

    public static EntityManagerFactory createTestEntityManagerFactory() {
        Properties props = baseProperties();
        props.put("hibernate.connection.driver_class", "org.testcontainers.jdbc.ContainerDatabaseDriver");
        props.put("hibernate.connection.url", "jdbc:tc:postgresql:16-alpine:///PackageTrackerTest");
        props.put("hibernate.connection.username", "postgres");
        props.put("hibernate.connection.password", "postgres");
        props.put("hibernate.hbm2ddl.auto", "create-drop");
        return buildEntityManagerFactory(props);
    }

    private static Properties baseProperties() {
        Properties props = new Properties();
        props.put("hibernate.connection.driver_class", "org.postgresql.Driver");
        props.put("hibernate.current_session_context_class", "thread");
        props.put("hibernate.show_sql", "false");
        props.put("hibernate.format_sql", "false");
        props.put("hibernate.hikari.maximumPoolSize", "10");
        props.put("hibernate.hikari.minimumIdle", "2");
        props.put("hibernate.hikari.connectionTimeout", "20000");
        return props;
    }

    private static EntityManagerFactory buildEntityManagerFactory(Properties properties) {
        Configuration configuration = new Configuration();
        configuration.setProperties(properties);
        configuration.addAnnotatedClass(User.class);
        configuration.addAnnotatedClass(Parcel.class);
        configuration.addAnnotatedClass(DeliveryEvent.class);

        StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder();
        registryBuilder.applySettings(configuration.getProperties());
        ServiceRegistry registry = registryBuilder.build();

        SessionFactory sessionFactory = configuration.buildSessionFactory(registry);
        return sessionFactory.unwrap(EntityManagerFactory.class);
    }

    private static void setDeployedProperties(Properties props) {
        String dbName = System.getenv("DB_NAME");
        props.setProperty("hibernate.connection.url", System.getenv("CONNECTION_STR") + dbName);
        props.setProperty("hibernate.connection.username", System.getenv("DB_USERNAME"));
        props.setProperty("hibernate.connection.password", System.getenv("DB_PASSWORD"));
    }

    private static void setDevProperties(Properties props) {
        String dbName = Utils.getPropertyValue("DB_NAME", "config.properties");
        String username = Utils.getPropertyValue("DB_USERNAME", "config.properties");
        String password = Utils.getPropertyValue("DB_PASSWORD", "config.properties");

        props.put("hibernate.connection.url", "jdbc:postgresql://localhost:5432/" + dbName);
        props.put("hibernate.connection.username", username);
        props.put("hibernate.connection.password", password);
    }
}
