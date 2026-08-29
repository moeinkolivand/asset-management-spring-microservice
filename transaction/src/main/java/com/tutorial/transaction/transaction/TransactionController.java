package com.tutorial.transaction.transaction;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transaction/")
public class TransactionController {
    private final TransactionService transactionService;

    @Autowired
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<String> withdraw(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody TransferDto transferDto
    ) {
        transactionService.transfer(transferDto, userId);
        return ResponseEntity.ok("");
    }

    @GetMapping
    public ResponseEntity<Page<Transaction>> getUserTransactions(
            @RequestHeader("X-User-Id") Long userId,
            @PageableDefault(
                    size = 10, sort = "createdAt", direction = Sort.Direction.DESC
            ) Pageable pageable){
        return ResponseEntity.ok(transactionService.getUserTransactions(userId, pageable));
    }

}
