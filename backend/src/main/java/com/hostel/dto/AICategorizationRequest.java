package com.hostel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "AI categorization request")
public class AICategorizationRequest {
    @Schema(example = "Broken fan in room")
    private String complaintTitle;

    @Schema(example = "The ceiling fan in room A101 is not working and making noise")
    private String complaintDescription;

    public void setTitle(String title) {
        if (this.complaintTitle == null) this.complaintTitle = title;
    }

    public String getTitle() {
        return this.complaintTitle;
    }

    public void setDescription(String description) {
        if (this.complaintDescription == null) this.complaintDescription = description;
    }

    public String getDescription() {
        return this.complaintDescription;
    }
}
