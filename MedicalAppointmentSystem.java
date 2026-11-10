import java.util.Scanner;

/*
 * =======================================================================
 *  MEDICAL APPOINTMENT BOOKING SYSTEM  (Doctor Channelling System)
 *  
 *
 *  Data structures used (matches the design specification / report):
 *    - Singly Linked List  -> Patients, Doctors, Appointments
 *     Queue ADT (FIFO)    -> Waiting list handling, implemented on top of
 *                              the Appointment linked list ("Waiting" status)
 *    - Stack ADT (LIFO)    -> Patient History Stack, used to retrieve
 *                              registered patients from newest to oldest
 *                             
 *    - Sorting Algorithms  -> Bubble Sort & Selection Sort, used to sort
 *                              patients by age
 *
 *  All operations use manual (custom) implementations of the data
 *  structures - no java.util.LinkedList / Stack / Queue / Collections.sort
 *  is used, since the whole point of the assignment is to build and
 *  reason about these structures ourselves.
 * =============================================================
 */
public class MedicalAppointmentSystem {

    public static void main(String[] args) {
        DoctorChannelingSystem system = new DoctorChannelingSystem();
        system.runMenu();
    }
}

/* ==========================================================================
 *  NODE CLASSES
 * ========================================================================== */

class PatientNode {
    String patientID;
    String name;
    int age;
    String mobileNumber;
    String email;
    String city;
    String medicalHistory;
    PatientNode next;

    PatientNode(String patientID, String name, int age, String mobileNumber,
                String email, String city, String medicalHistory) {
        this.patientID = patientID;
        this.name = name;
        this.age = age;
        this.mobileNumber = mobileNumber;
        this.email = email;
        this.city = city;
        this.medicalHistory = medicalHistory;
        this.next = null;
    }

    @Override
    public String toString() {
        return "Patient ID: " + patientID +
                " | Name: " + name +
                " | Age: " + age +
                " | Mobile: " + mobileNumber +
                " | City: " + city;
    }
}

class DoctorNode {
    String doctorID;
    String name;
    String specialization;
    double consultationFee;
    int totalSlots;
    int bookedSlots;
    DoctorNode next;

    DoctorNode(String doctorID, String name, String specialization,
               double consultationFee, int totalSlots) {
        this.doctorID = doctorID;
        this.name = name;
        this.specialization = specialization;
        this.consultationFee = consultationFee;
        this.totalSlots = totalSlots;
        this.bookedSlots = 0;
        this.next = null;
    }

    int availableSlots() {
        return totalSlots - bookedSlots;
    }

    @Override
    public String toString() {
        return "Doctor ID: " + doctorID +
                " | Name: " + name +
                " | Specialization: " + specialization +
                " | Fee: " + consultationFee +
                " | Total Slots: " + totalSlots +
                " | Booked: " + bookedSlots +
                " | Available: " + availableSlots();
    }
}

/*
 * Status values used for AppointmentNode.status:
 *   "Scheduled" - patient has a confirmed slot with the doctor
 *   "Waiting"   - patient is in the FIFO waiting queue for that doctor
 *   "Cancelled" - appointment was cancelled
 */
class AppointmentNode {
    String appointmentID;
    String patientID;
    String doctorID;
    int slotNumber;      // 0 while the patient is only "Waiting"
    String status;
    AppointmentNode next;

    AppointmentNode(String appointmentID, String patientID, String doctorID,
                     int slotNumber, String status) {
        this.appointmentID = appointmentID;
        this.patientID = patientID;
        this.doctorID = doctorID;
        this.slotNumber = slotNumber;
        this.status = status;
        this.next = null;
    }

    @Override
    public String toString() {
        return "Appointment ID: " + appointmentID +
                " | Patient ID: " + patientID +
                " | Doctor ID: " + doctorID +
                " | Slot: " + (slotNumber == 0 ? "-" : slotNumber) +
                " | Status: " + status;
    }
}

