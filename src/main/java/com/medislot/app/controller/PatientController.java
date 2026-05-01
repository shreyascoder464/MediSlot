package com.medislot.app.controller;

import com.medislot.app.entity.Role;
import com.medislot.app.repository.SpecializationRepository;
import com.medislot.app.service.NotificationService;
import com.medislot.app.service.PatientService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/patient")
public class PatientController {

    private final PatientService patientService;
    private final SpecializationRepository specializationRepository;
    private final NotificationService notificationService;

    public PatientController(
            PatientService patientService,
            SpecializationRepository specializationRepository,
            NotificationService notificationService
    ) {
        this.patientService = patientService;
        this.specializationRepository = specializationRepository;
        this.notificationService = notificationService;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Long patientId = requirePatient(session);
        if (patientId == null) return "redirect:/login";
        model.addAttribute("name", session.getAttribute("name"));
        model.addAttribute("appointments", patientService.appointments(patientId));
        model.addAttribute("notifications", notificationService.forUser(patientId));
        return "patient-dashboard";
    }

    @GetMapping("/doctors")
    public String doctors(
            @RequestParam(required = false) Long specializationId,
            @RequestParam(required = false) String city,
            HttpSession session,
            Model model
    ) {
        if (requirePatient(session) == null) return "redirect:/login";
        model.addAttribute("specializations", specializationRepository.findByActiveTrueOrderByNameAsc());
        model.addAttribute("doctors", patientService.searchDoctors(specializationId, city));
        model.addAttribute("selectedSpecialization", specializationId);
        model.addAttribute("city", city);
        return "patient-doctors";
    }

    @GetMapping("/doctors/{doctorId}/slots")
    public String doctorSlots(@PathVariable Long doctorId, HttpSession session, Model model) {
        if (requirePatient(session) == null) return "redirect:/login";
        model.addAttribute("slots", patientService.availableSlots(doctorId));
        model.addAttribute("doctorId", doctorId);
        return "patient-slots";
    }

    @PostMapping("/book")
    public String book(@RequestParam Long slotId, @RequestParam String reason, HttpSession session, Model model) {
        Long patientId = requirePatient(session);
        if (patientId == null) return "redirect:/login";
        try {
            patientService.book(patientId, slotId, reason);
            return "redirect:/patient/dashboard?booked";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "patient-slots";
        }
    }

    @PostMapping("/appointments/{id}/cancel")
    public String cancel(@PathVariable Long id, HttpSession session) {
        Long patientId = requirePatient(session);
        if (patientId == null) return "redirect:/login";
        patientService.cancel(id, patientId);
        return "redirect:/patient/dashboard?cancelled";
    }

    @PostMapping("/appointments/{id}/review")
    public String review(
            @PathVariable Long id,
            @RequestParam Integer rating,
            @RequestParam(required = false) String comment,
            HttpSession session
    ) {
        Long patientId = requirePatient(session);
        if (patientId == null) return "redirect:/login";
        patientService.review(id, patientId, rating, comment);
        return "redirect:/patient/dashboard?reviewed";
    }

    private Long requirePatient(HttpSession session) {
        if (session.getAttribute("role") != Role.PATIENT) {
            return null;
        }
        return (Long) session.getAttribute("userId");
    }
}
