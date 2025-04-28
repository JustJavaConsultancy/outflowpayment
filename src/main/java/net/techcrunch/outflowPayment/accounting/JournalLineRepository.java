package net.techcrunch.outflowPayment.accounting;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JournalLineRepository extends JpaRepository<JournalLine, Long> {
    List<JournalLine> findByAccount_CodeAndAccount_OwnerId(String code, String ownerId);
    List<JournalLine> findByAccount_TypeAndAccount_OwnerId(String type, String ownerId);

    List<JournalLine> findByAccount_OwnerId(String ownerId);



}