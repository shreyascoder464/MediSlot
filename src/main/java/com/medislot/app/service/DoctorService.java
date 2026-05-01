package com.medislot.app.service;

import com.medislot.app.entity.Appointment;
import com.medislot.app.entity.AppointmentStatus;
import com.medislot.app.entity.DoctorProfile;
import com.medislot.app.entity.NotificationType;
import com.medislot.app.entity.SlotStatus;
import com.medislot.app.entity.TimeSlot;
import com.medislot.app.entity.User;
import com.medislot.app.entity.VerificationStatus;
import com.medislot.app.repository.AppointmentRepository;
import com.medislot.app.repository.DoctorProfileRepository;
import com.medislot.app.repository.TimeSlotRepository;
import com.medislot.app.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class DoctorService {

    private final UserRepository userRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final AppointmentRepository appointmentRepository;
    private final NotificationService notificationService;

    public DoctorService(
            UserRepository userRepository,
            DoctorProfileRepository doctorProfileRepository,
            TimeSlotRepository timeSlotRepository,
            AppointmentRepository appointmentRepository,
            NotificationService notificationService
    ) {
        this.userRepository = userRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.appointmentRepository = appointmentRepository;
        this.notificationService = notificationService;
    }

    public DoctorProfile profile(Long doctorUserId) {
        return doctorProfileRepository.findByUserId(doctorUserId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor profile not found."));
    }

    public List<TimeSlot> slots(Long doctorUserId) {
        return timeSlotRepository.findByDoctorIdOrderBySlotDateAscStartTimeAsc(doctorUserId);
    }

    public List<Appointment> appointments(Long doctorUserId) {
        return appointmentRepository.findByDoctorIdOrderByCreatedAtDesc(doctorUserId);
    }

    @Transactional
    public void createSlots(Long doctorUserId, LocalDate date, LocalTime start, LocalTime end, int durationMinutes) {
        DoctorProfile profile = profile(doctorUserId);
        if (profile.getVerificationStatus() != VerificationStatus.APPROVED) {
            throw new IllegalArgumentException("Only approved doctors can create slots.");
        }
        if (date == null || start == null || end == null || !end.isAfter(start)) {
            throw new IllegalArgumentException("Please enter a valid date and time range.");
        }
        if (date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Slot date cannot be in the past.");
        }
        if (durationMinutes < 10 || durationMinutes > 120) {
            throw new IllegalArgumentException("Slot duration must be between 10 and 120 minutes.");
        }

        User doctor = userRepository.findById(doctorUserId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found."));
        LocalTime cursor = start;
        while (!cursor.plusMinutes(durationMinutes).isAfter(end)) {
            if (!timeSlotRepository.existsByDoctorIdAndSlotDateAndStartTime(doctorUserId, date, cursor)) {
                TimeSlot slot = new TimeSlot();
                slot.setDoctor(doctor);
                slot.setSlotDate(date);
                slot.setStartTime(cursor);
                slot.setEndTime(cursor.plusMinutes(durationMinutes));
                slot.setStatus(SlotStatus.AVAILABLE);
                timeSlotRepository.save(slot);
            }
            cursor = cursor.plusMinutes(durationMinutes);
        }
    }

    @Transactional
    public void approveAppointment(Long appointmentId, Long doctorUserId) {
        Appointment appointment = getDoctorAppointment(appointmentId, doctorUserId);
        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new IllegalArgumentException("Only pending appointments can be approved.");
        }
        appointment.setStatus(AppointmentStatus.APPROVED);
        appointment.setUpdatedAt(LocalDateTime.now());
        notificationService.create(appointment.getPatient(), "Appointment approved",
                "Dr. " + appointment.getDoctor().getName() + " approved your appointment.",
                NotificationType.APPOINTMENT);
    }

    @Transactional
    public void rejectAppointment(Long appointmentId, Long doctorUserId) {
        Appointment appointment = getDoctorAppointment(appointmentId, doctorUserId);
        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new IllegalArgumentException("Only pending appointments can be rejected.");
        }
        appointment.setStatus(AppointmentStatus.REJECTED);
        appointment.setUpdatedAt(LocalDateTime.now());
        appointment.getSlot().setStatus(SlotStatus.AVAILABLE);
        notificationService.create(appointment.getPatient(), "Appointment rejected",
                "Dr. " + appointment.getDoctor().getName() + " rejected your appointment.",
                NotificationType.APPOINTMENT);
    }

    @Transactional
    public void completeAppointment(Long appointmentId, Long doctorUserId) {
        Appointment appointment = getDoctorAppointment(appointmentId, doctorUserId);
        if (appointment.getStatus() != AppointmentStatus.APPROVED) {
            throw new IllegalArgumentException("Only approved appointments can be completed.");
        }
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setUpdatedAt(LocalDateTime.now());
    }

    private Appointment getDoctorAppointment(Long appointmentId, Long doctorUserId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found."));
        if (!appointment.getDoctor().getId().equals(doctorUserId)) {
            throw new IllegalArgumentException("This appointment does not belong to you.");
        }
        return appointment;
    }
}
