package com.acuver.teamE.customerDetails.controller;

import com.acuver.teamE.customerDetails.constants.IAppConstants;
import com.acuver.teamE.customerDetails.entity.Customer;
import com.acuver.teamE.customerDetails.entity.response.CustomerResponse;
import com.acuver.teamE.customerDetails.service.CustomerService;
import com.acuver.teamE.customerDetails.service.impl.CustomerServiceCacheAsideWithAnnotationsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/cacheAsideWithAnnotations/customers")
public class CustomerCacheAsideWithAnnotationsController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerServiceCacheAsideWithAnnotationsImpl serviceCacheAsideWithAnnotations;

    @PostMapping
    public ResponseEntity<Customer> insertCustomer(@Validated @RequestBody Customer customer) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceCacheAsideWithAnnotations.saveCustomer(customer));
    }

    @GetMapping
    public CustomerResponse getAllCustomers(
            @RequestParam(value="pageNo", defaultValue = IAppConstants.DEFAULT_PAGE_NUMBER, required = false) int pageNo,
            @RequestParam(value="pageSize", defaultValue = IAppConstants.DEFAULT_PAGE_SIZE, required = false) int pageSize,
            @RequestParam(value="sortBy", defaultValue = IAppConstants.DEFAULT_SORT_BY, required = false) String sortBy,
            @RequestParam(value="sortDir", defaultValue = IAppConstants.DEFAULT_SORT_DIRECTION, required = false) String sortDir
    ) {
        return serviceCacheAsideWithAnnotations.getAllCustomers(pageNo,pageSize,sortBy,sortDir);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customer> getCustomerById(@PathVariable(name = "id") String id) {
        return ResponseEntity.status(HttpStatus.FOUND).body(serviceCacheAsideWithAnnotations.getCustomerById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Customer> updateCustomerById(@Validated @RequestBody Customer customer, @PathVariable(name = "id") String id) {
        return ResponseEntity.ok(serviceCacheAsideWithAnnotations.updateCustomerById(customer,id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCustomerById(@PathVariable(name = "id") String id) {
        serviceCacheAsideWithAnnotations.deleteCustomerById(id);
        return ResponseEntity.ok("Customer deleted successfully!");
    }

}
