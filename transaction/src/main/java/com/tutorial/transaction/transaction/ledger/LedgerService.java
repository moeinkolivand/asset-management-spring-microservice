package com.tutorial.transaction.transaction.ledger;
import java.math.BigDecimal;
import com.tutorial.transaction.transaction.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LedgerService {

    private final LedgerRepository ledgerRepository;

    @Autowired
    public LedgerService(LedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    public void createDebit(Transaction transaction, Long walletId, BigDecimal amount) {
        ledgerRepository.save(new LedgerEntry(transaction, walletId, LedgerEntryDirectionEnum.CREDIT, amount));
    }

    public void createCredit(Transaction transaction, Long walletId, BigDecimal amount) {
        ledgerRepository.save(new LedgerEntry(transaction, walletId, LedgerEntryDirectionEnum.DEBIT, amount));

    }
}

