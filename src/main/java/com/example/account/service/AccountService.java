package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.dto.AccountDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static com.example.account.type.AccountStatus.IN_USE;
import static com.example.account.type.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class AccountService {
    Random random = new Random();
    private final AccountRepository accountRepository;
    private final AccountUserRepository accountUserRepository;

    /**
     * // 사용자가 있는지 조회
     * // 계좌의 번호를 생성하고
     * // 계좌를 저장하고, 그 정보를 넘긴다.
     */
    @Transactional
    public AccountDto createAccount(Long userId, Long initialBalance) {
        AccountUser accountUser = getAccountUser(userId);

        validateCreateAccount(accountUser);

        //---------------------------------------------------
        //계좌번호를 설정하는 부분
        //실제로 난수를 생성하도록 setSeed를 설정
        random.setSeed(System.currentTimeMillis());
        boolean flag = true;
        StringBuilder test = new StringBuilder();
        String newAccountNumber =createRandomAccountNumber(test);

//                accountRepository.findFirstByOrderByIdDesc()
//                        .map(account -> (Integer.parseInt(account.getAccountNumber())) + 1 + "")
//                        .orElse("1000000000");
//
//        //---------------------------------------------------


        // 만약 랜럼어카운트넘버가 기존 넘버에 존재하지 않는다면


        return AccountDto.fromEntity(
                accountRepository.save(
                        Account.builder()
                                .accountUser(accountUser)
                                .accountStatus(IN_USE)
                                .accountNumber(newAccountNumber)
                                .balance(initialBalance)
                                .registeredAt(LocalDateTime.now())
                                .build()
                ));
    }
    //계좌번호를 랜덤으로설정하는 부분
    private String createRandomAccountNumber(StringBuilder test) {
        do {
            test.setLength(0);
            for (int i = 0; i < 10; i++) {
                int x = random.nextInt(9);
                test.append(x);
            }
            //만약 생성된 랜덤넘버가 없다면 랜덤넘버를 반환
        } while (accountRepository.findByAccountNumber(test.toString()).isPresent());

        return test.toString();
    }

    private AccountUser getAccountUser(Long userId) {
        AccountUser accountUser = accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(USER_NOT_FOUND));
        return accountUser;
    }

    private void validateCreateAccount(AccountUser accountUser) {
        if (accountRepository.countByAccountUser(accountUser) >= 10) {
            throw new AccountException(MAX_ACCOUNT_PER_USER_10);
        }
    }

    @Transactional
    public Account getAccount(Long id) {
        if (id < 0) {
            throw new RuntimeException("Minus");
        }
        return accountRepository.findById(id).get();
    }

    @Transactional
    public AccountDto deleteAccount(Long userId, String accountNumber) {
        AccountUser accountUser = getAccountUser(userId);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

        validateDeleteAccount(accountUser, account);

        account.setAccountStatus(AccountStatus.UNREGISTERED);
        account.setUnRegisteredAt(LocalDateTime.now());

        accountRepository.save(account);

        return AccountDto.fromEntity(account);
    }

    private void validateDeleteAccount(AccountUser accountUser, Account account) {
        if (accountUser.getId() != account.getAccountUser().getId()) {
            throw new AccountException(USER_ACCOUNT_UN_MATCH);
        }
        if (account.getAccountStatus() == AccountStatus.UNREGISTERED) {
            throw new AccountException(ACCOUNT_ALREADY_UNREGISTERED);
        }
        if (account.getBalance() > 0) {
            throw new AccountException(BALANCE_NOT_EMPTY);
        }
    }

    @Transactional
    public List<AccountDto> getAccountByUserId(Long userId) {
        AccountUser accountUser = getAccountUser(userId);

        List<Account> accounts = accountRepository.findByAccountUser(accountUser);

        return accounts.stream()
                .map(AccountDto::fromEntity)
                .collect(Collectors.toList());
    }
}
