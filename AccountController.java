package com.example.Banking.CLI;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import java.util.Base64;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
public class AccountController {

    @Autowired
    private AccountRepository repository;

    @Autowired
    private TransactionRepository transactionRepo;

    @GetMapping("/")
    public String welcome() {
        return "welcome";
    }

    @GetMapping("/register")
    public String showRegisterForm() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String name,
                           @RequestParam String username,
                           @RequestParam int age,
                           @RequestParam String password,
                           @RequestParam String confirmPassword,
                           Model model) {

        if (!name.matches("^[A-Za-z ]+$")) {
            model.addAttribute("message", "Name must contain only letters and spaces.");
            return "register";
        }

        if (age < 18) {
            model.addAttribute("message", "You must be 18 or older to register.");
            return "register";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("message", "Passwords do not match.");
            return "register";
        }

        if (repository.findByUsername(username).isPresent()) {
            model.addAttribute("message", "Username already exists.");
            return "register";
        }

        String encodedPassword = Base64.getEncoder().encodeToString(password.getBytes());

        Account account = new Account();
        account.setName(name);
        account.setUsername(username);
        account.setAge(age);
        account.setPassword(encodedPassword);

        repository.save(account);

        model.addAttribute("message", "Account registered successfully! Please login.");
        return "login";
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {

        Optional<Account> optionalAccount = repository.findByUsername(username);
        if (optionalAccount.isPresent()) {
            Account account = optionalAccount.get();
            String encodedPassword = Base64.getEncoder().encodeToString(password.getBytes());
            if (account.getPassword().equals(encodedPassword)) {
                session.setAttribute("account", account);
                return "redirect:/dashboard";
            }
        }

        model.addAttribute("message", "Invalid username or password.");
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Account account = (Account) session.getAttribute("account");
        if (account == null) return "redirect:/login";

        model.addAttribute("username", account.getUsername());
        return "dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    @GetMapping("/deposit")
    public String showDepositForm() {
        return "deposit";
    }

    @PostMapping("/deposit")
    public String deposit(@RequestParam double amount, HttpSession session, Model model) {
        Account account = (Account) session.getAttribute("account");
        if (account == null) return "redirect:/login";

        if (amount <= 0) {
            model.addAttribute("message", "Deposit amount must be positive.");
            return "deposit";
        }

        account.setBalance(account.getBalance() + amount);
        repository.save(account);

        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setAmount(amount);
        transaction.setType("DEPOSIT");
        transaction.setTimestamp(LocalDateTime.now());
        transactionRepo.save(transaction);

        model.addAttribute("message", "Deposit successful!");
        return "dashboard";
    }

    @GetMapping("/withdraw")
    public String showWithdrawForm() {
        return "withdraw";
    }

    @PostMapping("/withdraw")
    public String withdraw(@RequestParam double amount, HttpSession session, Model model) {
        Account account = (Account) session.getAttribute("account");
        if (account == null) return "redirect:/login";

        if (amount <= 0 || amount > account.getBalance()) {
            model.addAttribute("message", "Invalid withdraw amount.");
            return "withdraw";
        }

        account.setBalance(account.getBalance() - amount);
        repository.save(account);

        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setAmount(amount);
        transaction.setType("WITHDRAW");
        transaction.setTimestamp(LocalDateTime.now());
        transactionRepo.save(transaction);

        model.addAttribute("message", "Withdrawal successful!");
        return "dashboard";
    }

    @GetMapping("/balance")
    public String checkBalance(HttpSession session, Model model) {
        Account account = (Account) session.getAttribute("account");
        if (account == null) return "redirect:/login";

        model.addAttribute("balance", account.getBalance());
        return "balance";
    }

    @GetMapping("/transactions")
    public String transactionHistory(@RequestParam(required = false) String type,
                                     HttpSession session, Model model) {
        Account account = (Account) session.getAttribute("account");
        if (account == null) return "redirect:/login";

        List<Transaction> transactions = (type == null || type.isEmpty()) ?
                transactionRepo.findByAccount(account) :
                transactionRepo.findByAccountAndType(account, type.toUpperCase());

        model.addAttribute("transactions", transactions);
        return "transactions";
    }
}