/* =====================================================================
 *  STACK ADT  (Imperative definition: Push, Pop, Peek, isEmpty, Size)
 *  Implemented using a linked list - used only for the Patient History
 *  Stack so that the newest registered patient can always be retrieved
 *  first (LIFO)
 * ========================================================================== */
class StackNode {
    PatientNode patient;
    StackNode next;

    StackNode(PatientNode patient) {
        this.patient = patient;
        this.next = null;
    }
}

class PatientHistoryStack {
    private StackNode top;   // encapsulated - hidden from outside classes
    private int size;

    PatientHistoryStack() {
        top = null;
        size = 0;
    }

    // push() - O(1)
    void push(PatientNode patient) {
        StackNode newNode = new StackNode(patient);
        newNode.next = top;
        top = newNode;
        size++;
    }

    // pop() - O(1)
    PatientNode pop() {
        if (isEmpty()) {
            throw new RuntimeException("Stack Underflow: No patient history available.");
        }
        PatientNode popped = top.patient;
        top = top.next;
        size--;
        return popped;
    }

    // peek() - O(1)
    PatientNode peek() {
        if (isEmpty()) {
            throw new RuntimeException("Stack is empty.");
        }
        return top.patient;
    }

    // isEmpty() - O(1)
    boolean isEmpty() {
        return top == null;
    }

    // size() - O(1)
    int size() {
        return size;
    }

    // Non-destructive traversal, newest to oldest (does not pop the stack)
    void displayNewestToOldest() {
        if (isEmpty()) {
            System.out.println("No registered patients in history yet.");
            return;
        }
        System.out.println("\n--- Patients (Newest to Oldest) ---");
        StackNode current = top;
        int count = 1;
        while (current != null) {
            System.out.println(count + ". " + current.patient);
            current = current.next;
            count++;
        }
    }
}

/* =========================================================================
 *  MAIN SYSTEM CLASS
 * ========================================================================== */
class DoctorChannelingSystem {

    private PatientNode patientHead;
    private DoctorNode doctorHead;
    private AppointmentNode appointmentHead;
    private final PatientHistoryStack historyStack;
    private final Scanner sc;

    private int patientCounter;
    private int doctorCounter;
    private int appointmentCounter;

    DoctorChannelingSystem() {
        patientHead = null;
        doctorHead = null;
        appointmentHead = null;
        historyStack = new PatientHistoryStack();
        sc = new Scanner(System.in);
        patientCounter = 1;
        doctorCounter = 1;
        appointmentCounter = 1;
    }

    /* ---------------------------------------------------------------
     *  MENU
     * -------------------------------------------------------------- */
    void runMenu() {
        boolean running = true;
        while (running) {
            System.out.println("\n=====================================================");
            System.out.println("   MEDICAL APPOINTMENT BOOKING SYSTEM - MAIN MENU");
            System.out.println("=====================================================");
            System.out.println(" 1.  Register Patient");
            System.out.println(" 2.  Display Patients");
            System.out.println(" 3.  Search Patient");
            System.out.println(" 4.  Register Doctor");
            System.out.println(" 5.  Display Doctors");
            System.out.println(" 6.  Search Doctor");
            System.out.println(" 7.  Book Appointment");
            System.out.println(" 8.  Cancel Appointment");
            System.out.println(" 9.  Request Reschedule");
            System.out.println("10.  Display All Appointments");
            System.out.println("11.  Display Waiting List");
            System.out.println("12.  View Patient Registration History (Newest to Oldest)");
            System.out.println("13.  Sort Patients by Age - Bubble Sort");
            System.out.println("14.  Sort Patients by Age - Selection Sort");
            System.out.println("15.  Exit");
            System.out.println("=====================================================");
            System.out.print("Enter your choice: ");

            int choice = readInt();

            switch (choice) {
                case 1: registerPatient(); break;
                case 2: displayPatients(); break;
                case 3: searchPatient(); break;
                case 4: registerDoctor(); break;
                case 5: displayDoctors(); break;
                case 6: searchDoctor(); break;
                case 7: bookAppointment(); break;
                case 8: cancelAppointment(); break;
                case 9: requestReschedule(); break;
                case 10: displayAppointments(); break;
                case 11: displayWaitingList(); break;
                case 12: historyStack.displayNewestToOldest(); break;
                case 13: sortPatientsBubbleSort(); break;
                case 14: sortPatientsSelectionSort(); break;
                case 15:
                    running = false;
                    System.out.println("Exiting system. Goodbye!");
                    break;
                default:
                    System.out.println("Invalid Choice. Please select an option between 1 and 15.");
            }
        }
        sc.close();
    }

