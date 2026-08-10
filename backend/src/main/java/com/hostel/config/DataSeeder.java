package com.hostel.config;

import com.hostel.entity.*;
import com.hostel.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final WardenRepository wardenRepository;
    private final StudentRepository studentRepository;
    private final HostelBlockRepository hostelBlockRepository;
    private final RoomRepository roomRepository;
    private final ComplaintRepository complaintRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final NoticeRepository noticeRepository;
    private final MessFeedbackRepository messFeedbackRepository;
    private final LostAndFoundRepository lostAndFoundRepository;
    private final MarketplaceItemRepository marketplaceItemRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      AdminRepository adminRepository,
                      WardenRepository wardenRepository,
                      StudentRepository studentRepository,
                      HostelBlockRepository hostelBlockRepository,
                      RoomRepository roomRepository,
                      ComplaintRepository complaintRepository,
                      LeaveRequestRepository leaveRequestRepository,
                      NoticeRepository noticeRepository,
                      MessFeedbackRepository messFeedbackRepository,
                      LostAndFoundRepository lostAndFoundRepository,
                      MarketplaceItemRepository marketplaceItemRepository,
                      AuditLogRepository auditLogRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
        this.wardenRepository = wardenRepository;
        this.studentRepository = studentRepository;
        this.hostelBlockRepository = hostelBlockRepository;
        this.roomRepository = roomRepository;
        this.complaintRepository = complaintRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.noticeRepository = noticeRepository;
        this.messFeedbackRepository = messFeedbackRepository;
        this.lostAndFoundRepository = lostAndFoundRepository;
        this.marketplaceItemRepository = marketplaceItemRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already seeded, skipping data initialization");
            return;
        }

        log.info("Seeding database with sample data...");

        String password = passwordEncoder.encode("password123");

        // 1. Admin
        User adminUser = userRepository.save(User.builder()
                .name("System Admin")
                .email("admin@hostel.com")
                .password(password)
                .role(User.Role.ADMIN)
                .phone("9876543210")
                .build());
        adminRepository.save(Admin.builder().user(adminUser).department("Administration").build());
        log.info("Created admin: admin@hostel.com / password123");

        // 2. Blocks
        HostelBlock blockA = hostelBlockRepository.save(HostelBlock.builder()
                .name("A Wing - Senior Boys").code("A-BLOCK").address("Main Campus, North Side").build());
        HostelBlock blockB = hostelBlockRepository.save(HostelBlock.builder()
                .name("B Wing - Junior Boys").code("B-BLOCK").address("Main Campus, North Side").build());
        HostelBlock blockC = hostelBlockRepository.save(HostelBlock.builder()
                .name("C Wing - Girls Hostel").code("C-BLOCK").address("Main Campus, South Side").build());
        HostelBlock blockD = hostelBlockRepository.save(HostelBlock.builder()
                .name("D Wing - International PG").code("D-BLOCK").address("East Campus, Executive Block").build());
        log.info("Created 4 hostel blocks");

        // 3. Wardens
        User wardenUser1 = userRepository.save(User.builder()
                .name("Mr. Sharma")
                .email("warden@hostel.com")
                .password(password)
                .role(User.Role.WARDEN)
                .phone("9876543211")
                .build());
        wardenRepository.save(Warden.builder()
                .user(wardenUser1).block(blockA).qualification("M.Sc. Hostel Management").build());

        User wardenUser2 = userRepository.save(User.builder()
                .name("Mrs. Anjali Verma")
                .email("warden2@hostel.com")
                .password(password)
                .role(User.Role.WARDEN)
                .phone("9876543222")
                .build());
        wardenRepository.save(Warden.builder()
                .user(wardenUser2).block(blockC).qualification("M.A. Student Welfare").build());

        User wardenUser3 = userRepository.save(User.builder()
                .name("Dr. Rajesh Patel")
                .email("warden3@hostel.com")
                .password(password)
                .role(User.Role.WARDEN)
                .phone("9876543233")
                .build());
        wardenRepository.save(Warden.builder()
                .user(wardenUser3).block(blockB).qualification("Ph.D. Administration").build());
        log.info("Created 3 wardens");

        // 4. Rooms
        Room rA101 = roomRepository.save(Room.builder().roomNo("A-101").block(blockA).floor(1).capacity(2).rent(5000.0).status(Room.RoomStatus.OCCUPIED).occupants(1).build());
        Room rA102 = roomRepository.save(Room.builder().roomNo("A-102").block(blockA).floor(1).capacity(2).rent(5000.0).status(Room.RoomStatus.AVAILABLE).occupants(0).build());
        Room rA201 = roomRepository.save(Room.builder().roomNo("A-201").block(blockA).floor(2).capacity(3).rent(6000.0).status(Room.RoomStatus.OCCUPIED).occupants(2).build());

        Room rB101 = roomRepository.save(Room.builder().roomNo("B-101").block(blockB).floor(1).capacity(2).rent(4800.0).status(Room.RoomStatus.OCCUPIED).occupants(1).build());
        Room rB102 = roomRepository.save(Room.builder().roomNo("B-102").block(blockB).floor(1).capacity(2).rent(4800.0).status(Room.RoomStatus.MAINTENANCE).occupants(0).build());

        Room rC101 = roomRepository.save(Room.builder().roomNo("C-101").block(blockC).floor(1).capacity(2).rent(5500.0).status(Room.RoomStatus.OCCUPIED).occupants(1).build());
        Room rC102 = roomRepository.save(Room.builder().roomNo("C-102").block(blockC).floor(1).capacity(2).rent(5500.0).status(Room.RoomStatus.OCCUPIED).occupants(1).build());
        Room rC201 = roomRepository.save(Room.builder().roomNo("C-201").block(blockC).floor(2).capacity(1).rent(7000.0).status(Room.RoomStatus.AVAILABLE).occupants(0).build());

        Room rD101 = roomRepository.save(Room.builder().roomNo("D-101").block(blockD).floor(1).capacity(1).rent(8500.0).status(Room.RoomStatus.OCCUPIED).occupants(1).build());
        log.info("Created 9 sample rooms");

        // 5. Students
        User uStud1 = userRepository.save(User.builder().name("Rahul Kumar").email("student@hostel.com").password(password).role(User.Role.STUDENT).phone("9876543212").build());
        Student s1 = studentRepository.save(Student.builder().user(uStud1).room(rA101).enrollmentNo("ENR2024001").parentContact("9876543213").address("123, Main Street, Delhi").dateOfBirth("2002-01-15").gender(Student.Gender.MALE).build());

        User uStud2 = userRepository.save(User.builder().name("Priya Sharma").email("student2@hostel.com").password(password).role(User.Role.STUDENT).phone("9876543214").build());
        Student s2 = studentRepository.save(Student.builder().user(uStud2).room(rC101).enrollmentNo("ENR2024002").parentContact("9876543215").address("456, Park Avenue, Mumbai").dateOfBirth("2002-03-20").gender(Student.Gender.FEMALE).build());

        User uStud3 = userRepository.save(User.builder().name("Amit Singh").email("student3@hostel.com").password(password).role(User.Role.STUDENT).phone("9876543216").build());
        Student s3 = studentRepository.save(Student.builder().user(uStud3).room(rA201).enrollmentNo("ENR2024003").parentContact("9876543217").address("789, Civil Lines, Jaipur").dateOfBirth("2001-08-11").gender(Student.Gender.MALE).build());

        User uStud4 = userRepository.save(User.builder().name("Neha Gupta").email("student4@hostel.com").password(password).role(User.Role.STUDENT).phone("9876543218").build());
        Student s4 = studentRepository.save(Student.builder().user(uStud4).room(rC102).enrollmentNo("ENR2024004").parentContact("9876543219").address("12, Mall Road, Shimla").dateOfBirth("2002-11-05").gender(Student.Gender.FEMALE).build());

        User uStud5 = userRepository.save(User.builder().name("Vikram Malhotra").email("student5@hostel.com").password(password).role(User.Role.STUDENT).phone("9876543220").build());
        Student s5 = studentRepository.save(Student.builder().user(uStud5).room(rB101).enrollmentNo("ENR2024005").parentContact("9876543221").address("88, Ring Road, Chandigarh").dateOfBirth("2001-05-30").gender(Student.Gender.MALE).build());

        User uStud6 = userRepository.save(User.builder().name("Sarah Jenkins").email("student6@hostel.com").password(password).role(User.Role.STUDENT).phone("9876543225").build());
        Student s6 = studentRepository.save(Student.builder().user(uStud6).room(rD101).enrollmentNo("ENR2024006").parentContact("9876543226").address("London, United Kingdom").dateOfBirth("2000-09-14").gender(Student.Gender.FEMALE).build());
        log.info("Created 6 students");

        // 6. Complaints
        complaintRepository.save(Complaint.builder().student(s1).title("Fan not working").description("Ceiling fan in A-101 is not rotating and making buzzing noise.").category(Complaint.ComplaintCategory.ELECTRICAL).status(Complaint.ComplaintStatus.PENDING).build());
        complaintRepository.save(Complaint.builder().student(s1).title("Water tap leaking").description("Bathroom tap in A-101 leaks continuously.").category(Complaint.ComplaintCategory.PLUMBING).status(Complaint.ComplaintStatus.IN_PROGRESS).build());
        complaintRepository.save(Complaint.builder().student(s2).title("WiFi disconnects in C-Block").description("WiFi signal drops every 10 minutes in room C-101.").category(Complaint.ComplaintCategory.INTERNET).status(Complaint.ComplaintStatus.RESOLVED).build());
        complaintRepository.save(Complaint.builder().student(s3).title("Study chair broken leg").description("Right leg of study chair in A-201 is broken.").category(Complaint.ComplaintCategory.FURNITURE).status(Complaint.ComplaintStatus.PENDING).build());
        complaintRepository.save(Complaint.builder().student(s4).title("Cold food served at dinner").description("Dinner served on Friday night was cold.").category(Complaint.ComplaintCategory.MESS).status(Complaint.ComplaintStatus.RESOLVED).build());
        complaintRepository.save(Complaint.builder().student(s5).title("Corridor light flickers").description("B-Block 1st floor corridor light flickers during night.").category(Complaint.ComplaintCategory.GENERAL).status(Complaint.ComplaintStatus.IN_PROGRESS).build());
        log.info("Created sample complaints");

        // 7. Leave Requests
        leaveRequestRepository.save(LeaveRequest.builder().student(s1).fromDate("2026-08-01").toDate("2026-08-07").reason("Family trip to hometown").status(LeaveRequest.LeaveStatus.PENDING).build());
        leaveRequestRepository.save(LeaveRequest.builder().student(s1).fromDate("2026-06-10").toDate("2026-06-15").reason("Medical appointment").status(LeaveRequest.LeaveStatus.APPROVED).approvedBy("Mr. Sharma").remarks("Approved with medical prescription").build());
        leaveRequestRepository.save(LeaveRequest.builder().student(s2).fromDate("2026-08-05").toDate("2026-08-10").reason("Attending sister's wedding").status(LeaveRequest.LeaveStatus.APPROVED).approvedBy("Mrs. Anjali Verma").remarks("Sanctioned leave").build());
        leaveRequestRepository.save(LeaveRequest.builder().student(s3).fromDate("2026-07-28").toDate("2026-07-30").reason("Personal work").status(LeaveRequest.LeaveStatus.REJECTED).approvedBy("Mr. Sharma").remarks("Insufficient notice period").build());
        leaveRequestRepository.save(LeaveRequest.builder().student(s4).fromDate("2026-08-12").toDate("2026-08-16").reason("Competitive Exam in Delhi").status(LeaveRequest.LeaveStatus.PENDING).build());
        log.info("Created sample leave requests");

        // 8. Notices
        noticeRepository.save(Notice.builder().title("Hostel Annual Sports Meet 2026").content("Registrations are open for Badminton, Cricket, and Chess competitions. Submit names to block warden before Aug 5.").postedBy("System Admin").targetRole(Notice.TargetRole.ALL).build());
        noticeRepository.save(Notice.builder().title("Water Supply Shutdown").content("Water supply in A & B Blocks will be shut down on Sunday from 10 AM to 1 PM for tank cleaning.").postedBy("Mr. Sharma").targetRole(Notice.TargetRole.STUDENT).build());
        noticeRepository.save(Notice.builder().title("Mess Menu Feedback Survey").content("Please fill out the weekly mess rating in student dashboard before Friday 8 PM.").postedBy("System Admin").targetRole(Notice.TargetRole.STUDENT).build());
        noticeRepository.save(Notice.builder().title("Monthly Warden Coordination Meeting").content("All block wardens are requested to assemble in Admin Conference Room on 1st Aug at 4 PM.").postedBy("System Admin").targetRole(Notice.TargetRole.WARDEN).build());
        log.info("Created sample notices");

        // 9. Mess Feedback
        messFeedbackRepository.save(MessFeedback.builder().student(s1).date("2026-07-25").foodQualityRating(5).tasteRating(4).cleanlinessRating(5).comments("Excellent Paneer Butter Masala today!").sentiment(MessFeedback.Sentiment.POSITIVE).build());
        messFeedbackRepository.save(MessFeedback.builder().student(s2).date("2026-07-25").foodQualityRating(4).tasteRating(4).cleanlinessRating(4).comments("Clean dining area and prompt service.").sentiment(MessFeedback.Sentiment.POSITIVE).build());
        messFeedbackRepository.save(MessFeedback.builder().student(s3).date("2026-07-24").foodQualityRating(2).tasteRating(2).cleanlinessRating(3).comments("Roti was undercooked during lunch.").sentiment(MessFeedback.Sentiment.NEGATIVE).build());
        messFeedbackRepository.save(MessFeedback.builder().student(s4).date("2026-07-24").foodQualityRating(3).tasteRating(3).cleanlinessRating(4).comments("Average breakfast options.").sentiment(MessFeedback.Sentiment.NEUTRAL).build());
        messFeedbackRepository.save(MessFeedback.builder().student(s5).date("2026-07-23").foodQualityRating(4).tasteRating(5).cleanlinessRating(5).comments("Loved the Dosa and Coconut Chutney!").sentiment(MessFeedback.Sentiment.POSITIVE).build());
        log.info("Created sample mess feedback");

        // 10. Lost & Found
        lostAndFoundRepository.save(LostAndFound.builder().title("Blue Steel Water Bottle").description("Milton blue insulated bottle forgotten near Table 4 in Mess Hall.").status(LostAndFound.LostFoundStatus.LOST).category("Bottles").location("Mess Hall").contactInfo("student@hostel.com").reportedBy(s1).build());
        lostAndFoundRepository.save(LostAndFound.builder().title("Casio Scientific Calculator FX-991EX").description("Black Casio calculator with name sticker 'Priya' found in C-Block Study Room.").status(LostAndFound.LostFoundStatus.FOUND).category("Electronics").location("C-Block Library Room").contactInfo("warden2@hostel.com").reportedBy(s2).build());
        lostAndFoundRepository.save(LostAndFound.builder().title("Noise Wireless Earbuds").description("White Wireless Earbuds case lost near A-Block entrance.").status(LostAndFound.LostFoundStatus.LOST).category("Electronics").location("A-Block Gate").contactInfo("student3@hostel.com").reportedBy(s3).build());
        lostAndFoundRepository.save(LostAndFound.builder().title("Set of Keys with Leather Keychain").description("3 keys found on bench near Sports Complex.").status(LostAndFound.LostFoundStatus.RESOLVED).category("Keys").location("Sports Complex Bench").contactInfo("warden@hostel.com").reportedBy(s4).build());
        log.info("Created sample lost & found items");

        // 11. Marketplace
        marketplaceItemRepository.save(MarketplaceItem.builder().title("Computer Science Core Textbooks (4th Sem)").description("Includes OS, DBMS, Algorithms, and Computer Networks books in mint condition.").price(650.0).category("Books").seller(s1).status(MarketplaceItem.ItemStatus.AVAILABLE).build());
        marketplaceItemRepository.save(MarketplaceItem.builder().title("Adjustable LED Study Desk Lamp").description("Wipro 10W rechargeable LED desk lamp with 3 color temperatures.").price(350.0).category("Electronics").seller(s1).status(MarketplaceItem.ItemStatus.AVAILABLE).build());
        marketplaceItemRepository.save(MarketplaceItem.builder().title("Hero Octane Mountain Bicycle").description("21-gear mountain bike, recently serviced, new brake pads.").price(3200.0).category("Vehicles").seller(s3).status(MarketplaceItem.ItemStatus.AVAILABLE).build());
        marketplaceItemRepository.save(MarketplaceItem.builder().title("Ergonomic Mesh Office Chair").description("High-back mesh chair with lumbar support. Perfect for late-night studying.").price(1200.0).category("Furniture").seller(s5).status(MarketplaceItem.ItemStatus.AVAILABLE).build());
        marketplaceItemRepository.save(MarketplaceItem.builder().title("Sony Extra Bass Headphones").description("Over-ear wired headphones with punchy bass and mic.").price(800.0).category("Electronics").seller(s2).status(MarketplaceItem.ItemStatus.SOLD).build());
        log.info("Created sample marketplace items");

        // 12. Initial Audit Logs
        auditLogRepository.save(AuditLog.builder().action("HOSTEL_CREATED").severity(AuditSeverity.INFO).performedByUserId(adminUser.getId()).performedBy("admin@hostel.com").performedByRole("ADMIN").targetType("HOSTEL_BLOCK").targetId(blockA.getId()).details("Created A Wing - Senior Boys Block").ipAddress("127.0.0.1").userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)").build());
        auditLogRepository.save(AuditLog.builder().action("WARDEN_CREATED").severity(AuditSeverity.INFO).performedByUserId(adminUser.getId()).performedBy("admin@hostel.com").performedByRole("ADMIN").targetType("USER").targetId(wardenUser1.getId()).details("Assigned Mr. Sharma as warden to Block A").ipAddress("127.0.0.1").userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)").build());
        auditLogRepository.save(AuditLog.builder().action("ROOM_ALLOCATED").severity(AuditSeverity.INFO).performedByUserId(wardenUser1.getId()).performedBy("warden@hostel.com").performedByRole("WARDEN").targetType("ROOM").targetId(rA101.getId()).details("Allocated Room A-101 to student Rahul Kumar").ipAddress("127.0.0.1").userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)").build());
        auditLogRepository.save(AuditLog.builder().action("LEAVE_APPROVED").severity(AuditSeverity.INFO).performedByUserId(wardenUser1.getId()).performedBy("warden@hostel.com").performedByRole("WARDEN").targetType("LEAVE").targetId(1L).details("Approved leave for student Rahul Kumar").ipAddress("127.0.0.1").userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)").build());
        auditLogRepository.save(AuditLog.builder().action("NOTICE_CREATED").severity(AuditSeverity.INFO).performedByUserId(adminUser.getId()).performedBy("admin@hostel.com").performedByRole("ADMIN").targetType("NOTICE").targetId(1L).details("Posted announcement: Hostel Annual Sports Meet 2026").ipAddress("127.0.0.1").userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)").build());
        auditLogRepository.save(AuditLog.builder().action("LOGIN_SUCCESS").severity(AuditSeverity.INFO).performedByUserId(adminUser.getId()).performedBy("admin@hostel.com").performedByRole("ADMIN").targetType("AUTH").targetId(adminUser.getId()).details("Admin logged into system").ipAddress("127.0.0.1").userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)").build());
        log.info("Created sample audit log entries");

        log.info("Database seeding complete!");
        log.info("--- Credentials ---");
        log.info("Admin:     admin@hostel.com / password123");
        log.info("Warden:    warden@hostel.com / password123");
        log.info("Student 1: student@hostel.com / password123");
        log.info("Student 2: student2@hostel.com / password123");
    }
}
