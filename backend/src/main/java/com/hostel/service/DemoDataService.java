package com.hostel.service;

import com.hostel.entity.*;
import com.hostel.repository.*;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Demo-data reset/import service.
 *
 * <p>Wipes existing application data in foreign-key-safe order and imports a
 * realistic medium-sized dataset that covers every business module.
 *
 * <p>Controlled explicitly via properties:
 * <ul>
 *   <li>{@code app.demo-data.reset} (default false) - pass {@code --app.demo-data.reset=true}
 *       to wipe and re-import on the next startup.</li>
 *   <li>{@code app.data-seeder.enabled} (default true) - master switch.</li>
 *   <li>{@code app.demo-data.seed-on-empty} (default true) - seed automatically when the
 *       database is empty.</li>
 * </ul>
 */
@Service
public class DemoDataService {

    private static final Logger log = LoggerFactory.getLogger(DemoDataService.class);

    private static final String DEMO_PASSWORD = "password123";

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
    private final EntityManager entityManager;

    public DemoDataService(UserRepository userRepository,
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
                           PasswordEncoder passwordEncoder,
                           EntityManager entityManager) {
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
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public boolean isEmpty() {
        return wardenRepository.count() == 0
                && studentRepository.count() == 0
                && roomRepository.count() == 0
                && hostelBlockRepository.count() == 0;
    }

    /**
     * Imports the full demo dataset without wiping anything first.
     *
     * <p>Existing rows (for example a pre-provisioned admin account) are
     * preserved: users are reused by email instead of being duplicated.
     */
    @Transactional
    public void seedIfEmpty() {
        log.info("========== DEMO DATA SEED (ON EMPTY DB) START ==========");
        seed();
        verify();
        log.info("========== DEMO DATA SEED COMPLETE ==========");
    }

    /**
     * Wipes all application data and imports the full demo dataset in one transaction.
     */
    @Transactional
    public void resetAndSeed() {
        log.info("========== DEMO DATA RESET / IMPORT START ==========");
        wipeAll();
        seed();
        verify();
        log.info("========== DEMO DATA RESET / IMPORT COMPLETE ==========");
    }

    /**
     * Deletes all data in FK-safe order (children before parents).
     *
     * <p>Clears every user table (including any legacy tables left behind by older
     * schemas, which {@code ddl-auto=update} never drops) with FK checks disabled
     * for the current database dialect, so leftover rows cannot block the wipe.
     */
    @Transactional
    public void wipeAll() {
        boolean h2 = getDialect().contains("H2");
        if (h2) {
            entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
        } else {
            entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS=0").executeUpdate();
        }
        String schemaFilter = h2 ? "TABLE_SCHEMA='PUBLIC'" : "TABLE_SCHEMA = DATABASE()";
        String schemaQuery = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE " + schemaFilter;
        List<?> rows = entityManager.createNativeQuery(schemaQuery).getResultList();
        for (Object row : rows) {
            String table = String.valueOf(row);
            entityManager.createNativeQuery("DELETE FROM " + table).executeUpdate();
        }
        if (h2) {
            entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
        } else {
            entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS=1").executeUpdate();
        }
        entityManager.flush();
        entityManager.clear();
        log.info("Existing data wiped (FK-safe order, " + rows.size() + " tables).");
    }

    private String getDialect() {
        try {
            Object sf = entityManager.getEntityManagerFactory()
                    .unwrap(org.hibernate.engine.spi.SessionFactoryImplementor.class);
            org.hibernate.dialect.Dialect d = ((org.hibernate.engine.spi.SessionFactoryImplementor) sf)
                    .getJdbcServices().getDialect();
            return d.getClass().getName();
        } catch (Exception e) {
            return "";
        }
    }

    private void seed() {
        String password = passwordEncoder.encode(DEMO_PASSWORD);

        // ============ ADMINS ============
        User admin1 = saveUser("System Admin", "admin@hostel.com", User.Role.ADMIN, "9876543210", password);
        if (adminRepository.findByUser(admin1).isEmpty()) {
            adminRepository.save(Admin.builder().user(admin1).department("Administration").build());
        }
        User admin2 = saveUser("Naveen Kumar", "admin2@hostel.com", User.Role.ADMIN, "9876543209", password);
        if (adminRepository.findByUser(admin2).isEmpty()) {
            adminRepository.save(Admin.builder().user(admin2).department("Hostel Operations").build());
        }
        log.info("Admins: " + adminRepository.count());

        // ============ HOSTEL BLOCKS ============
        HostelBlock blockA = saveBlock("A Wing - Senior Boys", "A-BLOCK", "Main Campus, North Side");
        HostelBlock blockB = saveBlock("B Wing - Junior Boys", "B-BLOCK", "Main Campus, North Side");
        HostelBlock blockC = saveBlock("C Wing - Girls Hostel", "C-BLOCK", "Main Campus, South Side");
        HostelBlock blockD = saveBlock("D Wing - PG / International", "D-BLOCK", "East Campus, Executive Block");
        List<HostelBlock> blocks = List.of(blockA, blockB, blockC, blockD);
        log.info("Hostel blocks: 4");

        // ============ WARDENS ============
        Warden wardenA = saveWarden("Mr. Sharma", "warden@hostel.com", "9876543211", blockA, "M.Sc. Hostel Management", password);
        Warden wardenC = saveWarden("Mrs. Anjali Verma", "warden2@hostel.com", "9876543222", blockC, "M.A. Student Welfare", password);
        Warden wardenB = saveWarden("Dr. Rajesh Patel", "warden3@hostel.com", "9876543233", blockB, "Ph.D. Administration", password);
        Warden wardenD = saveWarden("Mr. Ravi Menon", "warden4@hostel.com", "9876543244", blockD, "M.B.A. Facility Management", password);
        List<Warden> wardens = List.of(wardenA, wardenB, wardenC, wardenD);
        log.info("Wardens: 4");

        // ============ ROOMS ============
        // format: block, roomNo, floor, capacity, rent, status, occupants
        List<Room> rooms = new ArrayList<>();
        rooms.addAll(saveRooms(blockA, List.of(
                new Object[]{"A-101", 1, 2, 5000.0, "OCCUPIED", 2},
                new Object[]{"A-102", 1, 2, 5000.0, "OCCUPIED", 2},
                new Object[]{"A-103", 1, 3, 6000.0, "OCCUPIED", 2},
                new Object[]{"A-104", 1, 2, 5000.0, "OCCUPIED", 1},
                new Object[]{"A-201", 2, 2, 5500.0, "OCCUPIED", 2},
                new Object[]{"A-202", 2, 2, 5500.0, "OCCUPIED", 1},
                new Object[]{"A-203", 2, 3, 6500.0, "OCCUPIED", 2},
                new Object[]{"A-301", 3, 2, 6000.0, "AVAILABLE", 0},
                new Object[]{"A-302", 3, 2, 6000.0, "MAINTENANCE", 0})));
        rooms.addAll(saveRooms(blockB, List.of(
                new Object[]{"B-101", 1, 2, 4800.0, "OCCUPIED", 2},
                new Object[]{"B-102", 1, 2, 4800.0, "OCCUPIED", 2},
                new Object[]{"B-103", 1, 2, 4800.0, "OCCUPIED", 1},
                new Object[]{"B-201", 2, 3, 5200.0, "OCCUPIED", 3},
                new Object[]{"B-202", 2, 2, 5200.0, "OCCUPIED", 1},
                new Object[]{"B-203", 2, 2, 5200.0, "AVAILABLE", 0},
                new Object[]{"B-301", 3, 2, 5600.0, "AVAILABLE", 0},
                new Object[]{"B-302", 3, 2, 5600.0, "MAINTENANCE", 0},
                new Object[]{"B-303", 3, 2, 5600.0, "AVAILABLE", 0})));
        rooms.addAll(saveRooms(blockC, List.of(
                new Object[]{"C-101", 1, 2, 5500.0, "OCCUPIED", 2},
                new Object[]{"C-102", 1, 2, 5500.0, "OCCUPIED", 2},
                new Object[]{"C-103", 1, 2, 5500.0, "OCCUPIED", 2},
                new Object[]{"C-104", 1, 2, 5500.0, "OCCUPIED", 1},
                new Object[]{"C-201", 2, 3, 6000.0, "OCCUPIED", 2},
                new Object[]{"C-202", 2, 2, 6000.0, "OCCUPIED", 2},
                new Object[]{"C-203", 2, 2, 6000.0, "OCCUPIED", 1},
                new Object[]{"C-301", 3, 2, 6500.0, "AVAILABLE", 0},
                new Object[]{"C-302", 3, 2, 6500.0, "MAINTENANCE", 0})));
        rooms.addAll(saveRooms(blockD, List.of(
                new Object[]{"D-101", 1, 1, 8500.0, "OCCUPIED", 1},
                new Object[]{"D-102", 1, 1, 8500.0, "OCCUPIED", 1},
                new Object[]{"D-103", 1, 1, 8500.0, "OCCUPIED", 1},
                new Object[]{"D-104", 1, 1, 8500.0, "AVAILABLE", 0},
                new Object[]{"D-105", 1, 1, 8500.0, "AVAILABLE", 0})));
        log.info("Rooms: " + rooms.size());

        // ============ STUDENTS (assigned to rooms, occupants consistent) ============
        String[] addresses = {
                "123, Main Street, Delhi", "456, Park Avenue, Mumbai", "789, Civil Lines, Jaipur",
                "12, Mall Road, Shimla", "88, Ring Road, Chandigarh", "Banjara Hills, Hyderabad",
                "Salt Lake, Kolkata", "MG Road, Pune"
        };

        record StudentSeed(String name, String email, Student.Gender gender, String dob) {
        }

        // order matters: room index below maps to these students
        List<StudentSeed> studentSeeds = List.of(
                // A block (12)
                new StudentSeed("Rahul Kumar", "student@hostel.com", Student.Gender.MALE, "2002-01-15"),
                new StudentSeed("Arjun Mehta", "arjun.mehta@hostel.com", Student.Gender.MALE, "2002-04-22"),
                new StudentSeed("Amit Singh", "amit.singh@hostel.com", Student.Gender.MALE, "2001-08-11"),
                new StudentSeed("Vikram Malhotra", "vikram.malhotra@hostel.com", Student.Gender.MALE, "2001-05-30"),
                new StudentSeed("Rohan Sharma", "rohan.sharma@hostel.com", Student.Gender.MALE, "2002-07-09"),
                new StudentSeed("Karan Verma", "karan.verma@hostel.com", Student.Gender.MALE, "2002-11-03"),
                new StudentSeed("Aditya Nair", "aditya.nair@hostel.com", Student.Gender.MALE, "2001-12-19"),
                new StudentSeed("Siddharth Rao", "siddharth.rao@hostel.com", Student.Gender.MALE, "2002-02-27"),
                new StudentSeed("Manish Patel", "manish.patel@hostel.com", Student.Gender.MALE, "2001-09-14"),
                new StudentSeed("Deepak Yadav", "deepak.yadav@hostel.com", Student.Gender.MALE, "2002-06-01"),
                new StudentSeed("Nikhil Joshi", "nikhil.joshi@hostel.com", Student.Gender.MALE, "2002-03-08"),
                new StudentSeed("Harsh Gupta", "harsh.gupta@hostel.com", Student.Gender.MALE, "2001-10-25"),
                // B block (9)
                new StudentSeed("Suresh Reddy", "suresh.reddy@hostel.com", Student.Gender.MALE, "2003-01-02"),
                new StudentSeed("Ankit Chauhan", "ankit.chauhan@hostel.com", Student.Gender.MALE, "2003-04-18"),
                new StudentSeed("Varun Khanna", "varun.khanna@hostel.com", Student.Gender.MALE, "2002-12-06"),
                new StudentSeed("Pranav Iyer", "pranav.iyer@hostel.com", Student.Gender.MALE, "2003-02-11"),
                new StudentSeed("Kunal Desai", "kunal.desai@hostel.com", Student.Gender.MALE, "2002-09-30"),
                new StudentSeed("Mohit Agarwal", "mohit.agarwal@hostel.com", Student.Gender.MALE, "2003-05-16"),
                new StudentSeed("Tarun Bansal", "tarun.bansal@hostel.com", Student.Gender.MALE, "2002-08-21"),
                new StudentSeed("Ravi Shukla", "ravi.shukla@hostel.com", Student.Gender.MALE, "2003-03-09"),
                new StudentSeed("Gaurav Mishra", "gaurav.mishra@hostel.com", Student.Gender.MALE, "2002-07-28"),
                // C block (12)
                new StudentSeed("Priya Sharma", "student2@hostel.com", Student.Gender.FEMALE, "2002-03-20"),
                new StudentSeed("Neha Gupta", "neha.gupta@hostel.com", Student.Gender.FEMALE, "2002-11-05"),
                new StudentSeed("Sneha Kulkarni", "sneha.kulkarni@hostel.com", Student.Gender.FEMALE, "2001-06-17"),
                new StudentSeed("Ananya Das", "ananya.das@hostel.com", Student.Gender.FEMALE, "2002-01-26"),
                new StudentSeed("Ritika Jain", "ritika.jain@hostel.com", Student.Gender.FEMALE, "2002-09-08"),
                new StudentSeed("Pooja Singh", "pooja.singh@hostel.com", Student.Gender.FEMALE, "2001-11-29"),
                new StudentSeed("Kavya Menon", "kavya.menon@hostel.com", Student.Gender.FEMALE, "2002-05-13"),
                new StudentSeed("Divya Patel", "divya.patel@hostel.com", Student.Gender.FEMALE, "2002-08-02"),
                new StudentSeed("Meghna Chawla", "meghna.chawla@hostel.com", Student.Gender.FEMALE, "2001-07-24"),
                new StudentSeed("Shreya Roy", "shreya.roy@hostel.com", Student.Gender.FEMALE, "2002-04-09"),
                new StudentSeed("Tanvi Bhatt", "tanvi.bhatt@hostel.com", Student.Gender.FEMALE, "2002-10-31"),
                new StudentSeed("Ishita Saxena", "ishita.saxena@hostel.com", Student.Gender.FEMALE, "2001-12-16"),
                // D block (3)
                new StudentSeed("Sarah Jenkins", "sarah.jenkins@hostel.com", Student.Gender.FEMALE, "2000-09-14"),
                new StudentSeed("David Miller", "david.miller@hostel.com", Student.Gender.MALE, "2000-04-07"),
                new StudentSeed("Mei Ling Wong", "mei.wong@hostel.com", Student.Gender.FEMALE, "2001-02-19"));

        // room index (into `rooms` list) for each student in studentSeeds order
        int[] studentRoomIdx = {
                0, 0, 1, 1, 2, 2, 3, 4, 4, 5, 6, 6,      // A
                7, 7, 8, 8, 9, 10, 10, 10, 11,            // B
                12, 12, 13, 13, 14, 14, 15, 16, 16, 17, 17, 18, // C
                19, 20, 21                                   // D
        };

        List<Student> students = new ArrayList<>();
        for (int i = 0; i < studentSeeds.size(); i++) {
            StudentSeed s = studentSeeds.get(i);
            long phoneBase = 9800000000L + i * 7L;
            long parentBase = 9700000000L + i * 7L;
            User user = saveUser(s.name(), s.email(), User.Role.STUDENT, String.valueOf(phoneBase), password);
            Room room = rooms.get(studentRoomIdx[i]);
            Student student = studentRepository.save(Student.builder()
                    .user(user)
                    .room(room)
                    .enrollmentNo("ENR2024" + String.format("%03d", i + 1))
                    .parentContact(String.valueOf(parentBase))
                    .address(addresses[i % addresses.length])
                    .dateOfBirth(s.dob())
                    .gender(s.gender())
                    .build());
            students.add(student);
        }
        log.info("Students: " + students.size());

        // ============ COMPLAINTS ============
        // student index, title, description, category, status
        List<Complaint> complaints = new ArrayList<>();
        Object[][] complaintsData = {
                {0, "Fan not working", "Ceiling fan in room A-101 is not rotating and making a buzzing noise.", "ELECTRICAL", "IN_PROGRESS"},
                {0, "Water tap leaking", "Bathroom tap in A-101 leaks continuously.", "PLUMBING", "PENDING"},
                {2, "WiFi disconnects in A-Block", "WiFi signal drops every 10 minutes in room A-102.", "INTERNET", "RESOLVED"},
                {4, "Study chair broken leg", "Right leg of the study chair in A-103 is broken.", "FURNITURE", "PENDING"},
                {6, "Power socket sparking", "Top socket near the study table sparks on insertion.", "ELECTRICAL", "IN_PROGRESS"},
                {7, "Corridor light flickers", "A-Block 2nd floor corridor light flickers during the night.", "GENERAL", "IN_PROGRESS"},
                {9, "Water pressure very low", "Morning water pressure in A-202 is too low to shower.", "PLUMBING", "PENDING"},
                {10, "Window latch broken", "Window in A-203 does not close fully, lets cold air in.", "FURNITURE", "RESOLVED"},
                {12, "Common room AC not cooling", "B-Block common room AC blows only warm air.", "ELECTRICAL", "PENDING"},
                {13, "Locker keyhole jammed", "Personal locker in B-101 cannot be opened with the key.", "GENERAL", "REJECTED"},
                {15, "Bed creaks at night", "Upper bunk bed in B-102 makes loud creaking noises.", "FURNITURE", "IN_PROGRESS"},
                {17, "No hot water in bathroom", "Geyser in B-201 is not heating water.", "PLUMBING", "RESOLVED"},
                {19, "Dustbin not cleared", "Corridor dustbin has not been cleared for three days.", "GENERAL", "PENDING"},
                {21, "Cold food served at dinner", "Dinner served on Friday night was cold.", "MESS", "RESOLVED"},
                {21, "Mess table cleanliness", "Tables near the serving counter are often left unclean.", "MESS", "PENDING"},
                {23, "Bathroom exhaust fan dead", "Exhaust fan in C-102 bathroom does not run.", "ELECTRICAL", "IN_PROGRESS"},
                {25, "WiFi range poor in C-103", "Internet connectivity is very weak near the window side.", "INTERNET", "PENDING"},
                {27, "Reading lamp not working", "Bedside reading lamp in C-104 has a fused bulb.", "ELECTRICAL", "RESOLVED"},
                {28, "Bathroom tiles cracked", "Cracked tiles near the washbasin in C-201.", "PLUMBING", "PENDING"},
                {30, "Roti undercooked at lunch", "Roti served at lunch was undercooked.", "MESS", "REJECTED"},
                {31, "Study table wobbles", "Study table in C-202 wobbles while writing.", "FURNITURE", "IN_PROGRESS"},
                {32, "Water cooler not working", "C-Block 2nd floor water cooler is out of order.", "GENERAL", "PENDING"},
                {33, "Heater not working in winter", "Room heater in D-101 is not producing heat.", "ELECTRICAL", "RESOLVED"},
                {34, "Bathroom door lock broken", "Bathroom door lock in D-102 is broken from inside.", "GENERAL", "PENDING"}
        };
        for (Object[] c : complaintsData) {
            Student student = students.get((Integer) c[0]);
            Complaint.ComplaintStatus status = Complaint.ComplaintStatus.valueOf((String) c[4]);
            Complaint complaint = Complaint.builder()
                    .student(student)
                    .title((String) c[1])
                    .description((String) c[2])
                    .category(Complaint.ComplaintCategory.valueOf((String) c[3]))
                    .status(status)
                    .build();
            if (status == Complaint.ComplaintStatus.RESOLVED || status == Complaint.ComplaintStatus.REJECTED) {
                complaint.setResolvedAt(LocalDateTime.now().minusDays(3));
            }
            complaints.add(complaintRepository.save(complaint));
        }
        log.info("Complaints: " + complaints.size());

        // ============ LEAVE REQUESTS ============
        // student index, from, to, reason, status, approvedBy, remarks
        List<LeaveRequest> leaves = new ArrayList<>();
        Object[][] leavesData = {
                {0, "2026-08-01", "2026-08-07", "Family visit to hometown", "PENDING", null, null},
                {0, "2026-06-10", "2026-06-15", "Medical appointment", "APPROVED", "Mr. Sharma", "Approved with medical prescription"},
                {2, "2026-07-20", "2026-07-24", "Attending sister's wedding", "APPROVED", "Mr. Sharma", "Sanctioned leave"},
                {4, "2026-07-28", "2026-07-30", "Personal work", "REJECTED", "Mr. Sharma", "Insufficient notice period"},
                {6, "2026-08-12", "2026-08-16", "Competitive exam in Delhi", "PENDING", null, null},
                {7, "2026-06-25", "2026-06-28", "Family emergency", "APPROVED", "Mr. Sharma", "Emergency leave approved"},
                {9, "2026-08-05", "2026-08-09", "Internship interview in Pune", "PENDING", null, null},
                {10, "2026-07-10", "2026-07-12", "Graduation ceremony of sibling", "APPROVED", "Mr. Sharma", "Approved"},
                {12, "2026-08-03", "2026-08-08", "Goan trip with family", "PENDING", null, null},
                {13, "2026-06-18", "2026-06-20", "Dental surgery recovery", "APPROVED", "Dr. Rajesh Patel", "Medical certificate attached"},
                {15, "2026-07-15", "2026-07-17", "Railway exam in Bhopal", "REJECTED", "Dr. Rajesh Patel", "Clashes with hostel inspection"},
                {17, "2026-08-10", "2026-08-14", "Attending cousin's wedding", "PENDING", null, null},
                {19, "2026-06-05", "2026-06-09", "Trekking expedition - Himalayan Base Camp", "APPROVED", "Dr. Rajesh Patel", "Approved with safety briefing"},
                {20, "2026-08-15", "2026-08-18", "Visa interview at embassy", "PENDING", null, null},
                {21, "2026-07-02", "2026-07-05", "Attending a dance workshop", "APPROVED", "Mrs. Anjali Verma", "Approved"},
                {21, "2026-08-20", "2026-08-22", "Parents visiting, going to receive them", "PENDING", null, null},
                {23, "2026-06-22", "2026-06-26", "Hospitalization of mother", "APPROVED", "Mrs. Anjali Verma", "Urgent family leave"},
                {25, "2026-07-08", "2026-07-10", "Sibling's college admission in Bangalore", "APPROVED", "Mrs. Anjali Verma", "Approved"},
                {27, "2026-08-06", "2026-08-09", "Sangeet and wedding functions at home", "PENDING", null, null},
                {28, "2026-06-12", "2026-06-13", "Religious festival at home", "APPROVED", "Mrs. Anjali Verma", "Approved for festival"},
                {30, "2026-07-18", "2026-07-20", "Health checkup in Chennai", "REJECTED", "Mrs. Anjali Verma", "Please reschedule, mess duty assigned"},
                {31, "2026-08-11", "2026-08-15", "Attending brother's engagement", "PENDING", null, null},
                {33, "2026-07-25", "2026-08-02", "Family vacation - Europe trip", "APPROVED", "Mr. Ravi Menon", "Long leave approved"},
                {34, "2026-08-04", "2026-08-06", "Flight booking for hometown visit", "PENDING", null, null}
        };
        for (Object[] l : leavesData) {
            Student student = students.get((Integer) l[0]);
            LeaveRequest.LeaveStatus status = LeaveRequest.LeaveStatus.valueOf((String) l[4]);
            LeaveRequest leave = LeaveRequest.builder()
                    .student(student)
                    .fromDate((String) l[1])
                    .toDate((String) l[2])
                    .reason((String) l[3])
                    .status(status)
                    .approvedBy((String) l[5])
                    .remarks((String) l[6])
                    .build();
            if (status != LeaveRequest.LeaveStatus.PENDING) {
                leave.setResolvedAt(LocalDateTime.now().minusDays(5));
            }
            leaves.add(leaveRequestRepository.save(leave));
        }
        log.info("Leave requests: " + leaves.size());

        // ============ NOTICES ============
        Object[][] noticesData = {
                {"Hostel Annual Sports Meet 2026", "Registrations are open for Badminton, Cricket and Chess. Submit names to block warden before Aug 5.", "System Admin", "ALL", 25},
                {"Water Supply Shutdown", "Water supply in A & B Blocks will be shut on Sunday from 10 AM to 1 PM for tank cleaning.", "Mr. Sharma", "STUDENT", 6},
                {"Mess Menu Feedback Survey", "Please fill the weekly mess rating in the student dashboard before Friday 8 PM.", "System Admin", "STUDENT", 4},
                {"Monthly Warden Coordination Meeting", "All block wardens to assemble in Admin Conference Room on the 1st at 4 PM.", "System Admin", "WARDEN", 7},
                {"Electricity Maintenance - C Block", "Power supply in C Block will be interrupted on Aug 12 (9 AM - 12 PM) for panel maintenance.", "Mrs. Anjali Verma", "STUDENT", 5},
                {"Examination Period Quiet Hours", "Quiet hours (10 PM - 6 AM) strictly enforced across all blocks during exam week.", "System Admin", "ALL", 15},
                {"Hostel Fee Deadline Reminder", "All students are requested to pay the hostel fees by the 15th of every month.", "System Admin", "STUDENT", 10},
                {"Indoor Games Room Timings Updated", "The games room is now open from 5 PM to 11 PM on weekdays.", "Dr. Rajesh Patel", "ALL", 30},
                {"Holi Celebration in Common Hall", "Celebration on Mar 14 at 5 PM. Colors and snacks provided, register at reception.", "System Admin", "STUDENT", 12},
                {"Lost & Found Drive", "Unclaimed items from last month will be displayed at the reception on Aug 20.", "System Admin", "ALL", 18},
                {"Warden Duty Roster - August", "Weekly warden duty roster has been published. Check with admin office.", "System Admin", "WARDEN", 21},
                {"Mess Timing Change - Ramzan", "Sehri will be served at 4:30 AM and dinner extended to 10 PM during Ramzan.", "Mrs. Anjali Verma", "STUDENT", 20},
                {"WiFi Maintenance Night", "Hostel-wide WiFi will be down on Aug 18 from 1 AM to 3 AM for upgrades.", "System Admin", "ALL", 9},
                {"Blood Donation Camp", "A blood donation camp is organized on Aug 22 at the Main Campus Auditorium.", "System Admin", "STUDENT", 14},
                {"Room Inspection Notice", "Annual room inspections begin Aug 25. Ensure rooms are tidy.", "Mr. Sharma", "STUDENT", 16},
                {"Fire Safety Drill", "Mandatory fire safety drill for all residents on Aug 28 at 6 PM. Attendance required.", "System Admin", "ALL", 20},
                {"New Laundry Service", "A tie-up with QuickWash offers laundry at Rs. 60 per kg from next week.", "System Admin", "ALL", 35},
                {"Hostel Day Celebration", "Annual Hostel Day on Sep 5. Cultural events, food stalls and games.", "System Admin", "ALL", 28}
        };
        List<Notice> notices = new ArrayList<>();
        for (Object[] n : noticesData) {
            Notice notice = Notice.builder()
                    .title((String) n[0])
                    .content((String) n[1])
                    .postedBy((String) n[2])
                    .targetRole(Notice.TargetRole.valueOf((String) n[3]))
                    .expiresAt(LocalDateTime.now().plusDays((Integer) n[4]))
                    .build();
            notices.add(noticeRepository.save(notice));
        }
        log.info("Notices: " + notices.size());

        // ============ MESS FEEDBACK ============
        // student index, date, foodQuality, taste, cleanliness, comments, sentiment
        Object[][] feedbackData = {
                {0, "2026-07-25", 5, 4, 5, "Excellent Paneer Butter Masala today!", "POSITIVE"},
                {21, "2026-07-25", 4, 4, 4, "Clean dining area and prompt service.", "POSITIVE"},
                {2, "2026-07-24", 2, 2, 3, "Roti was undercooked during lunch.", "NEGATIVE"},
                {21, "2026-07-24", 3, 3, 4, "Average breakfast options.", "NEUTRAL"},
                {4, "2026-07-23", 4, 5, 5, "Loved the Dosa and Coconut Chutney!", "POSITIVE"},
                {6, "2026-07-23", 2, 3, 3, "Rice was dry and dal was bland.", "NEGATIVE"},
                {7, "2026-07-22", 4, 4, 4, "Good variety this week.", "POSITIVE"},
                {9, "2026-07-22", 3, 3, 4, "Okay, but serving queue is long.", "NEUTRAL"},
                {10, "2026-07-21", 5, 5, 5, "Best biryani I have had in hostel!", "POSITIVE"},
                {12, "2026-07-21", 2, 2, 2, "Sambhar was watery today.", "NEGATIVE"},
                {13, "2026-07-20", 4, 4, 5, "Tiffin service is very consistent.", "POSITIVE"},
                {15, "2026-07-20", 3, 4, 3, "Sweets on Sunday were nice.", "NEUTRAL"},
                {17, "2026-07-19", 5, 5, 4, "Chole Bhature was superb!", "POSITIVE"},
                {19, "2026-07-19", 2, 3, 4, "Breakfast repeats every day.", "NEGATIVE"},
                {20, "2026-07-18", 3, 3, 3, "Nothing special, but edible.", "NEUTRAL"},
                {21, "2026-07-18", 5, 5, 5, "Paneer dishes are the best!", "POSITIVE"},
                {23, "2026-07-17", 4, 4, 4, "Salad counter is a good addition.", "POSITIVE"},
                {25, "2026-07-17", 2, 2, 3, "Fish was stale in the evening.", "NEGATIVE"},
                {27, "2026-07-16", 3, 4, 4, "Pulav was nice, dessert average.", "NEUTRAL"},
                {28, "2026-07-16", 4, 5, 4, "Loved the lemon rice!", "POSITIVE"},
                {30, "2026-07-15", 2, 3, 3, "Roti hard and cold.", "NEGATIVE"},
                {31, "2026-07-15", 3, 3, 4, "Fine overall, more fruits please.", "NEUTRAL"},
                {32, "2026-07-14", 5, 4, 5, "Dinner pasta was restaurant quality!", "POSITIVE"},
                {33, "2026-07-14", 4, 4, 4, "International options are appreciated.", "POSITIVE"},
                {34, "2026-07-13", 3, 3, 3, "Decent, could add more greens.", "NEUTRAL"},
                {0, "2026-07-13", 4, 4, 4, "Good consistency this week.", "POSITIVE"},
                {2, "2026-07-12", 2, 2, 3, "Curry was too salty.", "NEGATIVE"},
                {4, "2026-07-12", 4, 4, 5, "Morning poha was great.", "POSITIVE"},
                {6, "2026-07-11", 3, 3, 4, "Average but hygienic.", "NEUTRAL"},
                {21, "2026-07-11", 4, 4, 4, "Better than last week!", "POSITIVE"}
        };
        for (Object[] f : feedbackData) {
            Student student = students.get((Integer) f[0]);
            messFeedbackRepository.save(MessFeedback.builder()
                    .student(student)
                    .date((String) f[1])
                    .foodQualityRating((Integer) f[2])
                    .tasteRating((Integer) f[3])
                    .cleanlinessRating((Integer) f[4])
                    .comments((String) f[5])
                    .sentiment(MessFeedback.Sentiment.valueOf((String) f[6]))
                    .build());
        }
        log.info("Mess feedback records: " + feedbackData.length);

        // ============ LOST & FOUND ============
        // title, description, category(lost/found), status, location, contactInfo, student index
        Object[][] lostFoundData = {
                {"Blue Steel Water Bottle", "Milton blue insulated bottle forgotten near Table 4 in the Mess Hall.", "lost", "LOST", "Mess Hall", "student@hostel.com", 0},
                {"Casio Scientific Calculator", "Black Casio FX-991EX with sticker 'Priya' found in the C-Block Study Room.", "found", "FOUND", "C-Block Study Room", "warden2@hostel.com", 21},
                {"Noise Wireless Earbuds", "White earbuds case lost near the A-Block entrance.", "lost", "LOST", "A-Block Gate", "arjun.mehta@hostel.com", 1},
                {"Set of Keys with Keychain", "3 keys on a leather keychain found on a bench near the Sports Complex.", "found", "RESOLVED", "Sports Complex", "warden@hostel.com", 4},
                {"Black Leather Wallet", "Black leather wallet with ID card lost in the Library.", "lost", "LOST", "Central Library", "vikram.malhotra@hostel.com", 3},
                {"Pencil Case with Stationery", "Blue pencil case with pens and highlighters found in B-201 common room.", "found", "FOUND", "B-Block Common Room", "ravi.shukla@hostel.com", 19},
                {"Umbrella - Red Polka Dot", "Red polka dot umbrella left at the Mess Hall entrance.", "lost", "LOST", "Mess Hall Entrance", "neha.gupta@hostel.com", 22},
                {"Samsung Fast Charger", "25W Samsung charger lost in the study room on A floor 2.", "lost", "LOST", "A-Block Study Room", "siddharth.rao@hostel.com", 7},
                {"Sports Water Bottle", "Grey sports bottle found near the basketball court.", "found", "FOUND", "Basketball Court", "kunal.desai@hostel.com", 16},
                {"Silver Analogue Watch", "Silver Casio watch found in the washroom on C floor 1.", "found", "RESOLVED", "C-Block Washroom", "warden2@hostel.com", 23},
                {"Laptop Charger - HP", "HP laptop adapter lost in the seminar hall.", "lost", "LOST", "Seminar Hall", "kavya.menon@hostel.com", 27},
                {"Yellow Notebook", "Yellow grid notebook with physics notes found in the Library.", "found", "FOUND", "Central Library", "tanvi.bhatt@hostel.com", 31},
                {"Reading Glasses - Black Frame", "Black frame reading glasses lost near the reception.", "lost", "LOST", "Hostel Reception", "shreya.roy@hostel.com", 30},
                {"Gym Towel", "Blue gym towel found in the common room.", "found", "FOUND", "Common Room", "gaurav.mishra@hostel.com", 20},
                {"USB Flash Drive 32GB", "Black 32GB pen drive lost in the computer lab.", "lost", "LOST", "Computer Lab", "divya.patel@hostel.com", 28},
                {"Spectacles - Blue Frame", "Blue frame spectacles found on the mess table 6.", "found", "RESOLVED", "Mess Hall", "warden@hostel.com", 25},
                {"Hoodie - Grey", "Grey Nike hoodie lost in the sports ground during practice.", "lost", "LOST", "Sports Ground", "tarun.bansal@hostel.com", 18},
                {"Keycard - Room D-102", "White keycard for D-102 found at the D-Block entrance.", "found", "FOUND", "D-Block Entrance", "warden4@hostel.com", 34},
                {"Water Bottle - Green", "Green bottle found in the auditorium after the seminar.", "found", "FOUND", "Auditorium", "mei.wong@hostel.com", 35},
                {"Earphones - Wired", "Black wired earphones lost in the C-Block corridor.", "lost", "LOST", "C-Block Corridor", "pooja.singh@hostel.com", 26},
                {"Board Game - Ludo", "Ludo board with pieces found in the games room.", "found", "FOUND", "Games Room", "suresh.reddy@hostel.com", 12},
                {"Black Sunglasses", "Ray-Ban style black sunglasses lost in the garden area.", "lost", "LOST", "Hostel Garden", "sarah.jenkins@hostel.com", 33}
        };
        for (Object[] lf : lostFoundData) {
            lostAndFoundRepository.save(LostAndFound.builder()
                    .title((String) lf[0])
                    .description((String) lf[1])
                    .category((String) lf[2])
                    .status(LostAndFound.LostFoundStatus.valueOf((String) lf[3]))
                    .location((String) lf[4])
                    .contactInfo((String) lf[5])
                    .reportedBy(students.get((Integer) lf[6]))
                    .build());
        }
        log.info("Lost & found records: " + lostFoundData.length);

        // ============ MARKETPLACE ============
        // title, description, price, category(lowercase), status, student index
        Object[][] marketplaceData = {
                {"Computer Science Core Textbooks (4th Sem)", "Includes OS, DBMS, Algorithms and Networks books in mint condition.", 650.0, "books", "AVAILABLE", 0},
                {"Adjustable LED Study Desk Lamp", "Wipro 10W rechargeable LED lamp with 3 color temperatures.", 350.0, "electronics", "AVAILABLE", 0},
                {"Hero Octane Mountain Bicycle", "21-gear mountain bike, recently serviced, new brake pads.", 3200.0, "sports", "AVAILABLE", 4},
                {"Ergonomic Mesh Office Chair", "High-back mesh chair with lumbar support.", 1200.0, "furniture", "AVAILABLE", 19},
                {"Sony Extra Bass Headphones", "Over-ear wired headphones with punchy bass and mic.", 800.0, "electronics", "SOLD", 21},
                {"Data Structures Made Easy", "Well-maintained DSA book with solved problems.", 150.0, "books", "AVAILABLE", 1},
                {"Gym Dumbbell Set (2 x 5kg)", "Rubber coated dumbbells with stand.", 900.0, "sports", "AVAILABLE", 12},
                {"Hooded Sweatshirt - Navy Blue", "Brand new, size L, worn twice.", 500.0, "clothing", "AVAILABLE", 2},
                {"HP Wireless Mouse", "HP X3000 wireless mouse, barely used.", 300.0, "electronics", "SOLD", 7},
                {"Study Table with Drawer", "Compact wooden study table, 3x2 feet.", 1800.0, "furniture", "AVAILABLE", 9},
                {"Mathematics for Machine Learning", "Oxford University Press textbook, like new.", 420.0, "books", "AVAILABLE", 3},
                {"Cassette Player + 10 Tapes", "Vintage Sony walkman with old Bollywood tapes.", 250.0, "other", "AVAILABLE", 6},
                {"Portable Bluetooth Speaker", "Boat Stone 1200, 12W, great bass.", 700.0, "electronics", "SOLD", 13},
                {"Bedside Table Lamp", "Warm white LED lamp with wooden base.", 450.0, "furniture", "AVAILABLE", 15},
                {"Formal Shirts Pack (3)", "Mango brand, size M, never worn.", 900.0, "clothing", "AVAILABLE", 17},
                {"Calculus Textbook", "Thomas' Calculus 14th edition, minor highlights.", 380.0, "books", "SOLD", 20},
                {"Cricket Bat - MRF", "Kashmir willow bat, used for one season.", 1100.0, "sports", "AVAILABLE", 12},
                {"Mini Fridge 45L", "Godrej mini fridge for room use, working perfectly.", 4500.0, "other", "AVAILABLE", 23},
                {"Wired Mechanical Keyboard", "Redragon K552 with RGB lighting.", 1200.0, "electronics", "AVAILABLE", 25},
                {"Wall Art Poster Set (4)", "Aesthetic A3 posters for room decor.", 200.0, "other", "AVAILABLE", 27},
                {"Electric Kettle 1.5L", "Prestige kettle, 1500W, lightly used.", 350.0, "electronics", "SOLD", 28},
                {"Yoga Mat", "6mm anti-slip mat with carry strap.", 320.0, "sports", "AVAILABLE", 30},
                {"Sketch Pens Set (48)", "Camel artist sketch pens, sealed pack.", 280.0, "other", "AVAILABLE", 31},
                {"Jeans - Levi's 512", "Size 32, slim fit, good condition.", 650.0, "clothing", "AVAILABLE", 33},
                {"Running Shoes - Asics", "Size 9, used for 3 months, very comfortable.", 1400.0, "sports", "SOLD", 34},
                {"Quantitative Aptitude Guide", "For campus placements, latest edition.", 220.0, "books", "AVAILABLE", 35}
        };
        for (Object[] m : marketplaceData) {
            marketplaceItemRepository.save(MarketplaceItem.builder()
                    .title((String) m[0])
                    .description((String) m[1])
                    .price((Double) m[2])
                    .category((String) m[3])
                    .status(MarketplaceItem.ItemStatus.valueOf((String) m[4]))
                    .seller(students.get((Integer) m[5]))
                    .build());
        }
        log.info("Marketplace items: " + marketplaceData.length);

        // ============ AUDIT LOGS ============
        seedAuditLogs(admin1, admin2, wardenA, wardenB, wardenC, wardenD,
                students, rooms, blocks, complaints, leaves, notices);
        log.info("Audit logs: " + auditLogRepository.count());
    }

    private void seedAuditLogs(User admin1, User admin2,
                               Warden wardenA, Warden wardenB, Warden wardenC, Warden wardenD,
                               List<Student> students, List<Room> rooms, List<HostelBlock> blocks,
                               List<Complaint> complaints, List<LeaveRequest> leaves, List<Notice> notices) {
        LocalDateTime now = LocalDateTime.now();

        record AuditSeed(String action, AuditSeverity severity, Long userId, String performedBy,
                         String role, String targetType, Long targetId, String details, int daysAgo, int hoursAgo) {
        }

        List<AuditSeed> seeds = new ArrayList<>();
        int d = 0; // helper index for day spread

        // Admins
        seeds.add(new AuditSeed("HOSTEL_CREATED", AuditSeverity.INFO, admin1.getId(), "admin@hostel.com", "ADMIN", "HOSTEL", null,
                "Hostel management system initialized with demo dataset", 30, 2));
        seeds.add(new AuditSeed("LOGIN_SUCCESS", AuditSeverity.INFO, admin1.getId(), "admin@hostel.com", "ADMIN", "AUTH", admin1.getId(),
                "Admin logged into system", 30, 1));
        seeds.add(new AuditSeed("LOGIN_SUCCESS", AuditSeverity.INFO, admin1.getId(), "admin@hostel.com", "ADMIN", "AUTH", admin1.getId(),
                "Admin logged into system", 28, 5));
        seeds.add(new AuditSeed("LOGIN_FAILED", AuditSeverity.WARNING, null, "admin@hostel.com", "ADMIN", "AUTH", null,
                "Failed login attempt with invalid password", 27, 3));
        seeds.add(new AuditSeed("LOGIN_SUCCESS", AuditSeverity.INFO, admin2.getId(), "admin2@hostel.com", "ADMIN", "AUTH", admin2.getId(),
                "Admin logged into system", 25, 6));
        seeds.add(new AuditSeed("BLOCK_CREATED", AuditSeverity.INFO, admin1.getId(), "admin@hostel.com", "ADMIN", "HOSTEL_BLOCK", blocks.get(1).getId(),
                "Created B Wing - Junior Boys block", 29, 4));

        // Wardens
        seeds.add(new AuditSeed("WARDEN_CREATED", AuditSeverity.INFO, admin1.getId(), "admin@hostel.com", "ADMIN", "USER", wardenA.getUser().getId(),
                "Created warden Mr. Sharma assigned to A-BLOCK", 29, 3));
        seeds.add(new AuditSeed("WARDEN_CREATED", AuditSeverity.INFO, admin1.getId(), "admin@hostel.com", "ADMIN", "USER", wardenC.getUser().getId(),
                "Created warden Mrs. Anjali Verma assigned to C-BLOCK", 29, 2));
        seeds.add(new AuditSeed("WARDEN_CREATED", AuditSeverity.INFO, admin1.getId(), "admin@hostel.com", "ADMIN", "USER", wardenB.getUser().getId(),
                "Created warden Dr. Rajesh Patel assigned to B-BLOCK", 28, 4));
        seeds.add(new AuditSeed("WARDEN_CREATED", AuditSeverity.INFO, admin1.getId(), "admin@hostel.com", "ADMIN", "USER", wardenD.getUser().getId(),
                "Created warden Mr. Ravi Menon assigned to D-BLOCK", 28, 3));

        // Rooms
        seeds.add(new AuditSeed("ROOM_CREATED", AuditSeverity.INFO, admin1.getId(), "admin@hostel.com", "ADMIN", "ROOM", rooms.get(0).getId(),
                "Room A-101 added to A-BLOCK", 27, 6));
        seeds.add(new AuditSeed("ROOM_CREATED", AuditSeverity.INFO, admin1.getId(), "admin@hostel.com", "ADMIN", "ROOM", rooms.get(12).getId(),
                "Room C-101 added to C-BLOCK", 27, 5));
        seeds.add(new AuditSeed("ROOM_UPDATED", AuditSeverity.INFO, admin1.getId(), "admin@hostel.com", "ADMIN", "ROOM", rooms.get(22).getId(),
                "Updated rent of B-201", 24, 3));
        seeds.add(new AuditSeed("BLOCK_CREATED", AuditSeverity.INFO, admin1.getId(), "admin@hostel.com", "ADMIN", "HOSTEL_BLOCK", blocks.get(3).getId(),
                "Created D Wing - PG / International block", 26, 4));

        // Students + allocation
        seeds.add(new AuditSeed("STUDENT_CREATED", AuditSeverity.INFO, admin1.getId(), "admin@hostel.com", "ADMIN", "STUDENT", students.get(0).getId(),
                "Registered student Rahul Kumar (ENR2024001)", 26, 3));
        seeds.add(new AuditSeed("STUDENT_CREATED", AuditSeverity.INFO, admin1.getId(), "admin@hostel.com", "ADMIN", "STUDENT", students.get(21).getId(),
                "Registered student Priya Sharma (ENR2024022)", 26, 2));
        seeds.add(new AuditSeed("ROOM_ALLOCATED", AuditSeverity.INFO, wardenA.getUser().getId(), "warden@hostel.com", "WARDEN", "ROOM", rooms.get(0).getId(),
                "Room A-101 allocated to Rahul Kumar", 25, 6));
        seeds.add(new AuditSeed("ROOM_ALLOCATED", AuditSeverity.INFO, wardenA.getUser().getId(), "warden@hostel.com", "WARDEN", "ROOM", rooms.get(0).getId(),
                "Room A-101 allocated to Arjun Mehta", 25, 5));
        seeds.add(new AuditSeed("ROOM_ALLOCATED", AuditSeverity.INFO, wardenC.getUser().getId(), "warden2@hostel.com", "WARDEN", "ROOM", rooms.get(12).getId(),
                "Room C-101 allocated to Priya Sharma", 24, 6));
        seeds.add(new AuditSeed("ROOM_ALLOCATED", AuditSeverity.INFO, wardenB.getUser().getId(), "warden3@hostel.com", "WARDEN", "ROOM", rooms.get(7).getId(),
                "Room B-101 allocated to Suresh Reddy", 23, 4));
        seeds.add(new AuditSeed("ROOM_ALLOCATED", AuditSeverity.INFO, wardenD.getUser().getId(), "warden4@hostel.com", "WARDEN", "ROOM", rooms.get(19).getId(),
                "Room D-101 allocated to Sarah Jenkins", 22, 3));
        seeds.add(new AuditSeed("ROOM_TRANSFERRED", AuditSeverity.WARNING, wardenA.getUser().getId(), "warden@hostel.com", "WARDEN", "ROOM", rooms.get(5).getId(),
                "Deepak Yadav transferred from A-104 to A-202", 15, 3));

        // Leaves
        seeds.add(new AuditSeed("LEAVE_APPLIED", AuditSeverity.INFO, students.get(0).getUser().getId(), "student@hostel.com", "STUDENT", "LEAVE", leaves.get(1).getId(),
                "Leave application for medical appointment", 21, 5));
        seeds.add(new AuditSeed("LEAVE_APPROVED", AuditSeverity.INFO, wardenA.getUser().getId(), "warden@hostel.com", "WARDEN", "LEAVE", leaves.get(1).getId(),
                "Approved medical leave for Rahul Kumar", 20, 4));
        seeds.add(new AuditSeed("LEAVE_APPLIED", AuditSeverity.INFO, students.get(2).getUser().getId(), "amit.singh@hostel.com", "STUDENT", "LEAVE", leaves.get(2).getId(),
                "Leave application for sister's wedding", 19, 6));
        seeds.add(new AuditSeed("LEAVE_APPROVED", AuditSeverity.INFO, wardenA.getUser().getId(), "warden@hostel.com", "WARDEN", "LEAVE", leaves.get(2).getId(),
                "Approved wedding leave for Amit Singh", 18, 3));
        seeds.add(new AuditSeed("LEAVE_REJECTED", AuditSeverity.WARNING, wardenA.getUser().getId(), "warden@hostel.com", "WARDEN", "LEAVE", leaves.get(3).getId(),
                "Rejected personal leave for Vikram Malhotra - insufficient notice", 17, 5));
        seeds.add(new AuditSeed("LEAVE_APPROVED", AuditSeverity.INFO, wardenC.getUser().getId(), "warden2@hostel.com", "WARDEN", "LEAVE", leaves.get(15).getId(),
                "Approved urgent family leave for Priya Sharma", 12, 3));

        // Complaints
        seeds.add(new AuditSeed("COMPLAINT_CREATED", AuditSeverity.INFO, students.get(0).getUser().getId(), "student@hostel.com", "STUDENT", "COMPLAINT", complaints.get(0).getId(),
                "Complaint: Fan not working", 14, 6));
        seeds.add(new AuditSeed("COMPLAINT_CREATED", AuditSeverity.INFO, students.get(2).getUser().getId(), "amit.singh@hostel.com", "STUDENT", "COMPLAINT", complaints.get(2).getId(),
                "Complaint: WiFi disconnects in A-Block", 13, 5));
        seeds.add(new AuditSeed("COMPLAINT_RESOLVED", AuditSeverity.INFO, wardenA.getUser().getId(), "warden@hostel.com", "WARDEN", "COMPLAINT", complaints.get(2).getId(),
                "Resolved WiFi complaint for Amit Singh", 10, 4));
        seeds.add(new AuditSeed("COMPLAINT_CREATED", AuditSeverity.INFO, students.get(21).getUser().getId(), "student2@hostel.com", "STUDENT", "COMPLAINT", complaints.get(13).getId(),
                "Complaint: Cold food served at dinner", 9, 6));
        seeds.add(new AuditSeed("COMPLAINT_RESOLVED", AuditSeverity.INFO, wardenC.getUser().getId(), "warden2@hostel.com", "WARDEN", "COMPLAINT", complaints.get(13).getId(),
                "Resolved mess complaint for Priya Sharma", 8, 3));

        // Notices
        seeds.add(new AuditSeed("NOTICE_CREATED", AuditSeverity.INFO, admin1.getId(), "admin@hostel.com", "ADMIN", "NOTICE", notices.get(0).getId(),
                "Posted announcement: Hostel Annual Sports Meet 2026", 7, 4));
        seeds.add(new AuditSeed("NOTICE_CREATED", AuditSeverity.INFO, wardenA.getUser().getId(), "warden@hostel.com", "WARDEN", "NOTICE", notices.get(1).getId(),
                "Posted announcement: Water Supply Shutdown", 6, 3));

        // Feedback / lost-found / marketplace
        seeds.add(new AuditSeed("MESS_FEEDBACK_SUBMITTED", AuditSeverity.INFO, students.get(0).getUser().getId(), "student@hostel.com", "STUDENT", "MESS_FEEDBACK", null,
                "Submitted mess feedback with 5-star food quality rating", 5, 2));
        seeds.add(new AuditSeed("LOST_ITEM_REPORTED", AuditSeverity.INFO, students.get(0).getUser().getId(), "student@hostel.com", "STUDENT", "LOST_FOUND", null,
                "Reported lost item: Blue Steel Water Bottle", 4, 5));
        seeds.add(new AuditSeed("FOUND_ITEM_REPORTED", AuditSeverity.INFO, students.get(21).getUser().getId(), "student2@hostel.com", "STUDENT", "LOST_FOUND", null,
                "Reported found item: Casio Scientific Calculator", 4, 2));
        seeds.add(new AuditSeed("MARKETPLACE_ITEM_CREATED", AuditSeverity.INFO, students.get(0).getUser().getId(), "student@hostel.com", "STUDENT", "MARKETPLACE", null,
                "Listed item: Computer Science Core Textbooks", 3, 6));
        seeds.add(new AuditSeed("MARKETPLACE_ITEM_CREATED", AuditSeverity.INFO, students.get(4).getUser().getId(), "rohan.sharma@hostel.com", "STUDENT", "MARKETPLACE", null,
                "Listed item: Hero Octane Mountain Bicycle", 2, 4));

        // Recent logins
        seeds.add(new AuditSeed("LOGIN_SUCCESS", AuditSeverity.INFO, admin1.getId(), "admin@hostel.com", "ADMIN", "AUTH", admin1.getId(),
                "Admin logged into system", 1, 2));
        seeds.add(new AuditSeed("LOGIN_SUCCESS", AuditSeverity.INFO, wardenA.getUser().getId(), "warden@hostel.com", "WARDEN", "AUTH", wardenA.getUser().getId(),
                "Warden logged into system", 1, 1));
        seeds.add(new AuditSeed("LOGIN_SUCCESS", AuditSeverity.INFO, students.get(0).getUser().getId(), "student@hostel.com", "STUDENT", "AUTH", students.get(0).getUser().getId(),
                "Student logged into system", 0, 5));
        seeds.add(new AuditSeed("LOGIN_FAILED", AuditSeverity.WARNING, null, "student2@hostel.com", "STUDENT", "AUTH", null,
                "Failed login attempt with wrong password", 0, 2));

        for (AuditSeed s : seeds) {
            auditLogRepository.save(AuditLog.builder()
                    .action(s.action())
                    .severity(s.severity())
                    .performedByUserId(s.userId())
                    .performedBy(s.performedBy())
                    .performedByRole(s.role())
                    .targetType(s.targetType())
                    .targetId(s.targetId())
                    .details(s.details())
                    .ipAddress("127.0.0.1")
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .createdAt(now.minusDays(s.daysAgo()).minusHours(s.hoursAgo()))
                    .build());
        }
    }

    private void verify() {
        log.info("----------------- DEMO DATA VERIFICATION -----------------");
        log.info("users             : " + userRepository.count());
        log.info("admins            : " + adminRepository.count());
        log.info("wardens           : " + wardenRepository.count());
        log.info("students          : " + studentRepository.count());
        log.info("hostel_blocks     : " + hostelBlockRepository.count());
        log.info("rooms             : " + roomRepository.count());
        log.info("complaints        : " + complaintRepository.count());
        log.info("leave_requests    : " + leaveRequestRepository.count());
        log.info("notices           : " + noticeRepository.count());
        log.info("mess_feedbacks    : " + messFeedbackRepository.count());
        log.info("lost_and_found    : " + lostAndFoundRepository.count());
        log.info("marketplace_items : " + marketplaceItemRepository.count());
        log.info("audit_logs        : " + auditLogRepository.count());
        log.info("----------------------------------------------------------");
        log.info("Demo credentials:");
        log.info("  ADMIN   -> admin@hostel.com  / " + DEMO_PASSWORD);
        log.info("  WARDEN  -> warden@hostel.com / " + DEMO_PASSWORD);
        log.info("  STUDENT -> student@hostel.com / " + DEMO_PASSWORD);
        log.info("  STUDENT -> student2@hostel.com / " + DEMO_PASSWORD);
    }

    private User saveUser(String name, String email, User.Role role, String phone, String password) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(User.builder()
                        .name(name)
                        .email(email)
                        .password(password)
                        .role(role)
                        .phone(phone)
                        .build()));
    }

    private HostelBlock saveBlock(String name, String code, String address) {
        return hostelBlockRepository.save(HostelBlock.builder()
                .name(name)
                .code(code)
                .address(address)
                .build());
    }

    private Warden saveWarden(String name, String email, String phone, HostelBlock block,
                              String qualification, String password) {
        User user = saveUser(name, email, User.Role.WARDEN, phone, password);
        return wardenRepository.save(Warden.builder()
                .user(user)
                .block(block)
                .qualification(qualification)
                .build());
    }

    private List<Room> saveRooms(HostelBlock block, List<Object[]> roomData) {
        List<Room> rooms = new ArrayList<>();
        for (Object[] r : roomData) {
            rooms.add(roomRepository.save(Room.builder()
                    .roomNo((String) r[0])
                    .block(block)
                    .floor((Integer) r[1])
                    .capacity((Integer) r[2])
                    .rent((Double) r[3])
                    .status(Room.RoomStatus.valueOf((String) r[4]))
                    .occupants((Integer) r[5])
                    .build()));
        }
        return rooms;
    }
}
