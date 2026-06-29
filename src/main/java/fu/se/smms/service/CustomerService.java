package fu.se.smms.service;

import fu.se.smms.dto.CustomerDTO;
import fu.se.smms.dto.CustomerRequestDTO;

import java.util.List;

public interface CustomerService {
    List<CustomerDTO> getAllCustomers();

    CustomerDTO createCustomer(CustomerRequestDTO request);

    CustomerDTO updateCustomer(String memberCardId, CustomerRequestDTO request);

    void deleteCustomer(String memberCardId);

    List<CustomerDTO> searchCustomers(String query);
}
