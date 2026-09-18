package com.hostel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Detailed student profile for warden view")
public class StudentDetailsDto {

    // Profile
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String enrollmentNo;
    private String parentContact;
    private String address;
    private String dateOfBirth;
    private String gender;
    private String profileImageUrl;

    // Current room
    private Long roomId;
    private String roomNo;
    private Integer floor;
    private Integer capacity;
    private Integer occupants;
    private String status;
    private String blockName;

    // Recent records (limited by service, never passwords/tokens)
    @Builder.Default
    private List<LeaveRequestDto> recentLeaves = new ArrayList<>();

    @Builder.Default
    private List<ComplaintDto> recentComplaints = new ArrayList<>();
}
