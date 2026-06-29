package fu.se.smms.service.impl;

import fu.se.smms.dto.CustomerDTO;
import fu.se.smms.dto.CustomerRequestDTO;
import fu.se.smms.entity.Customer;
import fu.se.smms.repository.CustomerRepository;
import fu.se.smms.service.CustomerService;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerServiceImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public List<CustomerDTO> getAllCustomers() {
        return customerRepository.findAll().stream()
                .filter(customer -> Boolean.TRUE.equals(customer.getStatus()))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public CustomerDTO createCustomer(CustomerRequestDTO request) {
        // Generate member card ID
        long count = customerRepository.count();
        String generateId = "CARD" + String.format("%06d", count + 1);

        int points = request.getPoints() != null ? request.getPoints() : 0;
        String determinedRank = calculateRank(points);

        Customer customer = Customer.builder()
                .memberCardId(generateId)
                .name(request.getName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .points(points)
                .rank(determinedRank)
                .status(true)
                .build();

        Customer savedCustomer = customerRepository.save(customer);
        return mapToDTO(savedCustomer);
    }

    @Override
    public CustomerDTO updateCustomer(String memberCardId, CustomerRequestDTO request) {
        Customer customer = customerRepository.findByMemberCardId(memberCardId)
                .orElseThrow(() -> new RuntimeException("Customer not found with ID: " + memberCardId));

        customer.setName(request.getName());
        customer.setPhone(request.getPhone());
        customer.setEmail(request.getEmail());
        int updatedPoints = request.getPoints() != null ? request.getPoints() : customer.getPoints();
        customer.setPoints(updatedPoints);
        customer.setRank(calculateRank(updatedPoints));

        Customer updatedCustomer = customerRepository.save(customer);
        return mapToDTO(updatedCustomer);
    }

    @Override
    public void deleteCustomer(String memberCardId) {
        Customer customer = customerRepository.findByMemberCardId(memberCardId)
                .orElseThrow(() -> new RuntimeException("Customer not found with ID: " + memberCardId));

        customer.setStatus(false); // Soft delete
        customerRepository.save(customer);
    }

    @Override
    public List<CustomerDTO> searchCustomers(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllCustomers();
        }
        return customerRepository.searchCustomers(query.trim()).stream()
                .filter(customer -> Boolean.TRUE.equals(customer.getStatus()))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private CustomerDTO mapToDTO(Customer customer) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String joinDate = customer.getCreatedAt() != null ? customer.getCreatedAt().format(formatter) : "";

        return CustomerDTO.builder()
                .id(customer.getMemberCardId())
                .customerId(customer.getCustomerId())
                .name(customer.getName())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .points(customer.getPoints())
                .rank(customer.getRank() != null ? customer.getRank() : "Bronze")
                .rank(customer.getRank() != null ? customer.getRank() : "Bronze")
                .joinDate(joinDate)
                .build();
    }

    private String calculateRank(int points) {
        if (points < 500) {
            return "Bronze";
        } else if (points < 1000) {
            return "Silver";
        } else if (points < 5000) {
            return "Gold";
        } else {
            return "Platinum";
        }
    }
}