    /* ---------------------------------------------------------------
     *  PATIENT MANAGEMENT
     * --------------------------------------------------------------- */

    // registerPatient() - collects details, checks duplicate ID, inserts
    // into the patient linked list and pushes onto the history stack.
    void registerPatient() {
        System.out.println("\n--- Register Patient ---");
        String id = "P" + String.format("%03d", patientCounter);

        System.out.print("Enter Patient Name: ");
        String name = sc.nextLine().trim();
        if (name.isEmpty()) {
            System.out.println("Error: Patient name cannot be empty.");
            return;
        }

        int age;
        try {
            System.out.print("Enter Patient Age: ");
            age = Integer.parseInt(sc.nextLine().trim());
            if (age <= 0 || age > 120) {
                System.out.println("Error: Please enter a valid age.");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Error: Age must be a number.");
            return;
        }

        System.out.print("Enter Mobile Number: ");
        String mobile = sc.nextLine().trim();

        System.out.print("Enter Email ID: ");
        String email = sc.nextLine().trim();

        System.out.print("Enter City: ");
        String city = sc.nextLine().trim();

        System.out.print("Enter Medical History (or 'None'): ");
        String history = sc.nextLine().trim();

        // duplicate check
        if (findPatient(id) != null) {
            System.out.println("Error: Patient ID already exists.");
            return;
        }

        PatientNode newPatient = new PatientNode(id, name, age, mobile, email, city, history);

        // insert at the end of the linked list (oldest -> newest order)
        if (patientHead == null) {
            patientHead = newPatient;
        } else {
            PatientNode current = patientHead;
            while (current.next != null) {
                current = current.next;
            }
            current.next = newPatient;
        }

        // push onto the Stack ADT so newest-to-oldest retrieval is possible
        historyStack.push(newPatient);

        patientCounter++;
        System.out.println("Patient Registered Successfully! Patient ID: " + id);
    }

    // displayPatients() - traverses the patient linked list start to end
    void displayPatients() {
        System.out.println("\n--- Registered Patients ---");
        if (patientHead == null) {
            System.out.println("No patients registered yet.");
            return;
        }
        PatientNode current = patientHead;
        while (current != null) {
            System.out.println(current);
            current = current.next;
        }
    }

    // searchPatient() - uses findPatient() helper
    void searchPatient() {
        System.out.print("\nEnter Patient ID to search: ");
        String id = sc.nextLine().trim();
        PatientNode found = findPatient(id);
        if (found != null) {
            System.out.println("Patient Found:");
            System.out.println(found);
        } else {
            System.out.println("Patient not found.");
        }
    }

    // helper - linear search through the patient linked list, O(n)
    PatientNode findPatient(String id) {
        PatientNode current = patientHead;
        while (current != null) {
            if (current.patientID.equalsIgnoreCase(id)) {
                return current;
            }
            current = current.next;
        }
        return null;
    }

    /* --------------------------------------------------------------
     *  DOCTOR MANAGEMENT
     * --------------------------------------------------------------- */

    void registerDoctor() {
        System.out.println("\n--- Register Doctor ---");
        String id = "D" + String.format("%03d", doctorCounter);

        System.out.print("Enter Doctor Name: ");
        String name = sc.nextLine().trim();
        if (name.isEmpty()) {
            System.out.println("Error: Doctor name cannot be empty.");
            return;
        }

        System.out.print("Enter Specialization: ");
        String specialization = sc.nextLine().trim();

        double fee;
        try {
            System.out.print("Enter Consultation Fee: ");
            fee = Double.parseDouble(sc.nextLine().trim());
            if (fee < 0) {
                System.out.println("Error: Consultation fee cannot be negative.");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Error: Consultation fee must be a number.");
            return;
        }

        int totalSlots;
        try {
            System.out.print("Enter Total Appointment Slots: ");
            totalSlots = Integer.parseInt(sc.nextLine().trim());
            if (totalSlots <= 0) {
                System.out.println("Error: Total slots must be greater than zero.");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Error: Total slots must be a number.");
            return;
        }

        if (findDoctor(id) != null) {
            System.out.println("Error: Doctor ID already exists.");
            return;
        }

        DoctorNode newDoctor = new DoctorNode(id, name, specialization, fee, totalSlots);

        if (doctorHead == null) {
            doctorHead = newDoctor;
        } else {
            DoctorNode current = doctorHead;
            while (current.next != null) {
                current = current.next;
            }
            current.next = newDoctor;
        }

        doctorCounter++;
        System.out.println("Doctor added to the list! Doctor ID: " + id);
    }

    void displayDoctors() {
        System.out.println("\n--- Registered Doctors ---");
        if (doctorHead == null) {
            System.out.println("No doctors registered yet.");
            return;
        }
        DoctorNode current = doctorHead;
        while (current != null) {
            System.out.println(current);
            current = current.next;
        }
    }

    void searchDoctor() {
        System.out.print("\nEnter Doctor ID to search: ");
        String id = sc.nextLine().trim();
        DoctorNode found = findDoctor(id);
        if (found != null) {
            System.out.println("Doctor Found:");
            System.out.println(found);
        } else {
            System.out.println("Doctor not found.");
        }
    }

    DoctorNode findDoctor(String id) {
        DoctorNode current = doctorHead;
        while (current != null) {
            if (current.doctorID.equalsIgnoreCase(id)) {
                return current;
            }
            current = current.next;
        }
        return null;
    }

    /* ------------------------
     *  APPOINTMENT MANAGEMENT  
     * ---------------------------------------------------- */

    // bookAppointment() - validates patient & doctor, then either books a
    // slot or (if the doctor is fully booked) enqueues the patient into
    // the FIFO waiting list.
    void bookAppointment() {
        System.out.println("\n--- Book Appointment ---");

        System.out.print("Enter Patient ID: ");
        String pid = sc.nextLine().trim();
        PatientNode patient = findPatient(pid);
        if (patient == null) {
            System.out.println("Error: Patient not registered. Please register the patient first.");
            return;
        }

        System.out.print("Enter Doctor ID: ");
        String did = sc.nextLine().trim();
        DoctorNode doctor = findDoctor(did);
        if (doctor == null) {
            System.out.println("Error: Doctor not registered.");
            return;
        }

        String appointmentID = "A" + String.format("%03d", appointmentCounter++);

        if (doctor.availableSlots() > 0) {
            doctor.bookedSlots++;
            int slotNumber = doctor.bookedSlots;
            AppointmentNode newAppointment =
                    new AppointmentNode(appointmentID, pid, did, slotNumber, "Scheduled");
            enqueueAppointment(newAppointment);
            System.out.println("Appointment booked successfully!");
            System.out.println(newAppointment);
            System.out.println("Patient " + patient.name + " has been notified of the confirmed appointment.");
        } else {
            AppointmentNode waitingAppointment =
                    new AppointmentNode(appointmentID, pid, did, 0, "Waiting");
            enqueueAppointment(waitingAppointment);
            System.out.println("All slots for Dr. " + doctor.name + " are currently full.");
            System.out.println("Patient " + patient.name + " has been added to the waiting list.");
            System.out.println(waitingAppointment);
        }
    }

    
    void enqueueAppointment(AppointmentNode node) {
        if (appointmentHead == null) {
            appointmentHead = node;
        } else {
            AppointmentNode current = appointmentHead;
            while (current.next != null) {
                current = current.next;
            }
            current.next = node;
        }
    }

    // cancelAppointment() - marks an appointment as Cancelled, frees the
    // doctor's slot, then calls assignNextWaitingPatient() 
    void cancelAppointment() {
        System.out.println("\n--- Cancel Appointment ---");
        System.out.print("Enter Appointment ID: ");
        String aid = sc.nextLine().trim();

        AppointmentNode appointment = findAppointment(aid);
        if (appointment == null) {
            System.out.println("Error: Appointment not found.");
            return;
        }
        if (!appointment.status.equals("Scheduled")) {
            System.out.println("Error: Only scheduled appointments can be cancelled.");
            return;
        }

        appointment.status = "Cancelled";
        DoctorNode doctor = findDoctor(appointment.doctorID);
        if (doctor != null && doctor.bookedSlots > 0) {
            doctor.bookedSlots--;
        }
        System.out.println("Appointment " + aid + " cancelled successfully.");

        PatientNode patient = findPatient(appointment.patientID);
        if (patient != null) {
            System.out.println("Patient " + patient.name + " has been notified about the cancellation.");
        }

        // dequeue logic: give the freed slot to the next waiting patient
        assignNextWaitingPatient(appointment.doctorID);
    }

    // requestReschedule() - moves a scheduled patient back into the
    // waiting queue and frees their slot for the next waiting patient
    void requestReschedule() {
        System.out.println("\n--- Request Reschedule ---");
        System.out.print("Enter Appointment ID: ");
        String aid = sc.nextLine().trim();

        AppointmentNode appointment = findAppointment(aid);
        if (appointment == null) {
            System.out.println("Error: Appointment not found.");
            return;
        }
        if (!appointment.status.equals("Scheduled")) {
            System.out.println("Error: Only scheduled appointments can be rescheduled.");
            return;
        }

        DoctorNode doctor = findDoctor(appointment.doctorID);
        if (doctor != null && doctor.bookedSlots > 0) {
            doctor.bookedSlots--;
        }

        appointment.status = "Waiting";
        appointment.slotNumber = 0;

        System.out.println("Reschedule requested. Patient has been moved to the waiting list.");

        // assign the freed slot to the next patient already waiting
        assignNextWaitingPatient(appointment.doctorID);
    }

    // assignNextWaitingPatient() - Queue ADT dequeue behaviour: finds the
    // first appointment (in insertion order = FIFO) with status "Waiting"
    // for the given doctor and promotes it to "Scheduled".
    void assignNextWaitingPatient(String doctorID) {
        DoctorNode doctor = findDoctor(doctorID);
        if (doctor == null || doctor.availableSlots() <= 0) {
            return;
        }

        AppointmentNode current = appointmentHead;
        while (current != null) {
            if (current.doctorID.equals(doctorID) && current.status.equals("Waiting")) {
                doctor.bookedSlots++;
                current.slotNumber = doctor.bookedSlots;
                current.status = "Scheduled";

                PatientNode patient = findPatient(current.patientID);
                String patientName = (patient != null) ? patient.name : current.patientID;
                System.out.println("Next waiting patient (" + patientName +
                        ") has been assigned slot " + current.slotNumber +
                        " with Dr. " + doctor.name + " and notified.");
                return; 
            }
            current = current.next;
        }
    }

    void displayAppointments() {
        System.out.println("\n--- All Appointments ---");
        if (appointmentHead == null) {
            System.out.println("No appointments have been made yet.");
            return;
        }
        AppointmentNode current = appointmentHead;
        while (current != null) {
            System.out.println(current);
            current = current.next;
        }
    }

    // displayWaitingList() - traverses the appointment list and prints
    // only the nodes with status "Waiting", in FIFO (insertion) order
    void displayWaitingList() {
        System.out.println("\n--- Waiting List (FIFO order) ---");
        if (appointmentHead == null) {
            System.out.println("No appointments have been made yet.");
            return;
        }
        boolean found = false;
        AppointmentNode current = appointmentHead;
        while (current != null) {
            if (current.status.equals("Waiting")) {
                System.out.println(current);
                found = true;
            }
            current = current.next;
        }
        if (!found) {
            System.out.println("The waiting list is currently empty.");
        }
    }

    AppointmentNode findAppointment(String id) {
        AppointmentNode current = appointmentHead;
        while (current != null) {
            if (current.appointmentID.equalsIgnoreCase(id)) {
                return current;
            }
            current = current.next;
        }
        return null;
    }

    /* ---------------------------------------------------------------
     *  SORTING ALGORITHMS  (Bubble Sort & Selection Sort by age)
     * --------------------------------------------------------------- */

    // Converts the patient linked list into an array so it can be sorted
    // and displayed. The original linked list order is left untouched.
    private PatientNode[] patientsToArray() {
        int count = 0;
        PatientNode current = patientHead;
        while (current != null) {
            count++;
            current = current.next;
        }
        PatientNode[] array = new PatientNode[count];
        current = patientHead;
        int i = 0;
        while (current != null) {
            array[i++] = current;
            current = current.next;
        }
        return array;
    }

    // Bubble Sort - repeatedly swaps adjacent elements if they are in the
    // wrong order. Time Complexity: O(n^2) worst/average, O(n) best case.
    void sortPatientsBubbleSort() {
        PatientNode[] patients = patientsToArray();
        if (patients.length == 0) {
            System.out.println("No patients to sort.");
            return;
        }
        int n = patients.length;
        int comparisons = 0;
        for (int i = 0; i < n - 1; i++) {
            boolean swapped = false;
            for (int j = 0; j < n - 1 - i; j++) {
                comparisons++;
                if (patients[j].age > patients[j + 1].age) {
                    PatientNode temp = patients[j];
                    patients[j] = patients[j + 1];
                    patients[j + 1] = temp;
                    swapped = true;
                }
            }
            if (!swapped) break; 
        }
        System.out.println("\n--- Patients Sorted by Age (Bubble Sort) ---");
        System.out.println("Total comparisons made: " + comparisons);
        for (PatientNode p : patients) {
            System.out.println(p);
        }
    }

    // Selection Sort - repeatedly selects the minimum element from the
    // unsorted part and places it at the beginning. Time Complexity:
    // O(n^2) in all cases (best, average, worst).
    void sortPatientsSelectionSort() {
        PatientNode[] patients = patientsToArray();
        if (patients.length == 0) {
            System.out.println("No patients to sort.");
            return;
        }
        int n = patients.length;
        int comparisons = 0;
        for (int i = 0; i < n - 1; i++) {
            int minIndex = i;
            for (int j = i + 1; j < n; j++) {
                comparisons++;
                if (patients[j].age < patients[minIndex].age) {
                    minIndex = j;
                }
            }
            if (minIndex != i) {
                PatientNode temp = patients[i];
                patients[i] = patients[minIndex];
                patients[minIndex] = temp;
            }
        }
        System.out.println("\n--- Patients Sorted by Age (Selection Sort) ---");
        System.out.println("Total comparisons made: " + comparisons);
        for (PatientNode p : patients) {
            System.out.println(p);
        }
    }

    /* ---------------------------------------------------------------
     *  INPUT HELPER  (basic error handling for menu choice)
     * --------------------------------------------------------------- */
    private int readInt() {
        try {
            return Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1; // triggers "Invalid Choice" in the menu switch
        }
    }
}
