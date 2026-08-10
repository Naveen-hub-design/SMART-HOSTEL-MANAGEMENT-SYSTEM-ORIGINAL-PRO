package com.hostel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Mess feedback details")
public class MessFeedbackDto {
    @Schema(example = "1")
    private Long id;

    @Schema(example = "John Doe")
    private String studentName;

    @NotBlank(message = "Date is required")
    @Schema(example = "2024-03-01")
    private String date;

    @Schema(example = "4")
    private Integer foodQualityRating;

    @Schema(example = "5")
    private Integer tasteRating;

    @Schema(example = "3")
    private Integer cleanlinessRating;

    @Schema(example = "Good food overall")
    private String comments;

    @Schema(example = "POSITIVE")
    private String sentiment;

    @Schema(example = "2024-03-01T12:30:00")
    private LocalDateTime createdAt;

    public void setFoodQuality(Integer foodQuality) {
        if (this.foodQualityRating == null) this.foodQualityRating = foodQuality;
    }

    public Integer getFoodQuality() {
        return this.foodQualityRating;
    }

    public void setTaste(Integer taste) {
        if (this.tasteRating == null) this.tasteRating = taste;
    }

    public Integer getTaste() {
        return this.tasteRating;
    }

    public void setCleanliness(Integer cleanliness) {
        if (this.cleanlinessRating == null) this.cleanlinessRating = cleanliness;
    }

    public Integer getCleanliness() {
        return this.cleanlinessRating;
    }
}
