package net.techcrunch.outflowPayment.invoice;

import org.springframework.data.jpa.repository.JpaRepository;


public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    //Invoice findFirstByCusomer(Customer customer);

}
