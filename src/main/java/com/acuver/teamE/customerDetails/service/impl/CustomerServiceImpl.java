package com.acuver.teamE.customerDetails.service.impl;

import com.acuver.teamE.customerDetails.entity.Customer;
import com.acuver.teamE.customerDetails.entity.response.CustomerResponse;
import com.acuver.teamE.customerDetails.repository.CustomerRepository;
import com.acuver.teamE.customerDetails.service.CustomerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@CacheConfig(cacheNames = {"Customer"})
@Slf4j
public class CustomerServiceImpl implements CustomerService {
    private static final String WRITE_BACK_CACHE_KEY = "customer:write_back";
    private RedisTemplate<String, Customer> redisTemplate;

    public CustomerServiceImpl(RedisTemplate<String, Customer> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Autowired
    private CustomerRepository customerRepository;

    @Override
    @Transactional
    public Customer saveCustomer(Customer customer) {
        customer.setId(UUID.randomUUID().toString());
        redisTemplate.boundValueOps(customer.getId()).set(customer);
        redisTemplate.boundSetOps(WRITE_BACK_CACHE_KEY).add(customer);
        return customer;
    }

    @Override
    public CustomerResponse getAllCustomers(int pageNo, int pageSize, String sortBy, String sortDir) {
        //Sorting dynamically based on Input
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        //Creating Pageable instance
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        //Creating Page object
        Page<Customer> customers = customerRepository.findAll(pageable);

        //Getting content from Page object
        List<Customer> listOfCustomers = customers.getContent();


        CustomerResponse customerResponse = new CustomerResponse();
        customerResponse.setContent(listOfCustomers);
        customerResponse.setPageNo(customers.getNumber());
        customerResponse.setPageSize(customers.getSize());
        customerResponse.setCount(customers.getTotalElements());
        customerResponse.setTotalPages(customers.getTotalPages());
        customerResponse.setLast(customers.isLast());

        return customerResponse;
    }

    @Override
    public Customer getCustomerById(String id) {
        final var customerOnCache = redisTemplate.boundValueOps(id).get();
        if (customerOnCache != null) {
            log.info("Customer retrieved from cache (customer Id={})", id);
            return customerOnCache;
        }
        final var customerNotCached = customerRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Resource not found"));
            log.info("customer retrieved from database (customer Id={})", id);
            redisTemplate.boundValueOps(customerNotCached.getId()).set(customerNotCached);
            redisTemplate.boundSetOps(WRITE_BACK_CACHE_KEY).add(customerNotCached);
            log.info("Person cached (key={}, value={})", id, customerNotCached);
        return customerNotCached;
    }

    @Override
    public Customer updateCustomerById(Customer customer, String id) {
        Customer fetchedCustomer = customerRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Resource not found"));

        fetchedCustomer.setFirstName(customer.getFirstName() == null ? fetchedCustomer.getFirstName() : customer.getFirstName());
        fetchedCustomer.setLastName(customer.getLastName() == null ? fetchedCustomer.getLastName() : customer.getLastName());
        fetchedCustomer.setGender(customer.getGender() == null ? fetchedCustomer.getGender() : customer.getGender());
        fetchedCustomer.setAge(customer.getAge() == null ? fetchedCustomer.getAge() : customer.getAge());
        fetchedCustomer.setContactNo(customer.getContactNo() == null ? fetchedCustomer.getContactNo() : customer.getContactNo());
        fetchedCustomer.setEmailId(customer.getEmailId() == null ? fetchedCustomer.getEmailId() : customer.getEmailId());

        Customer updatedCustomer =customerRepository.save(fetchedCustomer);
        return updatedCustomer;
    }

    @Override
    public void deleteCustomerById(String id) {
        Customer fetchedCustomer = customerRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Resource not found"));
        customerRepository.delete(fetchedCustomer);
    }
    @Scheduled(fixedRateString = "${customer-service.cache.write-back-rate}")
    public void writeBack() {
        log.info("write back scheduler invoked");
        final var amountOfPeopleToPersist = redisTemplate.boundSetOps(WRITE_BACK_CACHE_KEY).size();
        if (amountOfPeopleToPersist == null || amountOfPeopleToPersist == 0) {
            log.info("None customer to write back from cache to database");
            return;
        }

        log.info("Found {} customer to write back from cache to database", amountOfPeopleToPersist);
        final var setOperations = redisTemplate.boundSetOps(WRITE_BACK_CACHE_KEY);
        final var scanOptions = ScanOptions.scanOptions().build();

        try (final var cursor = setOperations.scan(scanOptions)) {
            assert cursor != null;
            while (cursor.hasNext()) {
                final var customer = cursor.next();
                customerRepository.save(customer);
                log.info("Customer saved (customer={})", customer);

                redisTemplate.boundSetOps(WRITE_BACK_CACHE_KEY).remove(customer);
                log.info("Customer removed from {} set (customer={})", WRITE_BACK_CACHE_KEY, customer);
            }
            log.info("Persisted {} customer in the database", amountOfPeopleToPersist);
        } catch (RuntimeException exception) {
            log.error("Error reading {} set from Redis", WRITE_BACK_CACHE_KEY, exception);
        }
    }

}
