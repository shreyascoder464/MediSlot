package com.medislot.app.service;

import com.medislot.app.entity.DoctorProfile;
import com.medislot.app.entity.PatientProfile;
import com.medislot.app.entity.Role;
import com.medislot.app.entity.Specialization;
import com.medislot.app.entity.User;
import com.medislot.app.repository.DoctorProfileRepository;
import com.medislot.app.repository.PatientProfileRepository;
import com.medislot.app.repository.SpecializationRepository;
import com.medislot.app.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final SpecializationRepository specializationRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            PatientProfileRepository patientProfileRepository,
            DoctorProfileRepository doctorProfileRepository,
            SpecializationRepository specializationRepository,
            BCryptPasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.specializationRepository = specializationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerPatient(String name, String email, String phone, String password, String city) {
        User user = createBaseUser(name, email, phone, password, Role.PATIENT);
        user = userRepository.save(user);

        PatientProfile profile = new PatientProfile();
        profile.setUser(user);
        profile.setCity(city);
        patientProfileRepository.save(profile);
        return user;
    }

    @Transactional
    public User registerDoctor(
            String name,
            String email,
            String phone,
            String password,
            Long specializationId,
            String qualification,
            String registrationNumber,
            Integer experienceYears,
            String clinicName,
            String clinicAddress,
            String city,
            BigDecimal consultationFee,
            String bio
    ) {
        if (doctorProfileRepository.existsByRegistrationNumberIgnoreCase(registrationNumber.trim())) {
            throw new IllegalArgumentException("This medical registration number is already submitted.");
        }

        User user = createBaseUser(name, email, phone, password, Role.DOCTOR);
        user.setEnabled(false);
        user = userRepository.save(user);

        Specialization specialization = specializationRepository.findById(specializationId)
                .orElseThrow(() -> new IllegalArgumentException("Please select a valid specialization."));

        DoctorProfile profile = new DoctorProfile();
        profile.setUser(user);
        profile.setSpecialization(specialization);
        profile.setQualification(qualification.trim());
        profile.setRegistrationNumber(registrationNumber.trim().toUpperCase());
        profile.setExperienceYears(experienceYears);
        profile.setClinicName(clinicName.trim());
        profile.setClinicAddress(clinicAddress.trim());
        profile.setCity(city.trim());
        profile.setConsultationFee(consultationFee);
        profile.setBio(bio);
        doctorProfileRepository.save(profile);
        return user;
    }

    public Optional<User> login(String email, String rawPassword) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .filter(User::isEnabled)
                .filter(user -> passwordEncoder.matches(rawPassword, user.getPassword()));
    }

    private User createBaseUser(String name, String email, String phone, String password, Role role) {
        if (userRepository.existsByEmail(email.trim().toLowerCase())) {
            throw new IllegalArgumentException("Email is already registered.");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }

        User user = new User();
        user.setName(name.trim());
        user.setEmail(email.trim().toLowerCase());
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        return user;
    }
}
