package com.mentis.hrms.config;

import com.mentis.hrms.model.Department;
import com.mentis.hrms.model.Job;
import com.mentis.hrms.repository.DepartmentRepository;
import com.mentis.hrms.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private JobRepository jobRepository;

    @Override
    public void run(String... args) throws Exception {
        // Create sample departments if they don't exist
        if (departmentRepository.count() == 0) {
            Department dept1 = new Department();
            dept1.setName("Engineering");
            dept1.setDescription("Software development and engineering department");
            departmentRepository.save(dept1);

            Department dept2 = new Department();
            dept2.setName("Marketing");
            dept2.setDescription("Marketing and sales department");
            departmentRepository.save(dept2);

            Department dept3 = new Department();
            dept3.setName("Human Resources");
            dept3.setDescription("HR and recruitment department");
            departmentRepository.save(dept3);

            System.out.println("Sample departments created");
        }

        // Create sample jobs if none exist
        if (jobRepository.count() == 0) {
            Job job1 = new Job();
            job1.setTitle("Senior Java Developer");
            job1.setDepartment("Engineering");
            job1.setJobType("Full-time");
            job1.setLocation("New York");
            job1.setExperienceLevel("Senior");
            job1.setSalaryRange("$90,000 - $120,000");
            job1.setApplicationDeadline(LocalDate.now().plusDays(30));
            job1.setDescription("We are looking for a skilled Java developer with Spring Boot experience.");
            job1.setRequirements("Java, Spring Boot, Hibernate, MySQL");
            job1.setApplicationInstructions("Send your resume to jobs@company.com");
            jobRepository.save(job1);

            Job job2 = new Job();
            job2.setTitle("Marketing Specialist");
            job2.setDepartment("Marketing");
            job2.setJobType("Full-time");
            job2.setLocation("Remote");
            job2.setExperienceLevel("Mid");
            job2.setSalaryRange("$60,000 - $80,000");
            job2.setApplicationDeadline(LocalDate.now().plusDays(45));
            job2.setDescription("Join our marketing team to help grow our brand presence.");
            job2.setRequirements("Marketing, SEO, Social Media, Analytics");
            job2.setApplicationInstructions("Apply through our website");
            jobRepository.save(job2);

            System.out.println("Sample jobs created");
        }
    }
}