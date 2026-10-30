package fu.de190299.postservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import fu.de190299.postservice.common.PostConstants;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Date;

public class PostDTO {

    private Long postId;

    @NotBlank(message = "advertiserName is required")
    @Size(max = 100, message = "advertiserName max length is 100")
    private String advertiserName;

    @NotBlank(message = "advertiserEmail is required")
    @Email(message = "advertiserEmail is invalid")
    @Size(max = 100, message = "advertiserEmail max length is 100")
    private String advertiserEmail;

    @NotBlank(message = "advertiserPhone is required")
    @Size(max = 20, message = "advertiserPhone max length is 20")
    private String advertiserPhone;

    @NotNull(message = "startDate is required")
    @JsonFormat(pattern = "dd/MM/yyyy")
    private Date startDate;

    @NotNull(message = "endDate is required")
    @JsonFormat(pattern = "dd/MM/yyyy")
    private Date endDate;

    @NotNull(message = "numberOfPosts is required")
    @Min(value = 1, message = "numberOfPosts must be >= 1")
    private Integer numberOfPosts;

    private BigDecimal totalCost;

    @Pattern(regexp = PostConstants.STATUS_REGEX, message = "status must be DRAFT, SCHEDULED, PUBLISHED or CANCELLED")
    private String status;

    @NotNull(message = "userId is required")
    private Long userId;

    public PostDTO() {
    }

    public PostDTO(Long postId, String advertiserName, String advertiserEmail, String advertiserPhone, Date startDate, Date endDate, Integer numberOfPosts, BigDecimal totalCost, String status, Long userId) {
        this.postId = postId;
        this.advertiserName = advertiserName;
        this.advertiserEmail = advertiserEmail;
        this.advertiserPhone = advertiserPhone;
        this.startDate = startDate;
        this.endDate = endDate;
        this.numberOfPosts = numberOfPosts;
        this.totalCost = totalCost;
        this.status = status;
        this.userId = userId;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public String getAdvertiserName() {
        return advertiserName;
    }

    public void setAdvertiserName(String advertiserName) {
        this.advertiserName = advertiserName;
    }

    public String getAdvertiserEmail() {
        return advertiserEmail;
    }

    public void setAdvertiserEmail(String advertiserEmail) {
        this.advertiserEmail = advertiserEmail;
    }

    public String getAdvertiserPhone() {
        return advertiserPhone;
    }

    public void setAdvertiserPhone(String advertiserPhone) {
        this.advertiserPhone = advertiserPhone;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Integer getNumberOfPosts() {
        return numberOfPosts;
    }

    public void setNumberOfPosts(Integer numberOfPosts) {
        this.numberOfPosts = numberOfPosts;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
