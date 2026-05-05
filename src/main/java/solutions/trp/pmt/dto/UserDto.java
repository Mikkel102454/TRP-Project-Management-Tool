package solutions.trp.pmt.dto;

public class UserDto {
    private int id;
    private String username;
    private String initial;
    private boolean isAdmin;
    private boolean isEnabled;
    private boolean forcedClockedOut;
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getInitial() {
        return initial;
    }

    public void setInitial(String initial) {
        this.initial = initial;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public boolean isForcedClockedOut() {
        return forcedClockedOut;
    }

    public void setForcedClockedOut(boolean forcedClockedOut) {
        this.forcedClockedOut = forcedClockedOut;
    }
}
