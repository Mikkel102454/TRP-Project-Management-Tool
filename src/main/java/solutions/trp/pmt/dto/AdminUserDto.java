package solutions.trp.pmt.dto;

public class AdminUserDto extends UserDto {
    private String pmUserId;
    private String pmProfileName;

    public String getPmUserId() { return pmUserId; }
    public void setPmUserId(String pmUserId) { this.pmUserId = pmUserId; }
    public String getPmProfileName() { return pmProfileName; }
    public void setPmProfileName(String pmProfileName) { this.pmProfileName = pmProfileName; }
}
