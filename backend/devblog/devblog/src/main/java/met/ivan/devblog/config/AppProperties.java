package met.ivan.devblog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Seed seed = new Seed();
    private Security security = new Security();

    public Seed getSeed() { return seed; }
    public void setSeed(Seed seed) { this.seed = seed; }

    public Security getSecurity() { return security; }
    public void setSecurity(Security security) { this.security = security; }

    public static class Security {
        private int bcryptStrength = 12;

        public int getBcryptStrength() { return bcryptStrength; }
        public void setBcryptStrength(int bcryptStrength) { this.bcryptStrength = bcryptStrength; }
    }

    public static class Seed {
        private SeedUser admin = new SeedUser();
        private SeedUser user = new SeedUser();

        public SeedUser getAdmin() { return admin; }
        public void setAdmin(SeedUser admin) { this.admin = admin; }
        public SeedUser getUser() { return user; }
        public void setUser(SeedUser user) { this.user = user; }
    }

    public static class SeedUser {
        private String username;
        private String email;
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
