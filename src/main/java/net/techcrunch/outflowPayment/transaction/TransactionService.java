package net.techcrunch.outflowPayment.transaction;

import net.techcrunch.outflowPayment.util.NotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    private final TransactionMapper transactionMapper;


    public TransactionService(final TransactionRepository transactionRepository, TransactionMapper transactionMapper) {
        this.transactionRepository = transactionRepository;
        this.transactionMapper = transactionMapper;
    }

    public List<TransactionDTO> findAll() {
        final List<Transaction> transactions = transactionRepository.findAll(Sort.by("id"));
        return transactions.stream()
                .map(transaction -> transactionMapper.toDto(transaction))
                .toList();
    }

    public TransactionDTO get(final Long id) {
        Transaction transaction = transactionRepository.findTransactionById(id);
        return transactionMapper.toDto(transaction);
//        return transactionRepository.findById(id)
//                .map(transaction -> transactionMapper.toDto(transaction))
//                .orElseThrow(NotFoundException::new);
    }

    public TransactionDTO create(final TransactionDTO transactionDTO) {
        Transaction transaction = transactionMapper.toEntity(transactionDTO);
        return transactionMapper.toDto(transactionRepository.save(transaction));
    }
    public Transaction createEntity(final TransactionDTO transactionDTO) {
        Transaction transaction = transactionMapper.toEntity(transactionDTO);
        return transactionRepository.save(transaction);
    }
    public void update(final Long id, final TransactionDTO transactionDTO) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        transaction = transactionMapper.toEntity(transactionDTO);
        transactionRepository.save(transaction);
    }

    public void delete(final Long id) {
        transactionRepository.deleteById(id);
    }

//    public TransactionDTO mapToDTO(final Transaction transaction,
//            final TransactionDTO transactionDTO) {
//        transactionDTO.setId(transaction.getId());
//        transactionDTO.setReference(transaction.getReference());
//        transactionDTO.setExternalReference(transaction.getExternalReference());
//        transactionDTO.setAmount(transaction.getAmount());
//        transactionDTO.setBeneficiaryAccount(transaction.getBeneficiaryAccount());
//        transactionDTO.setSourceAccount(transaction.getSourceAccount());
//        transactionDTO.setStatus(transaction.getStatus());
//        transactionDTO.setChannel(transaction.getChannel());
//        transactionDTO.setTransactionOwner(transaction.getTransactionOwner());
//        transactionDTO.setPaymentType(transaction.getPaymentType());
//      transactionDTO.setInvoice(transaction.getInvoice());
//        return transactionDTO;
//    }

//    public Transaction mapToEntity(final TransactionDTO transactionDTO,
//            final Transaction transaction) {
//        transaction.setReference(transactionDTO.getReference());
//        transaction.setExternalReference(transactionDTO.getExternalReference());
//        transaction.setAmount(transactionDTO.getAmount());
//        transaction.setBeneficiaryAccount(transactionDTO.getBeneficiaryAccount());
//        transaction.setSourceAccount(transactionDTO.getSourceAccount());
//        transaction.setStatus(transactionDTO.getStatus());
//        transaction.setPaymentType(transactionDTO.getPaymentType());
//        transaction.setTransactionOwner(transactionDTO.getTransactionOwner());
//        transaction.setChannel(transactionDTO.getChannel());
//        return transaction;
//    }

    public boolean referenceExists(final String reference) {
        return transactionRepository.existsByReferenceIgnoreCase(reference);
    }

    public boolean externalReferenceExists(final String externalReference) {
        return transactionRepository.existsByExternalReferenceIgnoreCase(externalReference);
    }

    public boolean sourceAccountExists(final String sourceAccount) {
        return transactionRepository.existsBySourceAccountIgnoreCase(sourceAccount);
    }

    public List<TransactionDTO> getMerchantTransactions(final String merchantId){
        List<Transaction> transactions = transactionRepository.findByTransactionOwnerIgnoreCase(merchantId);
        return transactions.stream()
                .map(transaction -> transactionMapper.toDto(transaction))
                .toList();
    }

}
